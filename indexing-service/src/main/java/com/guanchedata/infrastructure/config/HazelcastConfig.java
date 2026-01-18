package com.guanchedata.infrastructure.config;

import com.guanchedata.model.BookContentSerializer;
import com.guanchedata.model.BookMetadataSerializer;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastConfig {

    public static final String INDEX_MAP = "inverted-index";
    public static final String METADATA_MAP = "bookMetadata";
    public static final String DATALAKE_MAP = "datalake";

    public HazelcastInstance initHazelcast(String clusterName) {
        Config config = new Config();
        config.setClusterName(clusterName);

        config.getSerializationConfig()
                .getCompactSerializationConfig()
                .addSerializer(new BookContentSerializer())
                .addSerializer(new BookMetadataSerializer());

        setMembersConfig(config);
        setMapConfig(config);
        setNetworkConfig(config);
        setJoinConfig(config);

        return Hazelcast.newHazelcastInstance(config);
    }

    private void setMembersConfig(Config config) {
        config.getMemberAttributeConfig()
                .setAttribute("role", "indexer");
    }

    private void setMapConfig(Config config) {
        MapConfig invertedIndexReplicaConfig = new MapConfig(INDEX_MAP)
                .setBackupCount(2)
                .setAsyncBackupCount(1);
        config.addMapConfig(invertedIndexReplicaConfig);

        MapConfig bookMetadataReplicaConfig = new MapConfig(METADATA_MAP)
                .setBackupCount(2)
                .setAsyncBackupCount(1);
        config.addMapConfig(bookMetadataReplicaConfig);

        MapConfig datalakeReplicaConfig = new MapConfig(DATALAKE_MAP)
                .setBackupCount(2)
                .setAsyncBackupCount(1);
        config.addMapConfig(datalakeReplicaConfig);
    }

    private void setNetworkConfig(Config config){
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(Integer.parseInt(System.getenv("HZ_PORT")));
        networkConfig.setPortAutoIncrement(false);
        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        String publicAddr = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddr != null && !publicAddr.isBlank()) {
            networkConfig.setPublicAddress(publicAddr);
        }
    }

    private void setJoinConfig(Config config) {
        JoinConfig join = config.getNetworkConfig().getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);

        String members = System.getenv("HZ_MEMBERS");
        if (members != null && !members.isBlank()) {
            join.getTcpIpConfig().setEnabled(true);
            for (String m : members.split(",")) {
                join.getTcpIpConfig().addMember(m.trim());
            }
        }
    }
}
