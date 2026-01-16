package com.guanchedata.infrastructure.config;

import com.guanchedata.model.BookContentSerializer;
import com.guanchedata.model.BookMetadataSerializer;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastConfig {

    public HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);

        config.getMemberAttributeConfig()
                .setAttribute("role", "indexer");

        config.getSerializationConfig()
                .getCompactSerializationConfig()
                .addSerializer(new BookContentSerializer())
                .addSerializer(new BookMetadataSerializer());


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
