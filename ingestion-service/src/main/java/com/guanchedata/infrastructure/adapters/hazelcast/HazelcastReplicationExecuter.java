package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.ports.ReplicationExecuter;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.BookReplicationCommand;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;

import java.util.List;

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
        addBookLocation(bookId);
        replicate(bookId);
    }

    @Override
    public void replicate(int bookId) {
        //FencedLock lock = hazelcast.getCPSubsystem().getLock("lock:book:" + bookId);
        //lock.lock();
        IQueue<BookReplicationCommand> booksToBeReplicated = hazelcast.getQueue("booksToBeReplicated");
        try {
            for (int i = 1; i < replicationFactor; i++) {
                booksToBeReplicated.put(new BookReplicationCommand(bookId, this.nodeInfoProvider.getNodeId()));
            }
            List<BookReplicationCommand> snapshot = List.copyOf(booksToBeReplicated);
            for (BookReplicationCommand book: snapshot){
                System.out.println(book.getId());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //lock.unlock();
        }
    }

    public void addBookLocation(int bookId) {
        MultiMap<Integer,NodeInfoProvider> bookLocations = this.hazelcast.getMultiMap("bookLocations");
        bookLocations.put(bookId, this.nodeInfoProvider);
    }

}
