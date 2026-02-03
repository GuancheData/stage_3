# Search Engine - Stage 3

[![My Skills](https://go-skill-icons.vercel.app/api/icons?i=java,maven,docker)](https://go-skill-icons.vercel.app/api/) &nbsp;![Architecture Badge](assets/badges/hazelcastv3.1.svg) &nbsp;![Architecture Badge](assets/badges/activemq.svg) &nbsp;[![My Skills](https://go-skill-icons.vercel.app/api/icons?i=nginx,github)](https://go-skill-icons.vercel.app/api/)

## Project Description

This project implements a **distributed, fault-tolerant, and horizontally scalable search engine architecture**. The objective is to provide a complete search platform capable of handling ingestion, indexing, and querying workloads across multiple cooperating nodes.

The system is designed as a set of cooperating services deployed across multiple nodes and containers. It supports **parallel document ingestion, distributed indexing, and low-latency search** under increasing load, while remaining operational in the presence of partial failures. Scalability and resilience are achieved through replication, asynchronous communication, and in-memory data sharing.

Key architectural features include:

- **Distributed ingestion (crawling)**, where multiple crawler instances retrieve documents in parallel and store them in a replicated datalake.
- **Asynchronous indexing**, coordinated through a message broker, allowing indexers to process documents independently and reliably.
- **A distributed in-memory inverted index**, implemented with Hazelcast, which is sharded and replicated across the cluster to ensure fast queries and fault tolerance.
- **A load-balanced search layer**, using Nginx, that distributes incoming search requests across multiple search service instances and transparently handles node failures.

The entire system is containerized and deployed using **Docker Compose**, enabling reproducible execution in a laboratory environment. Benchmarking and fault-injection experiments are included to evaluate scalability, performance, and recovery behavior, demonstrating the core properties expected from a modern distributed search platform.

## Build and Run Instructions

### Prerequisites

Make sure the following tools are installed on all nodes that will participate in the cluster:

- **Java JDK 17**  
  Verify with:
  ```bash
  java -version
  ```

- **Apache Maven 3.6+**  
  Verify with:
  ```bash
  mvn -v
  ```

- **Docker Desktop**

- **curl** (optional, for quick endpoint and health checks)

---

### Build

All services are built from a single multi-module Maven project. Before running the cluster for the first time, compile and package all services by executing the following command **from the root directory of the repository**:

```bash
mvn clean package
```

This step generates the executable JAR files required by each microservice. Docker images will later reuse these artifacts during container startup.

---

### Service Configuration (Docker Compose)

The system is deployed using **Docker Compose** and configured through environment variables defined in the `docker-compose.yml` file. Each service must be correctly parameterized so it can join the Hazelcast in-memory cluster, discover other cluster members, connect to the central ActiveMQ broker if needed, etc.

All placeholders marked as `xxx` must be replaced with the **host IP address of the machine where the service is running** or, when specified, the IP of the broker node.

```yaml
ingestion-service:
  build:
    context: ./ingestion-service
  image: ingestion-service:latest
  container_name: ingestion-service
  ports:
    - "5701:5701"
  command: ['datalake']
  environment:
    HZ_PORT: "5701"
    HZ_PUBLIC_ADDRESS: xxx:5701
    HZ_MEMBERS: xxx:5701
    HAZELCAST_CLUSTER_NAME: SearchEngine
    BROKER_URL: tcp://xxx:61616
    REPLICATION_FACTOR: 2
    INDEXING_BUFFER_FACTOR: 2
  volumes:
    - ./mnt/datalake:/app/datalake
  networks:
    - search_net
  profiles:
    - backend

indexing-service:
  build:
    context: ./indexing-service
  image: indexing-service:latest
  container_name: indexing-service
  ports:
    - "5702:5702"
  command: ['datalake']
  environment:
    HZ_PORT: "5702"
    HZ_PUBLIC_ADDRESS: xxx:5702
    HZ_MEMBERS: xxx:5701
    HAZELCAST_CLUSTER_NAME: SearchEngine
    BROKER_URL: tcp://xxx:61616
  volumes:
    - ./mnt/datalake:/app/datalake
  networks:
    - search_net
  profiles:
    - backend

search-service:
  build:
    context: ./search-service
  image: search-service:latest
  container_name: search-service
  ports:
    - "5703:5703"
    - "7003:7003"
  environment:
    HZ_PORT: "5703"
    SERVICE_PORT: "7003"
    HZ_PUBLIC_ADDRESS: xxx:5703
    HZ_MEMBERS: xxx:5701
    HAZELCAST_CLUSTER_NAME: SearchEngine
    SORTING_CRITERIA: "frequency"
  networks:
    - search_net
  profiles:
    - backend
```
**Relevant parameters:**

- `HZ_PUBLIC_ADDRESS`: Public address of this service, reachable by other Hazelcast members.
- `HZ_MEMBERS`: Seed node used for Hazelcast cluster formation.
- `BROKER_URL`: Address of the ActiveMQ broker.
- `REPLICATION_FACTOR`: Number of datalake replicas per document.
- `INDEXING_BUFFER_FACTOR`: Controls batching before publishing indexing events.
- `SERVICE_PORT`: HTTP port exposed by the search API.
- `SORTING_CRITERIA`: Ranking strategy used to order search results (`frequency` | `id`)

---

### Nginx Load Balancer Configuration

Before starting the load balancer, the `nginx.conf` file must be updated to include the IP addresses of all nodes running a search service. Each backend entry must point to a reachable `<NODE_IP>:7003` endpoint.

```nginx
upstream search_backend {
    least_conn;

    server <NODE_IP>:7003 max_fails=10 fail_timeout=30s;
    # server <NODE_IP>:7003 max_fails=10 fail_timeout=30s;

    keepalive 64;
}
```

Add or remove `server` lines as search service instances are added or removed. Nginx will automatically distribute traffic and bypass failed nodes.

---

### Docker Compose Profiles and Startup

Service execution is controlled using **Docker Compose profiles**, allowing different roles to be assigned to different nodes:

- `backend`: ingestion, indexing, and search services
- `broker`: ActiveMQ message broker
- `loadbalancer`: Nginx reverse proxy

Once all configuration values are correctly set, the cluster can be started.

#### Main Node (Broker + Backend + Load Balancer)

```bash
docker compose --profile backend --profile broker --profile loadbalancer up -d
```

#### Additional Nodes (Backend Services Only)

```bash
docker compose --profile backend up -d
```

Each node will automatically join the Hazelcast cluster and connect to the broker using the configured parameters. Scaling is achieved by launching additional backend nodes with adjusted IP addresses.

---

### Notes

- Docker Compose handles both image building and container execution; no separate `docker build` step is required.
- Services can be restarted independently without data loss thanks to Hazelcast replication and broker-based coordination.

## Benchmarking

### Benchmark Summary

A set of controlled benchmarks was executed to evaluate the **performance, scalability, and fault tolerance** of the distributed search engine under different workloads. The experiments focus on:

- Ingestion rate and indexing throughput
- Search query latency under concurrent load
- Horizontal scalability when adding service replicas
- Fault tolerance and recovery time after simulated node failures

---

### Benchmark Service Configuration

```yaml
benchmark:
  environment:
    BENCHMARK_MODE: recoverytime
```

Supported benchmark modes:

- `ingestionrate`: documents per second (docs/s)
- `indexingthroughput`: tokens per second (tokens/s)
- `recoverytime`: cluster recovery time after failures

---

### Reproducing the Benchmarks

1. Deploy the system using Docker Compose.
2. Populate the datalake using the ingestion service.
3. Set `BENCHMARK_MODE` to the desired experiment.
4. Start the benchmark service:

```bash
docker compose --profile benchmark up -d
```

5. Scale backend services and repeat tests as needed.
6. Simulate failures by stopping containers during execution.

---

### Query Latency Benchmark (Apache JMeter)

Query latency was measured using **Apache JMeter**. With the system running, execute:

- `load-test.jmx` (located in `/benchmarks`)

The `/benchmarks` directory also contains datasets, logs, and previous benchmark results.

## Demonstration Video

👉 [[Stage 3] Search Engine Project - GuancheData (ULPGC)](https://www.youtube.com/watch?v=RHMDEk85xtI)

The video demonstrates system deployment, real-time ingestion and search operations, horizontal
scaling under load, and automatic recovery after simulated failures.













