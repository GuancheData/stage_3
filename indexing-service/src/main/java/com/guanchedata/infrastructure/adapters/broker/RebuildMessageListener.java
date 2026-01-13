package com.guanchedata.infrastructure.adapters.broker;

import com.google.gson.Gson;
import com.guanchedata.model.RebuildCommand;
import com.guanchedata.infrastructure.adapters.recovery.ReindexingExecutor;
import com.hazelcast.core.HazelcastInstance;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class RebuildMessageListener {

    private static final Logger log = LoggerFactory.getLogger(RebuildMessageListener.class);
    private static final String REBUILD_TOPIC = "index.rebuild.command";

    private final HazelcastInstance hz;
    private final ReindexingExecutor reindexingExecutor;
    private final String brokerUrl;
    private final Gson gson = new Gson();

    public RebuildMessageListener(HazelcastInstance hz,
                                  ReindexingExecutor reindexingExecutor,
                                  String brokerUrl) {
        this.hz = hz;
        this.reindexingExecutor = reindexingExecutor;
        this.brokerUrl = brokerUrl;
    }

    public void startListening() {
        new Thread(() -> {
            while (true) {
                try {
                    log.info("Starting rebuild listener on topic: {}", REBUILD_TOPIC);
                    listen();
                } catch (Exception e) {
                    log.error("Rebuild listener crashed, restarting in 5s...", e);
                    sleep(5000);
                }
            }
        }, "Rebuild-Listener").start();
    }

    private void listen() throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        Connection connection = factory.createConnection();

        connection.setClientID("indexer-rebuild-" + UUID.randomUUID());
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(REBUILD_TOPIC);

        MessageConsumer consumer = session.createConsumer(topic);

        consumer.setMessageListener(message -> {
            try {
                if (message instanceof TextMessage textMessage) {
                    String json = textMessage.getText();
                    RebuildCommand command = gson.fromJson(json, RebuildCommand.class);
                    handleRebuildCommand(command);
                }
            } catch (Exception e) {
                log.error("Error handling rebuild message", e);
            }
        });

        log.info("Rebuild listener active on topic: {}", REBUILD_TOPIC);

        while (true) {
            sleep(1000);
        }
    }

    private void handleRebuildCommand(RebuildCommand command) {
        log.info("Received rebuild command. Epoch: {}", command.getEpoch());

        new Thread(() -> {
            try {
                log.info("Waiting 5 seconds for all nodes to receive command...");
                Thread.sleep(5000);

                cleanupDistributedStructures();

                Thread.sleep(2000);

                log.info("Starting reindex from disk...");
                reindexingExecutor.rebuildFromDisk();

                log.info("Rebuild completed on this node");

            } catch (Exception e) {
                log.error("Error during rebuild", e);
            }
        }, "Rebuild-Worker").start();
    }

    private void cleanupDistributedStructures() {
        try {
            log.info("Clearing distributed structures...");

            hz.getSet("indexingRegistry").clear();
            hz.getMultiMap("inverted-index").clear();
            hz.getMap("bookMetadata").clear();
            hz.getQueue("books").clear();

            log.info("Cleanup completed");

        } catch (Exception e) {
            log.warn("Error during cleanup (might be race condition): {}", e.getMessage());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
