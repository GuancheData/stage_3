package com.guanchedata.measurement_metrics;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import java.util.ArrayList;
import java.util.List;

public class IndexingThroughput {
    public static void main(String[] args) throws Exception {
        System.setProperty("hazelcast.logging.type", "none");
        ClientConfig cc = new ClientConfig();

        String clusterName = System.getenv("HAZELCAST_CLUSTER_NAME");
        cc.setClusterName(clusterName != null ? clusterName : "SearchEngine");

        String membersEnv = System.getenv("HZ_MEMBERS");
        if (membersEnv != null && !membersEnv.isBlank()) {
            System.out.println("Configuring Client Members: " + membersEnv);
            for (String member : membersEnv.split(",")) {
                cc.getNetworkConfig().addAddress(member.trim());
            }
        } else {
            cc.getNetworkConfig().addAddress("ingestion1", "indexing1");
        }

        System.out.println("Attempting to connect to Hazelcast Cluster...");
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(cc);

        IAtomicLong activation = hz.getCPSubsystem().getAtomicLong("token_counter_activator");
        IAtomicLong tokens = hz.getCPSubsystem().getAtomicLong("token_counter");

        List<Double> rates = new ArrayList<>();
        int warmupIterations = 5;
        int measurementIterations = 10;
        int totalIterations = warmupIterations + measurementIterations;

        String status;
        int count;

        for (int i = 0; i < totalIterations; i++) {
            tokens.set(0);
            activation.set(1);

            long startTime = System.currentTimeMillis();
            Thread.sleep(10000);
            long endTime = System.currentTimeMillis();

            long tokensCounter = tokens.get();
            activation.set(0);

            double seconds = (endTime - startTime) / 1000.0;
            double tokensPerSecond = tokensCounter / seconds;

            if (i >= warmupIterations) {
                rates.add(tokensPerSecond);
                status = "Iter";
                count = i - warmupIterations;
            } else {
                status = "warmup";
                count = i;
            }

            System.out.printf("%s %2d: %.1f tokens/s (%.1fs) [tokens=%d]%n",
                    status, count + 1, tokensPerSecond, seconds, tokensCounter);
        }

        double avg = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdev = Math.sqrt(rates.stream().mapToDouble(r -> Math.pow(r - avg, 2)).average().orElse(0.0));

        System.out.printf("%n=== RESULTADO FINAL ===%n");
        System.out.printf("IndexingThroughput: %.1f ± %.1f tokens/s%n", avg, stdev);
        System.out.printf("(min=%.1f, avg=%.1f, max=%.1f)%n",
                rates.stream().min(Double::compareTo).orElse(0.0),
                avg,
                rates.stream().max(Double::compareTo).orElse(0.0));

        hz.shutdown();
    }
}
