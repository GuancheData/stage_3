package com.guanchedata.infrastructure.config;

import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import com.guanchedata.infrastructure.adapters.broker.ActiveMQMessageConsumer;
import com.guanchedata.infrastructure.ports.MessageConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBrokerConfig {
    private static final Logger log = LoggerFactory.getLogger(MessageBrokerConfig.class);

    public MessageConsumer createConsumer(String brokerUrl, IndexingService indexingService) {
        MessageConsumer messageConsumer = new ActiveMQMessageConsumer(brokerUrl, "documents.ingested");
        messageConsumer.startConsuming(documentId -> {
            log.info("Processing document from broker: {}", documentId);
            indexingService.indexDocument(Integer.parseInt(documentId));
        });
        return messageConsumer;
    }
}
