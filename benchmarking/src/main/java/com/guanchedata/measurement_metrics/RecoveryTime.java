package com.guanchedata.measurement_metrics;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicReference;

public class RecoveryTime {

    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "none");
        Config config = new Config();

        String clusterName = System.getenv("HAZELCAST_CLUSTER_NAME");
        config.setClusterName(clusterName != null ? clusterName : "SearchEngine");

        NetworkConfig networkConfig = config.getNetworkConfig();

        String portEnv = System.getenv("HZ_PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            networkConfig.setPort(Integer.parseInt(portEnv));
        }
        networkConfig.setPortAutoIncrement(false);
        config.setProperty("hazelcast.wait.seconds.before.join", "0");

        JoinConfig join = networkConfig.getJoin();
        join.getMulticastConfig().setEnabled(false);
        join.getAutoDetectionConfig().setEnabled(false);

        String publicAddr = System.getenv("HZ_PUBLIC_ADDRESS");
        if (publicAddr != null && !publicAddr.isBlank()) {
            networkConfig.setPublicAddress(publicAddr);
            System.out.println(">> Configured Public Address: " + publicAddr);
        }

        String members = System.getenv("HZ_MEMBERS");
        if (members != null && !members.isBlank()) {
            join.getTcpIpConfig().setEnabled(true);
            for (String m : members.split(",")) {
                join.getTcpIpConfig().addMember(m.trim());
                System.out.println(">> Added Member to discovery list: " + m.trim());
            }
        }

        MapConfig mapConfig = new MapConfig("*");
        mapConfig.setBackupCount(1);
        config.addMapConfig(mapConfig);

        HazelcastInstance hz = Hazelcast.newHazelcastInstance(config);

        while (hz.getCluster().getMembers().size() < 2) {
            Thread.sleep(1000);
        }

        while (!hz.getPartitionService().isClusterSafe()) {
            Thread.sleep(500);
        }

        System.out.println("\n>>> CLUSTER IS SAFE.");

        IAtomicReference<Boolean> measuring = hz.getCPSubsystem().getAtomicReference("recovery-measuring-lock");
        measuring.compareAndSet(null,false);

        hz.getCluster().addMembershipListener(new MembershipListener() {
            @Override
            public void memberRemoved(MembershipEvent event) {
                if (measuring.compareAndSet(false, true)) {
                    System.out.println("!!! MEMBER REMOVED: " + event.getMember());
                    long startTime = System.currentTimeMillis();
                    measureRecovery(hz, startTime, measuring);
                }
            }

            @Override
            public void memberAdded(MembershipEvent event) {
                System.out.println("New member added: " + event.getMember());
            }
        });

        Thread.currentThread().join();
    }

    private static void measureRecovery(HazelcastInstance hz, long startTime, IAtomicReference<Boolean> measuring) {
        new Thread(() -> {
            try {
                while (!hz.getPartitionService().isClusterSafe()) {
                    Thread.sleep(50);
                }

                long recoveryTimeMs = System.currentTimeMillis() - startTime;
                System.out.println("RECOVERY TIME: " + recoveryTimeMs / 1000.0 + " s (" + recoveryTimeMs + " ms)");


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }finally {
                measuring.set(false);
            }
        }).start();
    }
}