package com.guanchedata.infrastructure.adapters.indexstore;

import com.guanchedata.infrastructure.ports.IndexStore;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class HazelcastIndexStore implements IndexStore {
    private static final Logger log = LoggerFactory.getLogger(HazelcastIndexStore.class);
    private final MultiMap<String, String> invertedIndex;
    private final ISet<Integer> indexingRegistry;

    public HazelcastIndexStore(HazelcastInstance hazelcastInstance) {
        this.invertedIndex = hazelcastInstance.getMultiMap("inverted-index");
        this.indexingRegistry = hazelcastInstance.getSet("indexingRegistry");
        log.info("Hazelcast inverted index initialized");
    }

    @Override
    public void addEntry(String term, String documentId) {
        invertedIndex.put(term, documentId);
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

    public ISet<Integer> retrieveIndexingRegistry(){
        return this.indexingRegistry;
    }
}
