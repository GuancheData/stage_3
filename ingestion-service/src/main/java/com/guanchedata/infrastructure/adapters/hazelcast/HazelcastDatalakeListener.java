package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.adapters.bookprovider.BookStorageDate;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.ReplicatedBook;
import com.guanchedata.util.GutenbergBookProvider;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;

public class HazelcastDatalakeListener {

    private final NodeInfoProvider nodeInfoProvider;
    private final HazelcastInstance hazelcast;
    private final GutenbergBookProvider bookProvider;
    private final BookStorageDate bookStorageDate;

    public HazelcastDatalakeListener(HazelcastInstance hazelcast,
                                     NodeInfoProvider nodeInfoProvider,
                                     GutenbergBookProvider bookProvider,
                                     BookStorageDate bookStorageDate) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.bookProvider = bookProvider;
        this.bookStorageDate = bookStorageDate;
    }

    public void registerListener() {
        IQueue<ReplicatedBook> booksToBeReplicated = hazelcast.getQueue("booksToBeReplicated");

        booksToBeReplicated.addItemListener(new ItemListener<ReplicatedBook>() {
            @Override
            public void itemAdded(ItemEvent<ReplicatedBook> itemEvent) {
                processBook(itemEvent.getItem());
            }

            @Override
            public void itemRemoved(ItemEvent<ReplicatedBook> itemEvent) {}
        }, true);
    }

    private void processBook(ReplicatedBook replicated) {
        if (replicated.getSourceNode().equals(nodeInfoProvider.getNodeId())) return;
        IMap<Integer,Integer> replicationLog = hazelcast.getMap("replicationLog");
        saveRetrievedBook(replicated.getId());
        replicationLog.put(replicated.getId(), replicationLog.getOrDefault(replicated.getId(), 1) + 1);
        System.out.println(replicationLog);
    }

    private void saveRetrievedBook(int bookId) {
        try {
            this.bookStorageDate.save(bookId,this.bookProvider.getBook(bookId));
            addBookLocation(bookId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addBookLocation(int bookId) {
        MultiMap<Integer,NodeInfoProvider> bookLocations = this.hazelcast.getMultiMap("bookLocations");
        bookLocations.put(bookId, this.nodeInfoProvider);
    }
}
