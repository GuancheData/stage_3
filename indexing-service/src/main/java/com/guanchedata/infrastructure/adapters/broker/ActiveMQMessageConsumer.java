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

    private static final Logger log = LoggerFactory.getLogger(ActiveMQMessageConsumer.class);

    private final String brokerUrl;
    private final String queueName;
    private final RebuildMessageListener rebuildListener;
    private final Gson gson = new Gson();

    private Connection connection;
    private Session session;
    private jakarta.jms.MessageConsumer consumer;

    public ActiveMQMessageConsumer(String brokerUrl, String queueName, RebuildMessageListener rebuildListener) {
        this.brokerUrl = brokerUrl;
        this.queueName = queueName;
        this.rebuildListener = rebuildListener;
    }

    @Override
    public void startConsuming(Consumer<String> messageHandler) {
        int attempt = 0;

        while (true) {
            try {
                attempt++;
                log.info("Starting ActiveMQ consumer (attempt {})...", attempt);

                ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
                connection = factory.createConnection();
                connection.start();

                session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
                Destination queue = session.createQueue(queueName);
                consumer = session.createConsumer(queue);

                consumer.setMessageListener(message -> {
                    String bookId = null;
                    try {
                        if (message instanceof TextMessage textMessage) {
                            String text = textMessage.getText();
                            bookId = extractBookId(text);

                            if (rebuildListener != null && rebuildListener.isRebuildInProgress()) {
                                log.warn("Rebuild in progress. Message will be redelivered later");
                                session.recover();
                                return;
                            }

                            int attempts = 0;
                            int maxAttempts = 3;
                            Exception lastException = null;

                            while (attempts < maxAttempts) {
                                try {
                                    messageHandler.accept(bookId);
                                    message.acknowledge();
                                    return;

                                } catch (Exception e) {
                                    attempts++;
                                    lastException = e;

                                    if (isBookNotFoundException(e)) {
                                        if (attempts < maxAttempts) {
                                            log.warn("Book {} not found (attempt {}/{}), retrying in 2s...",
                                                    bookId, attempts, maxAttempts);
                                            sleep(2000);
                                        } else {
                                            log.error("Book {} not found after {} attempts, skipping", bookId, maxAttempts);
                                            message.acknowledge();
                                            return;
                                        }
                                    } else {
                                        throw e;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error processing message for book {}", bookId, e);
                        try {
                            session.recover();
                        } catch (JMSException ex) {
                            log.error("Failed to recover session", ex);
                        }
                    }
                });

                log.info("ActiveMQ consumer started on queue: {}", queueName);
                return;

            } catch (JMSException e) {
                log.warn("ActiveMQ not ready yet, retrying in 3s...", e);
                cleanup();
                sleep(3000);
            }
        }
    }

    private boolean isBookNotFoundException(Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("Book not found")) {
            return true;
        }

        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("Book not found")) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

    private void cleanup() {
        try { if (consumer != null) consumer.close(); } catch (Exception ignored) {}
        try { if (session != null) session.close(); } catch (Exception ignored) {}
        try { if (connection != null) connection.close(); } catch (Exception ignored) {}
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private String extractBookId(String jsonMessage) {
        try {
            JsonObject json = gson.fromJson(jsonMessage, JsonObject.class);
            return json.get("bookId").getAsString();
        } catch (Exception e) {
            log.warn("Failed to parse JSON, using raw message: {}", jsonMessage);
            return jsonMessage;
        }
    }
}
