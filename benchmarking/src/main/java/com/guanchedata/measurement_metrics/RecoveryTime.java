package com.guanchedata.measurement_metrics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;

import java.util.concurrent.atomic.AtomicBoolean;

public class RecoveryTime {
    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "none");
        ClientConfig cc = new ClientConfig();
        cc.setClusterName("SearchEngine");
        cc.getNetworkConfig().addAddress("");
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(cc);

        while (!hz.getPartitionService().isClusterSafe()) {
            Thread.sleep(500);
        }

        System.out.println("Cluster is SAFE. Benchmark armed.");

        AtomicBoolean measuring = new AtomicBoolean(false);

        hz.getCluster().addMembershipListener(new MembershipListener() {

            @Override
            public void memberRemoved(MembershipEvent event) {
                if (measuring.compareAndSet(false, true)) {
                    System.out.println("Member removed: " + event.getMember());
                    long startTime = System.currentTimeMillis();
                    measureRecovery(hz, startTime);
                }
            }

            @Override
            public void memberAdded(MembershipEvent event) {
            }
        });

    }

    private static void measureRecovery(HazelcastInstance hz, long startTime) {
        new Thread(() -> {
            try {
                while (!hz.getPartitionService().isClusterSafe()) {
                    Thread.sleep(200);
                }

                long recoveryTimeMs = System.currentTimeMillis() - startTime;
                System.out.println("Recovery time: " + recoveryTimeMs / 1000.0 + " s");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

}
