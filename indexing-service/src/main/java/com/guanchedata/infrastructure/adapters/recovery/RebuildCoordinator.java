package com.guanchedata.infrastructure.adapters.recovery;

import com.google.gson.Gson;
import com.guanchedata.model.RebuildCommand;
import com.hazelcast.core.HazelcastInstance;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RebuildCoordinator {

    private static final Logger log = LoggerFactory.getLogger(RebuildCoordinator.class);
    private static final String REBUILD_TOPIC = "index.rebuild.command";

    private final HazelcastInstance hz;
    private final String brokerUrl;
    private final Gson gson = new Gson();

    public RebuildCoordinator(HazelcastInstance hz, String brokerUrl) {
        this.hz = hz;
        this.brokerUrl = brokerUrl;
    }

    public void initiateRebuild() {
        try {
            int clusterSize = hz.getCluster().getMembers().size();
            log.info("Initiating rebuild across {} nodes", clusterSize);

            long timestamp = System.currentTimeMillis();
            RebuildCommand command = new RebuildCommand(timestamp, clusterSize, "");

            publishRebuildCommand(command);

            log.info("Rebuild command broadcasted to all nodes");

        } catch (Exception e) {
            log.error("Error during rebuild coordination", e);
            throw new RuntimeException("Rebuild failed", e);
        }
    }

    private void publishRebuildCommand(RebuildCommand command) throws JMSException {
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        try (Connection conn = factory.createConnection();
             Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE)) {

            Topic topic = session.createTopic(REBUILD_TOPIC);
            MessageProducer producer = session.createProducer(topic);

            String json = gson.toJson(command);
            TextMessage message = session.createTextMessage(json);

            producer.send(message);
            log.info("Published rebuild command to topic: {}", REBUILD_TOPIC);
        }
    }
}
