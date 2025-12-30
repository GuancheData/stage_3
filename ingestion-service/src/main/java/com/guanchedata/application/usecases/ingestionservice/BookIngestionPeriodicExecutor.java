package com.guanchedata.application.usecases.ingestionservice;

import com.guanchedata.infrastructure.ports.BookDownloader;
import com.guanchedata.model.BookContent;
import com.guanchedata.model.NodeInfoProvider;
import com.hazelcast.cluster.Member;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BookIngestionPeriodicExecutor {

    private final HazelcastInstance hazelcast;
    private final BookDownloader ingestBookService;
    private IQueue<Integer> queue;
    private final AtomicBoolean queueInitialized = new AtomicBoolean(false);

    public BookIngestionPeriodicExecutor(HazelcastInstance hazelcast, BookDownloader ingestBookService) {
        this.hazelcast = hazelcast;
        this.ingestBookService = ingestBookService;
        this.queue = this.hazelcast.getQueue("books");
    }

    public void startPeriodicExecution() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::execute, 0, 5, TimeUnit.SECONDS);
    }

    public void execute() {
        System.out.print("Indexers alive: ");
        System.out.println(this.hazelcast.getCluster().getMembers().stream().filter(m -> "indexer".equals(m.getAttribute("role"))).count());
        IMap<Integer, BookContent> datalake = this.hazelcast.getMap("datalake");
        if (datalake.keySet().size() < Integer.parseInt(System.getenv("INDEXING_BUFFER_FACTOR")) * this.hazelcast.getCluster().getMembers().stream().filter(m -> "indexer".equals(m.getAttribute("role"))).count()){
            try {
                Integer bookId = queue.poll(100, TimeUnit.MILLISECONDS);
                if (bookId != null) {
                    System.out.println("\nIngesting book: " + bookId);
                    Map<String, Object> result = ingestBookService.ingest(bookId);
                    System.out.println("Result: " + result);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setupBookQueue() {
        if (!queueInitialized.compareAndSet(false, true)) {
            System.out.println("Queue initalized");
            return;
        }

        MultiMap<Integer, NodeInfoProvider> bookLocations = hazelcast.getMultiMap("bookLocations");
        int maxBookId = bookLocations.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        new Thread(() -> populateQueueAsync(maxBookId), "Queue-Populator").start();
    }

    private void populateQueueAsync(int maxBookId) {
        System.out.println("Initializing queue from " + (maxBookId + 1));

        int batchSize = 1000;
        int added = 0;
        long start = System.currentTimeMillis();

        for (int i = maxBookId + 1; i <= 100000; i += batchSize) {
            int end = Math.min(i + batchSize - 1, 100000);

            boolean allAdded = true;
            for (int bookId = i; bookId <= end; bookId++) {
                if (!queue.offer(bookId)) {
                    allAdded = false;
                    break;
                }
                added++;
            }

            if (!allAdded) {
                System.out.println("Queue full in book " + i + ", pausing...");
                try { Thread.sleep(5000); } catch (InterruptedException e) {}
                continue;
            }

            if (added % 10000 == 0) {
                long elapsed = System.currentTimeMillis() - start;
                double rate = added * 1000.0 / elapsed;
                System.out.printf("Queue: %d/%d added (%.1f/sec)%n", added, 100000-maxBookId, rate);
            }
        }

            System.out.printf("Queue COMPLETED: %d books in %.1fs (%.1f/sec)%n",
                added, (System.currentTimeMillis() - start) / 1000.0, added * 1000.0 / (System.currentTimeMillis() - start));
    }
}
