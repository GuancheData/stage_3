package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.ports.ReplicationExecuter;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.ReplicatedBook;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.multimap.MultiMap;

public class HazelcastReplicationExecuter implements ReplicationExecuter {

    private final HazelcastInstance hazelcast;
    private final NodeInfoProvider nodeInfoProvider;

    public HazelcastReplicationExecuter(HazelcastInstance hazelcast, NodeInfoProvider nodeInfoProvider) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
    }

    @Override
    public void replicate(int bookId, String header, String body) {
        FencedLock lock = hazelcast.getCPSubsystem().getLock("lock:book:" + bookId);
        lock.lock();
        try {
            MultiMap<Integer, ReplicatedBook> datalake = hazelcast.getMultiMap("datalake");
            datalake.put(bookId, new ReplicatedBook(header, body, nodeInfoProvider.getNodeId()));
            System.out.println("Current MultiMap keys: " + datalake.keySet());
        } finally {
            lock.unlock();
        }
    }
}
