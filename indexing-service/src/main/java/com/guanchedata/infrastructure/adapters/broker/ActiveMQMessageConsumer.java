package com.guanchedata.infrastructure.adapters.broker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.guanchedata.infrastructure.ports.MessageConsumer;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ActiveMQMessageConsumer implements MessageConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(ActiveMQMessageConsumer.class);

    private final String brokerUrl;
    private final String queueName;
    private final Gson gson = new Gson();

    private Connection connection;
    private Session session;
    private jakarta.jms.MessageConsumer consumer;

    public ActiveMQMessageConsumer(String brokerUrl, String queueName) {
        this.brokerUrl = brokerUrl;
        this.queueName = queueName;
    }

    @Override
    public void startConsuming(Consumer<String> messageHandler) {
        try {
            ConnectionFactory factory =
                    new ActiveMQConnectionFactory(brokerUrl);

            connection = factory.createConnection();
            connection.start();

            session = connection.createSession(
                    false,
                    Session.AUTO_ACKNOWLEDGE
            );

            Destination queue = session.createQueue(queueName);
            consumer = session.createConsumer(queue);

            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof TextMessage textMessage) {
                        String text = textMessage.getText();
                        String bookId = extractBookId(text);
                        messageHandler.accept(bookId);
                    }
                } catch (JMSException e) {
                    log.error("Error processing message", e);
                }
            });

            log.info(
                    "ActiveMQ consumer started on queue: {}",
                    queueName
            );

        } catch (JMSException e) {
            log.error("Error starting ActiveMQ consumer", e);
            throw new RuntimeException(
                    "Failed to start message consumer",
                    e
            );
        }
    }

    @Override
    public void stopConsuming() {}

    private String extractBookId(String jsonMessage) {
        try {
            JsonObject json =
                    gson.fromJson(jsonMessage, JsonObject.class);
            return json.get("bookId").getAsString();
        } catch (Exception e) {
            log.warn(
                    "Failed to parse JSON, using raw message: {}",
                    jsonMessage
            );
            return jsonMessage;
        }
    }
}
