package com.guanchedata.infrastructure.adapters.indexstore;

import com.guanchedata.infrastructure.ports.IndexStore;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HazelcastIndexStore implements IndexStore {

    private static final Logger log = LoggerFactory.getLogger(HazelcastIndexStore.class);

    private final MultiMap<String, String> invertedIndex;

    private final Map<String, Set<String>> invertedIndexEntry =
            new ConcurrentHashMap<>();


    public HazelcastIndexStore(HazelcastInstance hazelcastInstance) {
        this.invertedIndex = hazelcastInstance.getMultiMap("inverted-index");
        log.info("Hazelcast inverted index initialized");
    }

    @Override
    public void addEntry(String term, String documentId) {
        invertedIndexEntry
                .computeIfAbsent(term,
                        k -> ConcurrentHashMap.newKeySet())
                .add(documentId);
    }

    @Override
    public void pushEntries() {
        Map<String, Set<String>> snapshot = new HashMap<>(invertedIndexEntry);

        invertedIndexEntry.clear();

        for (var entry : snapshot.entrySet()) {
            String term = entry.getKey();
            for (String value : entry.getValue()) {
                invertedIndex.put(term, value);
            }
        }
    }

    @Override
    public Set<String> getDocuments(String term) {
        Collection<String> docs = invertedIndex.get(term);
        return new HashSet<>(docs);
    }

    @Override
    public void clear() {
        invertedIndex.clear();
        log.info("Inverted index cleared");
    }
}
