package com.guanchedata.application.usecases.ingestionservice;

import com.guanchedata.infrastructure.ports.BookDownloader;
import com.guanchedata.model.BookContent;
import com.guanchedata.model.NodeInfoProvider;
import com.hazelcast.cluster.Member;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
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
    private ISet<Integer> indexingRegistry;

    public BookIngestionPeriodicExecutor(HazelcastInstance hazelcast, BookDownloader ingestBookService) {
        this.hazelcast = hazelcast;
        this.ingestBookService = ingestBookService;
        this.queue = this.hazelcast.getQueue("books");
        this.indexingRegistry = this.hazelcast.getSet("indexingRegistry");
    }

    public void startPeriodicExecution() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::execute, 0, 1, TimeUnit.MILLISECONDS);
    }

    public void execute() {
        System.out.print("Indexers alive: ");
        System.out.println(this.hazelcast.getCluster().getMembers().stream().filter(m -> "indexer".equals(m.getAttribute("role"))).count());

        IMap<Integer, BookContent> datalake = this.hazelcast.getMap("datalake");
        if (datalake.keySet().size() < Integer.parseInt(System.getenv("INDEXING_BUFFER_FACTOR")) * this.hazelcast.getCluster().getMembers().stream().filter(m -> "indexer".equals(m.getAttribute("role"))).count()){
            try {
                if (this.queue.isEmpty()) {
                    System.out.println("A node is executing inverted index recovery from disk. Ingestion will start when it's done");
                }
                else {
                    Integer bookId = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (bookId != null && !this.indexingRegistry.contains(bookId)) {
                        System.out.println("\nIngesting book: " + bookId);
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
}
