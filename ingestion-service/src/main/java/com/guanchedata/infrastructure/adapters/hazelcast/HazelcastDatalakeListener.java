package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.ReplicatedBook;
import com.guanchedata.util.DateTimePathGenerator;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HazelcastDatalakeListener extends AbstractEntryListener<Integer, ReplicatedBook> {

    private final NodeInfoProvider nodeInfoProvider;
    private final int replicationFactor;
    private final HazelcastInstance hazelcast;

    public HazelcastDatalakeListener(HazelcastInstance hazelcast,
                                     NodeInfoProvider nodeInfoProvider,
                                     int replicationFactor) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.replicationFactor = replicationFactor;
    }

    public void registerListener() {
        MultiMap<Integer, ReplicatedBook> datalake = hazelcast.getMultiMap("datalake");
        datalake.addEntryListener(this, true);
    }

    @Override
    public void entryAdded(EntryEvent<Integer, ReplicatedBook> event) {
        ReplicatedBook replicated = event.getValue();
        int bookId = event.getKey();

        if (replicated.getSourceNode().equals(nodeInfoProvider.getNodeId())) return;

        IMap<Integer, Integer> replicaCount = hazelcast.getMap("replication-count");
        int current = replicaCount.getOrDefault(bookId, 0);
        if (current >= replicationFactor) return;
        replicaCount.put(bookId, current + 1);

        saveRetrievedBook(bookId, replicated.getHeader(), replicated.getBody());
    }

    public void saveRetrievedBook(int bookId, String header, String body) {
        try {

            DateTimePathGenerator dateTimePathGenerator = new DateTimePathGenerator("datalake");
            // add env var for datalakepath?
            Path path = dateTimePathGenerator.generatePath();

            Path headerPath = path.resolve(String.format("%d_header.txt", bookId));
            Path contentPath = path.resolve(String.format("%d_body.txt", bookId));

            Files.writeString(headerPath, header);
            Files.writeString(contentPath, body);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
