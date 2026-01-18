package com.guanchedata.infrastructure.adapters.recovery;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.adapters.broker.ActiveMQIngestionControlPublisher;
import com.guanchedata.model.RebuildCommand;
import com.guanchedata.model.RebuildResult;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.ICountDownLatch;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CoordinateRebuild {
    private static final Logger log = LoggerFactory.getLogger(CoordinateRebuild.class);
    private final HazelcastInstance hz;
    private final String brokerUrl;
    private final Gson gson = new Gson();

    public CoordinateRebuild(HazelcastInstance hz, String brokerUrl) {
        this.hz = hz;
        this.brokerUrl = brokerUrl;
    }

    public RebuildResult execute() {
        try {
            int indexerCount = (int) hz.getCluster().getMembers().stream()
                    .filter(m -> "indexer".equals(m.getAttribute("role")))
                    .count();

            log.info("Starting rebuild coordination for {} nodes.", indexerCount);

            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            ActiveMQIngestionControlPublisher controlPublisher = new ActiveMQIngestionControlPublisher(factory);
            controlPublisher.publishPause();

            ICountDownLatch latch = hz.getCPSubsystem().getCountDownLatch("rebuild-latch");
            latch.trySetCount(indexerCount);

            broadcastRebuildCommand();
            new Thread(() -> waitForCompletion(latch, controlPublisher, indexerCount)).start();

            return new RebuildResult(true, "Rebuild triggered on " + indexerCount + " nodes.");

        } catch (Exception e) {
            log.error("Rebuild coordination failed", e);
            throw new RuntimeException("Failed to coordinate rebuild", e);
        }
    }

    private void broadcastRebuildCommand() throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        ActiveMQIngestionControlPublisher controlPublisher = new ActiveMQIngestionControlPublisher(factory);
        try (Connection connection = factory.createConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("index.rebuild.command");
            MessageProducer producer = session.createProducer(topic);

            RebuildCommand command = new RebuildCommand(System.currentTimeMillis());
            TextMessage message = session.createTextMessage(gson.toJson(command));
            producer.send(message);
        }
    }

    private void waitForCompletion(ICountDownLatch latch, ActiveMQIngestionControlPublisher publisher, int count) {
        try {
            if (latch.await(1, TimeUnit.HOURS)) {
                log.info("REBUILD COMPLETE. Resuming ingestion.");
                publisher.publishResume();
            } else {
                log.error("REBUILD TIMEOUT. Ingestion remains paused.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}