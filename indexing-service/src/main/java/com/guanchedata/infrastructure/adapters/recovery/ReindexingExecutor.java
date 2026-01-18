package com.guanchedata.infrastructure.adapters.recovery;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReindexingExecutor {
    private static final Logger log = LoggerFactory.getLogger(ReindexingExecutor.class);

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

    public void rebuildIndex() {
        this.ingestionQueueManager.stopPopulation();

        this.hz.getSet("log").clear();
        this.hz.getSet("indexingRegistry").clear();
        this.hz.getMap("inverted-index").clear();
        this.hz.getMap("bookMetadata").clear();
        this.hz.getQueue("books").clear();

        this.hz.getCPSubsystem().getAtomicLong("queueInitialized").set(0);

        this.executeRecovery();

        log.info("Rebuild completed");
    }
}