package com.guanchedata.infrastructure.adapters.activemq;

import com.google.gson.Gson;
import com.guanchedata.model.RebuildCommand;
import com.guanchedata.infrastructure.adapters.recovery.ReindexingExecutor;
import com.hazelcast.core.HazelcastInstance;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class RebuildMessageListener {
    private static final Logger log = LoggerFactory.getLogger(RebuildMessageListener.class);
    private static final String REBUILD_TOPIC = "index.rebuild.command";
    private static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final HazelcastInstance hz;
    private final ReindexingExecutor reindexingExecutor;
    private final ConnectionFactory factory;
    private final Gson gson = new Gson();
    private final AtomicBoolean rebuildInProgress = new AtomicBoolean(false);

    public RebuildMessageListener(HazelcastInstance hz, ReindexingExecutor reindexingExecutor, ConnectionFactory factory) {
        this.hz = hz;
        this.reindexingExecutor = reindexingExecutor;
        this.factory = factory;
    }

    public boolean isRebuildInProgress() { return rebuildInProgress.get(); }

    public void startListening() {
        new Thread(this::runListener, "Rebuild-Listener-Thread").start();
    }

    private void runListener() {
        while (!Thread.currentThread().isInterrupted()) {
            try (Connection connection = factory.createConnection()) {
                connection.setClientID("indexer-rebuild-" + UUID.randomUUID());
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Topic topic = session.createTopic(REBUILD_TOPIC);

                jakarta.jms.MessageConsumer consumer = session.createConsumer(topic);
                consumer.setMessageListener(this::onMessage);

                log.info("Rebuild listener active on topic: {}", REBUILD_TOPIC);
                Thread.currentThread().join();
            } catch (Exception e) {
                log.error("Rebuild listener error, retrying in 5s...", e);
                sleep(5000);
            }
        }
    }

    private void onMessage(Message message) {
        try {
            if (message instanceof TextMessage text) {
                RebuildCommand command = gson.fromJson(text.getText(), RebuildCommand.class);
                String dateUtc = ISO_UTC.format(Instant.ofEpochMilli(command.getEpoch()));
                log.info("Received rebuild command issued at: {} UTC", dateUtc);
                handleExecution();
            }
        } catch (Exception e) { log.error("Error processing rebuild message", e); }
    }

    private void handleExecution() {
        rebuildInProgress.set(true);
        new Thread(() -> {
            try {
                log.info("Waiting 10s for cluster sync...");
                Thread.sleep(10000);
                reindexingExecutor.rebuildIndex();
                hz.getCPSubsystem().getCountDownLatch("rebuild-latch").countDown();
            } catch (Exception e) {
                log.error("Rebuild failed", e);
            } finally {
                rebuildInProgress.set(false);
                log.info("Node ready for messages again.");
            }
        }, "Rebuild-Worker").start();
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException ignored) {} }
}