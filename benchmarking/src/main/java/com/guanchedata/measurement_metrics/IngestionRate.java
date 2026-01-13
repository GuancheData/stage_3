package com.guanchedata.measurement_metrics;

import com.guanchedata.infrastructure.adapters.bookprovider.BookDownloadLog;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import java.util.ArrayList;
import java.util.List;


public class IngestionRate {
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
        BookDownloadLog booklog = new BookDownloadLog(hz, "log");

        List<Double> rates = new ArrayList<>();

        int warmup = 5;
        String status;
        int count;

        for (int i = 0; i < 15; i++) {
            long startCount = booklog.getAllDownloaded().size();
            long startTime = System.currentTimeMillis();

            Thread.sleep(1000);

            long endCount = booklog.getAllDownloaded().size();
            long endTime = System.currentTimeMillis();
            double seconds = (endTime - startTime) / 1000.0;
            double rate = (endCount - startCount) / seconds;
            if (i >= warmup) {
                rates.add(rate);
                status = "Iter";
                count = i-warmup;
            }else{
                count = i;
                status = "warmup";
            }
            System.out.printf("%s %2d: %.3f books/s (%.1fs)%n", status, count+1, rate, seconds);
        }

        double avg = rates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double stdev = Math.sqrt(rates.stream().mapToDouble(r -> Math.pow(r - avg, 2)).average().orElse(0));
        System.out.printf("%n=== RESULTADO FINAL ===%n");
        System.out.printf("IngestionRate: %.3f ± %.3f books/s%n", avg, stdev);
        System.out.printf("(min=%.3f, avg=%.3f, max=%.3f)%n",
                rates.stream().min(Double::compareTo).orElse(0d),
                avg,
                rates.stream().max(Double::compareTo).orElse(0d));

        hz.shutdown();
    }
}
