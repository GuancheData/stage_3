package com.guanchedata.infrastructure.adapters.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastConfig {

    public HazelcastInstance initHazelcast(String clusterName) {

        Config config = new Config();
        config.setClusterName(clusterName);
        config.getNetworkConfig().setPublicAddress(System.getenv("PUBLIC_IP"));
        config.getNetworkConfig().setPort(5701);
        config.getNetworkConfig().setPortAutoIncrement(false);
        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);

        join.getTcpIpConfig()
                .addMember(System.getenv("PUBLIC_IP"))
                //.addMember("")
                .setEnabled(true);

        return Hazelcast.newHazelcastInstance(config);
    }
}
