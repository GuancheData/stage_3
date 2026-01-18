package com.guanchedata.infrastructure.adapters.broker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.guanchedata.infrastructure.ports.MessageConsumer;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

public class ActiveMQMessageConsumer implements MessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(ActiveMQMessageConsumer.class);
    private final ConnectionFactory factory;
    private final String queueName;
    private final RebuildMessageListener rebuildListener;
    private final Gson gson = new Gson();

    public ActiveMQMessageConsumer(ConnectionFactory factory, String queueName, RebuildMessageListener rebuildListener) {
        this.factory = factory;
        this.queueName = queueName;
        this.rebuildListener = rebuildListener;
    }

    @Override
    public void startConsuming(Consumer<String> messageHandler) {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Connection connection = factory.createConnection()) {
                    connection.start();
                    processSession(connection, messageHandler);
                } catch (Exception e) {
                    log.warn("ActiveMQ connection lost or not ready, retrying in 5s...", e);
                    sleep(5000);
                }
            }
        }, "JMS-Consumer-Thread").start();
    }

    private void processSession(Connection connection, Consumer<String> messageHandler) throws JMSException {
        try (Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE)) {
            Destination queue = session.createQueue(queueName);
            try (jakarta.jms.MessageConsumer consumer = session.createConsumer(queue)) {
                log.info("Consumer active on queue: {}", queueName);

                while (true) {
                    Message message = consumer.receive();
                    handleMessage(message, session, messageHandler);
                }
            }
        }
    }

    private void handleMessage(Message message, Session session, Consumer<String> handler) {
        try {
            if (message instanceof TextMessage text) {
                String bookId = extractBookId(text.getText());

                if (rebuildListener != null && rebuildListener.isRebuildInProgress()) {
                    session.recover();
                    return;
                }

                handler.accept(bookId);
                message.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process message, redelivering...", e);
            try { session.recover(); } catch (JMSException ignored) {}
        }
    }

    private String extractBookId(String json) {
        try {
            return gson.fromJson(json, JsonObject.class).get("bookId").getAsString();
        } catch (Exception e) {
            return json;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}