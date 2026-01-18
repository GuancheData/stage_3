package com.guanchedata.infrastructure.adapters.recovery;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;

public class IngestionQueueManager {

    private final IQueue<Integer> queue;
    private final IAtomicLong queueInitialized;
    private Thread currentPopulatorThread;

    public IngestionQueueManager(HazelcastInstance hz) {
        this.queue = hz.getQueue("books");
        this.queueInitialized = hz.getCPSubsystem().getAtomicLong("queueInitialized");
    }

    public synchronized void stopPopulation() {
        if (currentPopulatorThread != null && currentPopulatorThread.isAlive()) {
            System.out.println("[QueueManager] Stopping existing population thread...");
            currentPopulatorThread.interrupt();
        }
    }

    public synchronized void setupBookQueue(int startReference) {
        if (!queueInitialized.compareAndSet(0, 1)) {
            System.out.println("Queue already initialized by another node");
            return;
        }

        stopPopulation();

        currentPopulatorThread = new Thread(() -> populateQueueAsync(startReference), "Queue-Populator");
        currentPopulatorThread.start();
    }

    private void populateQueueAsync(int maxBookId) {
        System.out.println("Initializing queue from " + (maxBookId + 1));

        int batchSize = 1000;
        int added = 0;
        long start = System.currentTimeMillis();

        for (int i = maxBookId + 1; i <= 100000; i += batchSize) {

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("[QueueManager] Population thread interrupted. Stopping.");
                return;
            }

            int end = Math.min(i + batchSize - 1, 100000);
            boolean allAdded = true;

            for (int bookId = i; bookId <= end; bookId++) {
                if (Thread.currentThread().isInterrupted()) return;

                if (!queue.offer(bookId)) {
                    allAdded = false;
                    break;
                }
                added++;
            }

            if (!allAdded) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restaurar flag de interrupciÃ³n
                    System.out.println("[QueueManager] Thread interrupted during sleep.");
                    return;
                }
                i -= batchSize;
                continue;
            }

            if (added > 0 && added % 10000 == 0) {
                long elapsed = System.currentTimeMillis() - start;
                double rate = added * 1000.0 / elapsed;
                System.out.printf("[Queue: %d/%d added (%.1f/sec)]%n", added, 100000 - maxBookId, rate);
            }
        }

        System.out.printf("Queue COMPLETED: %d books in %.1fs (%.1f/sec)%n",
                added, (System.currentTimeMillis() - start) / 1000.0, added * 1000.0 / (System.currentTimeMillis() - start));
    }
}