package com.guanchedata.infrastructure.adapters.activemq;

import com.hazelcast.core.HazelcastInstance;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQInvertedIndexListener {

    private final ConnectionFactory factory;
    private final HazelcastInstance hazelcast;

    public ActiveMQInvertedIndexListener(HazelcastInstance hazelcast,
                                         String brokerUrl) {
        this.hazelcast = hazelcast;
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
    }

    public void startListening() {

    }
}
