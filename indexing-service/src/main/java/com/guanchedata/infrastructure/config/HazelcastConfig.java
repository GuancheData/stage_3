package com.guanchedata.infrastructure.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.util.Arrays;

public class HazelcastConfig {

    public HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);

        config.getMemberAttributeConfig()
                .setAttribute("role", "indexer");

        String publicIp = System.getenv("PUBLIC_IP");
        String hzPort   = System.getenv("HZ_PORT");

        NetworkConfig networkConfig = config.getNetworkConfig();

        networkConfig.setPort(Integer.parseInt(hzPort));
        networkConfig.setPortAutoIncrement(false);
        networkConfig.setPublicAddress(publicIp + ":" + hzPort);

        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);
        join.getTcpIpConfig()
                //.setMembers(Arrays.asList())
                .setEnabled(true);

        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        return Hazelcast.newHazelcastInstance(config);
    }
}
