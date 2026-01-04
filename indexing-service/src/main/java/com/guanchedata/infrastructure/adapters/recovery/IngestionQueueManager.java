package com.guanchedata.infrastructure.adapters.recovery;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicReference;

public class IngestionQueueManager {

    private IQueue<Integer> queue;
    private IAtomicReference<Boolean> queueInitialized;

    public IngestionQueueManager(HazelcastInstance hz) {
        this.queue = hz.getQueue("books");
        this.queueInitialized = hz.getCPSubsystem().getAtomicReference("queueInitialized");

        if (queueInitialized.get() == null) {
            queueInitialized.set(false);
        }
    }

    public void setupBookQueue(int startReference) {
        if (!queueInitialized.compareAndSet(false, true)) {
            System.out.println("Queue already initialized by another node");
            return;
        }

        new Thread(() -> populateQueueAsync(startReference), "Queue-Populator").start();
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
                System.out.printf("[Queue: %d/%d added (%.1f/sec)]%n", added, 100000-maxBookId, rate);
            }
        }

        System.out.printf("Queue COMPLETED: %d books in %.1fs (%.1f/sec)%n",
                added, (System.currentTimeMillis() - start) / 1000.0, added * 1000.0 / (System.currentTimeMillis() - start));
    }
}
