package com.guanchedata.infrastructure.adapters.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastStarter {
    public static HazelcastInstance startHazelcast() {
        Config config = new Config();
        config.setClusterName("CLUSTER");

        MapConfig mapConfig = new MapConfig("BooksToBe")
                .setBackupCount(2)
                .setAsyncBackupCount(1);

        config.addMapConfig(mapConfig);

        return Hazelcast.newHazelcastInstance(config);
    }
}
