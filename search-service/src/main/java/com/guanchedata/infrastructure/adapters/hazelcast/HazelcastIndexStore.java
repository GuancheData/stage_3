package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.ports.IndexStore;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HazelcastIndexStore implements IndexStore {
    private static final Logger log = LoggerFactory.getLogger(HazelcastIndexStore.class);
    private final IMap<String, Set<String>> invertedIndex;

    public HazelcastIndexStore(HazelcastInstance hazelcastInstance) {
        this.invertedIndex = hazelcastInstance.getMap("inverted-index");
        log.info("Connected to Hazelcast inverted index");
    }

    @Override
    public Set<String> getDocuments(String term) {
        Collection<String> docs = invertedIndex.get(term);

        if (docs == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(docs);
    }
}
