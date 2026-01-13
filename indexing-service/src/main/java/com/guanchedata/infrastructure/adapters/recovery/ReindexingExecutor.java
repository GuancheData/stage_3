package com.guanchedata.infrastructure.adapters.recovery;

import com.hazelcast.core.HazelcastInstance;

public class ReindexingExecutor {

    private final InvertedIndexRecovery invertedIndexRecovery;
    private final HazelcastInstance hz;
    private final IngestionQueueManager ingestionQueueManager;

    public ReindexingExecutor(InvertedIndexRecovery invertedIndexRecovery, HazelcastInstance hz, IngestionQueueManager ingestionQueueManager) {
        this.invertedIndexRecovery = invertedIndexRecovery;
        this.hz = hz;
        this.ingestionQueueManager = ingestionQueueManager;
    }

    public void executeRecovery(){
        int startReference = this.invertedIndexRecovery.executeRecovery();
        this.ingestionQueueManager.setupBookQueue(startReference);
    }

    public void rebuildFromDisk() {
        int startReference = this.invertedIndexRecovery.executeRecovery();
        this.ingestionQueueManager.setupBookQueue(startReference);
    }
}
