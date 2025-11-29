package com.guanchedata.application.usecases.ingestionservice;

import com.guanchedata.infrastructure.ports.BookDownloader;
import com.guanchedata.model.ReplicatedBook;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import java.util.Map;

public class BookIngestionPeriodicExecutor {

    private final HazelcastInstance hazelcast;
    private final BookDownloader ingestBookService;
    private IQueue<Integer> queue;

    public BookIngestionPeriodicExecutor(HazelcastInstance hazelcast, BookDownloader ingestBookService) {
        this.hazelcast = hazelcast;
        this.ingestBookService = ingestBookService;
        this.queue = this.hazelcast.getQueue("books");
    }

    public void startPeriodicExecution() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        //scheduler.scheduleAtFixedRate(this::execute, 0, 0, TimeUnit.SECONDS);
        scheduler.scheduleWithFixedDelay(this::execute, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void execute() {

        try {
            Integer bookId = this.queue.take();
            System.out.println("\nIngesting book: " + bookId);
            Map<String, Object> result = ingestBookService.ingest(bookId);
            System.out.println("Result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setupBookQueue() {
        MultiMap<Integer, ReplicatedBook> datalake = hazelcast.getMultiMap("datalake");

        if (this.queue.isEmpty()) {
            int maxBookId = datalake.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

            for (int i = maxBookId + 1; i <= 100000; i++) {
                this.queue.add(i);
            }
            System.out.println("Queue initialized with books from " + (maxBookId + 1) + " to 100000");
        } else {
            System.out.println("Queue already has elements, skipping initialization.");
        }
    }

}
