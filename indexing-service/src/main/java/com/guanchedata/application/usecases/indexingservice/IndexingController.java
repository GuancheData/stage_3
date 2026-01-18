package com.guanchedata.application.usecases.indexingservice;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.adapters.web.IndexingService;
import com.guanchedata.infrastructure.adapters.broker.ActiveMQIngestionControlPublisher;
import com.guanchedata.infrastructure.adapters.recovery.ReindexingExecutor;
import com.guanchedata.model.RebuildCommand;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.ICountDownLatch;
import io.javalin.http.Context;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);
    private static final Gson gson = new Gson();

    private final IndexingService indexingService;
    private final ReindexingExecutor reindexingExecutor;
    private final String brokerUrl;
    private final HazelcastInstance hz; // Añadido para gestionar el Latch distribuido

    public IndexingController(IndexingService indexingService,
                              ReindexingExecutor reindexingExecutor,
                              String brokerUrl,
                              HazelcastInstance hz) {
        this.indexingService = indexingService;
        this.reindexingExecutor = reindexingExecutor;
        this.brokerUrl = brokerUrl;
        this.hz = hz;
    }

    public void indexDocument(Context ctx) {
        int documentId = Integer.parseInt(ctx.pathParam("documentId"));
        log.info("Received index request for document: {}", documentId);

        try {
            indexingService.indexDocument(documentId);
            ctx.status(200).result(gson.toJson(Map.of(
                    "status", "success",
                    "message", "Document indexed successfully",
                    "documentId", documentId
            )));
        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage());
            ctx.status(500).result(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }

    public void rebuild(Context ctx) {
        log.info("Initiating coordinated rebuild process");

        try {
            ActiveMQIngestionControlPublisher controlPublisher = new ActiveMQIngestionControlPublisher(brokerUrl);

            int indexerCount = (int) hz.getCluster().getMembers().stream()
                    .filter(m -> "indexer".equals(m.getAttribute("role")))
                    .count();

            log.info("Detected {} indexer nodes. Setting up synchronization latch.", indexerCount);

            ICountDownLatch latch = hz.getCPSubsystem().getCountDownLatch("rebuild-latch");
            latch.trySetCount(indexerCount);

            controlPublisher.publishPause();
            log.info("Ingestion Paused cluster-wide");

            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            try (Connection connection = factory.createConnection()) {
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic topic = session.createTopic("index.rebuild.command");
                MessageProducer producer = session.createProducer(topic);

                RebuildCommand command = new RebuildCommand(System.currentTimeMillis());
                String json = gson.toJson(command);

                TextMessage message = session.createTextMessage(json);
                producer.send(message);
            }
            log.info("Rebuild command broadcast sent successfully");

            new Thread(() -> {
                try {
                    log.info("Worker thread waiting for all {} nodes to complete reindexing...", indexerCount);

                    boolean success = latch.await(1, TimeUnit.HOURS);

                    if (success) {
                        log.info("REBUILD SUCCESS: All nodes finished. Resuming ingestion.");
                        controlPublisher.publishResume();
                    } else {
                        log.error("REBUILD TIMEOUT: Some nodes did not finish in time. Ingestion remains paused for safety.");
                    }
                } catch (InterruptedException e) {
                    log.error("Rebuild coordination thread interrupted");
                    Thread.currentThread().interrupt();
                }
            }, "Rebuild-Coordinator").start();

            ctx.status(200).result(gson.toJson(Map.of(
                    "status", "success",
                    "message", "Rebuild triggered on " + indexerCount + " nodes. Ingestion will resume when all nodes finish."
            )));

        } catch (Exception e) {
            log.error("Error starting rebuild: {}", e.getMessage());
            ctx.status(500).result(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            )));
        }
    }

    public void health(Context ctx) {
        ctx.result(gson.toJson(Map.of(
                "status", "healthy",
                "service", "indexing"
        )));
    }
}