package com.guanchedata.infrastructure.config;

import com.guanchedata.infrastructure.adapters.broker.ActiveMQMessageConsumer;
import com.guanchedata.infrastructure.adapters.broker.RebuildMessageListener;
import com.guanchedata.infrastructure.adapters.web.IndexBook;
import com.guanchedata.infrastructure.ports.MessageConsumer;
import org.apache.activemq.ActiveMQConnectionFactory;
import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageBrokerConfig {
    private static final Logger log = LoggerFactory.getLogger(MessageBrokerConfig.class);

    public MessageConsumer createConsumer(String brokerUrl, IndexBook indexBook, RebuildMessageListener rebuildListener) {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        MessageConsumer messageConsumer = new ActiveMQMessageConsumer(factory,"documents.ingested", rebuildListener);

        messageConsumer.startConsuming(documentId -> {
            log.info("Processing document from broker: {}", documentId);
            indexBook.execute(Integer.parseInt(documentId));
        });

        return messageConsumer;
    }
}