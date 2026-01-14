package com.guanchedata.application.usecases.ingestionservice;

import com.guanchedata.infrastructure.ports.BookDownloader;
import com.guanchedata.model.BookContent;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BookIngestionPeriodicExecutor {

    private final HazelcastInstance hazelcast;
    private final BookDownloader ingestBookService;
    private IQueue<Integer> queue;
    private ISet<Integer> indexingRegistry;

    public BookIngestionPeriodicExecutor(HazelcastInstance hazelcast, BookDownloader ingestBookService) {
        this.hazelcast = hazelcast;
        this.ingestBookService = ingestBookService;
        this.queue = this.hazelcast.getQueue("books");
        this.indexingRegistry = this.hazelcast.getSet("indexingRegistry");
    }

    public void startPeriodicExecution() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::execute, 0, 5, TimeUnit.MILLISECONDS);
    }

    private static final long RECOVERY_LOG_INTERVAL_MS = 20_000;
    private long lastRecoveryLogTime = 0;

    public void execute() {
        IMap<Integer, BookContent> datalake = this.hazelcast.getMap("datalake");
        if (datalake.keySet().size() < Integer.parseInt(System.getenv("INDEXING_BUFFER_FACTOR")) * this.hazelcast.getCluster().getMembers().stream().filter(m -> "indexer".equals(m.getAttribute("role"))).count()){
            try {
                if (this.queue.isEmpty()) {
                    logRecoveryIfNeeded();
                }
                else {
                    Integer bookId = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (bookId != null && !this.indexingRegistry.contains(bookId)) {
                        System.out.println("Ingesting book: " + bookId);
                        Map<String, Object> result = ingestBookService.ingest(bookId);
                        System.out.println("Result: " + result);
                    }
                    else {
                        System.out.println("Book {" + bookId + "} is already indexed. Skipping ingestion...");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void logRecoveryIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRecoveryLogTime >= RECOVERY_LOG_INTERVAL_MS) {
            System.out.println("(" + Instant.now() + ")" + " [INDEXER][RECOVERY] Rebuilding inverted index from disk. Ingestion paused.");
            lastRecoveryLogTime = now;
        }
    }

}
