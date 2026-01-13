package com.guanchedata.infrastructure.adapters.hazelcast;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import java.util.Arrays;

public class HazelcastConfig {

    public HazelcastInstance initHazelcast(String clusterName) {

        Config config = new Config();
        config.setClusterName(clusterName);

        MapConfig mapCfg = new MapConfig("inverted-index")
                .setBackupCount(2)
                .setAsyncBackupCount(1);
        config.addMapConfig(mapCfg);

        MapConfig mapCfg2 = new MapConfig("bookMetadata")
                .setBackupCount(2)
                .setAsyncBackupCount(1);
        config.addMapConfig(mapCfg2);

        MapConfig mapCfg3 = new MapConfig("datalake")
                .setBackupCount(2)
                .setAsyncBackupCount(1);
        config.addMapConfig(mapCfg3);

        MapConfig mapCfg4= new MapConfig("inverted-index");
        EvictionConfig evictionConfig = new EvictionConfig()
                .setEvictionPolicy(EvictionPolicy.NONE);
        mapCfg4.setEvictionConfig(evictionConfig);
        mapCfg4.setBackupCount(1);
        config.addMapConfig(mapCfg4);

        //config.getNetworkConfig().setPublicAddress(System.getenv("PUBLIC_IP"));
        //config.getNetworkConfig().setPort(5701);
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(Integer.parseInt(System.getenv("HZ_PORT")));
        networkConfig.setPortAutoIncrement(false);
        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);

        String publicAddr = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddr != null && !publicAddr.isBlank()) {
            networkConfig.setPublicAddress(publicAddr);
        }

        String members = System.getenv("HZ_MEMBERS");
        if (members != null && !members.isBlank()) {
            join.getTcpIpConfig().setEnabled(true);
            for (String m : members.split(",")) {
                join.getTcpIpConfig().addMember(m.trim());
            }
        }

        return Hazelcast.newHazelcastInstance(config);
    }
}
