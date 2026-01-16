package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.adapters.bookprovider.BookStorageDate;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.util.GutenbergBookProvider;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HazelcastDatalakeListener {

    private final NodeInfoProvider nodeInfoProvider;
    private final HazelcastInstance hazelcast;
    private final GutenbergBookProvider bookProvider;
    private final BookStorageDate bookStorageDate;

    private final ExecutorService executorService;
    private volatile boolean active = true;

    public HazelcastDatalakeListener(HazelcastInstance hazelcast,
                                     NodeInfoProvider nodeInfoProvider,
                                     GutenbergBookProvider bookProvider,
                                     BookStorageDate bookStorageDate) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.bookProvider = bookProvider;
        this.bookStorageDate = bookStorageDate;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void registerListener() {
        executorService.submit(this::consumeQueue);
    }

    private void consumeQueue() {
        IQueue<Integer> queue = hazelcast.getQueue("booksToBeReplicated");

        while (active) {
            try {
                int bookId = queue.take();
                processBook(bookId, queue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void processBook(int bookId, IQueue<Integer> queue) {
        String myNodeId = nodeInfoProvider.getNodeId();
        IMap<Integer, Set<String>> replicatedNodesMap = hazelcast.getMap("replicatedNodesMap");
        Set<String> currentOwners = replicatedNodesMap.get(bookId);
        boolean iAlreadyHaveIt = currentOwners != null && currentOwners.contains(myNodeId);

        if (iAlreadyHaveIt) {
            try {
                Thread.sleep(200);
                queue.put(bookId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        System.out.println("Node " + myNodeId + " replicating book:  " + bookId);
        saveRetrievedBook(bookId);

        replicatedNodesMap.lock(bookId);
        try {
            Set<String> nodes = replicatedNodesMap.getOrDefault(bookId, new HashSet<>());
            nodes.add(myNodeId);
            replicatedNodesMap.put(bookId, nodes);
        } finally {
            replicatedNodesMap.unlock(bookId);
        }

        updateReplicationLog(bookId);
    }

    private void updateReplicationLog(int bookId) {
        IMap<Integer, Integer> replicationLog = hazelcast.getMap("replicationLog");
        replicationLog.lock(bookId);
        try {
            int count = replicationLog.getOrDefault(bookId, 0);
            replicationLog.put(bookId, count + 1);
        } finally {
            replicationLog.unlock(bookId);
        }
    }

    private void saveRetrievedBook(int bookId) {
        try {
            this.bookStorageDate.save(bookId, this.bookProvider.getBook(bookId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        this.active = false;
        this.executorService.shutdownNow();
    }
}