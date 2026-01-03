package com.guanchedata.infrastructure.config;

import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import com.guanchedata.infrastructure.adapters.broker.ActiveMQMessageConsumer;
import com.guanchedata.infrastructure.ports.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageBrokerConfig {
    private static final Logger log = LoggerFactory.getLogger(MessageBrokerConfig.class);

    private final ExecutorService indexingPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );

    public MessageConsumer createConsumer(String brokerUrl, IndexingService indexingService) {
        MessageConsumer messageConsumer = new ActiveMQMessageConsumer(brokerUrl, "documents.ingested");

        messageConsumer.startConsuming(documentId -> {
            indexingPool.submit(() -> {
                try {
                    log.info("Processing document from broker: {}", documentId);
                    indexingService.indexDocument(Integer.parseInt(documentId));
                    log.info("Finished processing document: {}", documentId);
                } catch (Exception e) {
                    log.error("Error processing document {}: {}", documentId, e.getMessage(), e);
                }
            });
        });

        return messageConsumer;
    }

    public void shutdown() {
        indexingPool.shutdown();
    }
}
