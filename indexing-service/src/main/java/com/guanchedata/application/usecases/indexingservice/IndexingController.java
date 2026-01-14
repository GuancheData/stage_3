package com.guanchedata.application.usecases.indexingservice;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import com.guanchedata.infrastructure.adapters.recovery.ReindexingExecutor;
import com.guanchedata.model.RebuildCommand;
import io.javalin.http.Context;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);

    private final IndexingService indexingService;
    private final ReindexingExecutor reindexingExecutor;
    private final String brokerUrl;

    public IndexingController(IndexingService indexingService, ReindexingExecutor reindexingExecutor, String brokerUrl) {
        this.indexingService = indexingService;
        this.reindexingExecutor = reindexingExecutor;
        this.brokerUrl = brokerUrl;
    }

    public void indexDocument(Context ctx) {
        int documentId = Integer.parseInt(ctx.pathParam("documentId"));
        log.info("Received index request for document: {}", documentId);

        try {
            indexingService.indexDocument(documentId);
            ctx.status(200).json(Map.of(
                    "status", "success",
                    "message", "Document indexed successfully",
                    "documentId", documentId
            ));
        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage());
            ctx.status(500).json(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    public void rebuild(Context ctx) {
        log.info("Broadcasting rebuild command to all nodes");

        try {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("index.rebuild.command");
            MessageProducer producer = session.createProducer(topic);

            RebuildCommand command = new RebuildCommand(System.currentTimeMillis());
            String json = new Gson().toJson(command);

            TextMessage message = session.createTextMessage(json);
            producer.send(message);

            connection.close();

            log.info("Rebuild command broadcast to all nodes");

            ctx.status(200).json(Map.of(
                    "status", "success",
                    "message", "Rebuild broadcast to all indexer nodes"
            ));
        } catch (Exception e) {
            log.error("Error broadcasting rebuild: {}", e.getMessage());
            ctx.status(500).json(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    public void health(Context ctx) {
        ctx.json(Map.of(
                "status", "healthy",
                "service", "indexing"
        ));
    }
}
