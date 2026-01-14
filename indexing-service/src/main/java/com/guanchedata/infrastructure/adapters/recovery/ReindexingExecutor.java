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
        int startReference = this.invertedIndexRecovery.executeRecovery(); // recovers I.index from disk. does not make conflict with other reindexings
        this.ingestionQueueManager.setupBookQueue(startReference); // first indexer to finish reindexing creates queue
    }

    public void rebuildIndex() {
        this.hz.getSet("log").clear();
        this.hz.getSet("indexingRegistry").clear();
        this.hz.getMultiMap("inverted-index").clear();
        this.hz.getMap("bookMetadata").clear();
        //this.hz.getQueue("books").clear(); // ???????????
        this.invertedIndexRecovery.executeRecovery();

    }


}
