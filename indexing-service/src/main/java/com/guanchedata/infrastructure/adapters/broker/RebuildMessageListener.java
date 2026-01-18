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
import java.util.concurrent.atomic.AtomicBoolean;

public class RebuildMessageListener {

    private static final Logger log = LoggerFactory.getLogger(RebuildMessageListener.class);
    private static final String REBUILD_TOPIC = "index.rebuild.command";

    private final HazelcastInstance hz;
    private final ReindexingExecutor reindexingExecutor;
    private final String brokerUrl;
    private final Gson gson = new Gson();

    private final AtomicBoolean rebuildInProgress = new AtomicBoolean(false);

    public RebuildMessageListener(HazelcastInstance hz,
                                  ReindexingExecutor reindexingExecutor,
                                  String brokerUrl) {
        this.hz = hz;
        this.reindexingExecutor = reindexingExecutor;
        this.brokerUrl = brokerUrl;
    }

    public boolean isRebuildInProgress() {
        return rebuildInProgress.get();
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

        jakarta.jms.MessageConsumer consumer = session.createConsumer(topic);

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
        rebuildInProgress.set(true);

        new Thread(() -> {
            try {
                log.info("Waiting 10 seconds for all nodes to receive command...");
                Thread.sleep(10000);

                log.info("Starting rebuild index...");

                reindexingExecutor.rebuildIndex();

                log.info("Rebuild completed on this node");

                hz.getCPSubsystem().getCountDownLatch("rebuild-latch").countDown();

            } catch (Exception e) {
                log.error("Error during rebuild", e);
            } finally {
                rebuildInProgress.set(false);
                log.info("Rebuild cleared. Ready to consume messages again.");

                sleep(2000);
                log.info("Consumer ready to process pending messages");
            }
        }, "Rebuild-Worker").start();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}