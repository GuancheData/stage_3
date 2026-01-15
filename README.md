# Search Engine - Stage 3

[![My Skills](https://go-skill-icons.vercel.app/api/icons?i=java,maven,docker)](https://go-skill-icons.vercel.app/api/) &nbsp;![Architecture Badge](assets/badges/hazelcastv3.1.svg) &nbsp;![Architecture Badge](assets/badges/activemq.svg) &nbsp;[![My Skills](https://go-skill-icons.vercel.app/api/icons?i=nginx,github)](https://go-skill-icons.vercel.app/api/)

## Project Description

WIP

---

## Build and Run Instructions (EXAMPLE, WIP)

### Prerequisites

Install the following on your development machine:

- **Java JDK 17**

  - Verify: `java -version`

- **Maven 3.6+**
    
  - Verify: `mvn -v`
 
- **Docker Desktop**
  
- `curl` (for quick endpoint checks)

### Building and Running

In a multi-node deployment, one **central node** is responsible for starting the message broker (ActiveMQ), while the remaining nodes only run the other microservices (crawler, indexer, search, etc.).

Before starting the cluster, each node must adapt the `docker-compose.yml` to its own IP and to the IP of the central broker node.

For each service, update:

```yaml
services:
  ingestion1:
    build:
      context: ./ingestion-service
    image: ingestion-service:latest
    container_name: ingestion1
    ports:
      - "5701:5701"
    command: ['datalake', 'logs/log.log']
    environment:
      PUBLIC_IP: "" # ← host IP of this node
      HZ_PORT: "5701"
      REPLICATION_FACTOR: 3
      INDEXING_BUFFER_FACTOR: 10
      BROKER_URL: tcp://XXX:61616  # ← IP of the central broker node
      HAZELCAST_CLUSTER_NAME: SearchEngine
    volumes:
      - ./mnt/datalake:/app/datalake
      - ./mnt/logs:/app/logs
    networks:
      - search_net
```

- `PUBLIC_IP`: must be set to the host IP address of **the machine where this compose file is running**, so that other nodes and Hazelcast members can reach it.
- `BROKER_URL`: must point to the **central broker node** that runs ActiveMQ, replacing `XXX` with the IP of that central node, e.g. `tcp://192.168.1.10:61616`.

Once the IPs are correctly configured on each node, you can start:

- On the central node (broker + services):

```bash
docker compose --profile broker up -d
```

- On the other nodes (services only):

```bash
docker compose up -d
```

> Note: In this project it is not necessary to run a separate manual build step, because the Compose configuration is prepared to compile the application JARs and build the Docker images automatically as part of the service startup process. This means `docker compose up` (with or without the `--profile broker` flag) is enough to trigger the build and run pipeline in each node.

---

## Benchmarking (EXAMPLE, WIP)

### Benchmark Summary

Benchmarks were executed to evaluate the performance, scalability, and resilience of the system
under different workloads. The experiments focus on:

- Ingestion and indexing throughput
- Search query latency under concurrent load
- Horizontal scalability when adding service replicas
- Fault tolerance and recovery after simulated node failures

### Reproducing the Benchmarks

To reproduce the benchmarks:

1. Deploy the system using Docker Compose.

2. Execute the ingestion process to populate the datalake.

3. Run the provided benchmark or load-testing scripts to generate concurrent search requests.

4. Increase the number of crawler, indexer, or search service replicas to observe scalability.

5. Simulate node failures by stopping one or more containers during execution.

6. Collect throughput, latency, and resource utilization metrics from logs and monitoring outputs.

Benchmark datasets, logs, and performance results are available in the `benchmarks/` directory.

---

## Demonstration Video (EXAMPLE, WIP)

**🎥 YouTube (Unlisted):**
👉 <PASTE_YOUTUBE_LINK_HERE>

**Video title:**
[Stage 3] Search Engine Project - <GuancheData> (ULPGC)

The video demonstrates system deployment, real-time ingestion and search operations, horizontal
scaling under load, and automatic recovery after simulated failures.













