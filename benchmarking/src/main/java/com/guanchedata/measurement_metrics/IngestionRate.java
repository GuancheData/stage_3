package com.guanchedata.measurement_metrics;

import com.guanchedata.infrastructure.adapters.bookprovider.BookDownloadLog;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import java.util.ArrayList;
import java.util.List;



public class IngestionRate {
    public static void main(String[] args) throws Exception {
        ClientConfig cc = new ClientConfig();
        cc.setClusterName("SearchEngine");
        cc.getNetworkConfig().addAddress("192.168.1.232:5701");
        HazelcastInstance hz = HazelcastClient.newHazelcastClient(cc);
        BookDownloadLog booklog = new BookDownloadLog(hz, "log");

        List<Double> rates = new ArrayList<>();

        for (int i = 0; i < 15; i++) {
            if (i >= 5) {
                long startCount = booklog.getAllDownloaded().size();
                long startTime = System.currentTimeMillis();

                Thread.sleep(1000);  // ← Tu ingestión real aquí

                long endCount = booklog.getAllDownloaded().size();
                long endTime = System.currentTimeMillis();
                double seconds = (endTime - startTime) / 1000.0;  // ← TIEMPO REAL
                double rate = (endCount - startCount) / seconds;   // ← ✓ DIVIDIDO
                rates.add(rate);
                System.out.printf("Iter %2d: %.3f books/s (%.1fs)%n", i+1, rate, seconds);
            } else {
                Thread.sleep(1000);
            }
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
