package com.guanchedata.infrastructure.adapters.recovery;

import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicReference;

import java.util.ArrayList;
import java.util.List;

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
        List<Integer> batch = new ArrayList<>(batchSize);
        int added = 0;

        for (int i = maxBookId + 1; i <= 100000; i ++) {
            batch.add(i);
            if (batch.size() == batchSize || i == 100000) {
                boolean batchInserted = false;
                while (!batchInserted) {
                    try {
                        batchInserted = queue.addAll(batch);

                        if (!batchInserted) {
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                    }
                }

                added += batch.size();
                System.out.printf("[Queue: %d added (Batch insert)]%n", added);
                batch.clear();
            }
        }
        System.out.printf("Queue COMPLETED: %d books", added);
    }
}
