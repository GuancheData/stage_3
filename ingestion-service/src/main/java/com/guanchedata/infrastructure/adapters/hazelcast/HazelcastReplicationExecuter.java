package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.ports.ReplicationExecuter;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.BookReplicationCommand;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastReplicationExecuter implements ReplicationExecuter {

    private final HazelcastInstance hazelcast;
    private final NodeInfoProvider nodeInfoProvider;
    private final int replicationFactor;

    public HazelcastReplicationExecuter(HazelcastInstance hazelcast, NodeInfoProvider nodeInfoProvider, int replicationFactor) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.replicationFactor = replicationFactor;
    }

    public void execute(int bookId) {
        replicate(bookId);
    }

    @Override
    public void replicate(int bookId) {
        IQueue<BookReplicationCommand> booksToBeReplicated = hazelcast.getQueue("booksToBeReplicated");
        try {
            for (int i = 1; i < replicationFactor; i++) {
                booksToBeReplicated.put(new BookReplicationCommand(bookId, this.nodeInfoProvider.getNodeId()));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
