package com.guanchedata.infrastructure.adapters.indexstore;
import com.guanchedata.infrastructure.ports.IndexStore;
import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HazelcastIndexStore implements IndexStore {
    private static final Logger log = LoggerFactory.getLogger(HazelcastIndexStore.class);
    private final IMap<String, Set<String>> invertedIndex;
    private final Map<String, Set<String>> invertedIndexEntry;
    private final ISet<Integer> indexingRegistry;
    private final HazelcastInstance hz;

    public HazelcastIndexStore(HazelcastInstance hazelcastInstance) {
        this.hz = hazelcastInstance;
        this.invertedIndex = hz.getMap("inverted-index");
        this.indexingRegistry = hz.getSet("indexingRegistry");
        this.invertedIndexEntry = new ConcurrentHashMap<>();
        log.info("Hazelcast inverted index initialized");
    }

    @Override
    public void addEntry(String term, String documentId) {
        invertedIndexEntry
                .computeIfAbsent(term, k -> ConcurrentHashMap.newKeySet())
                .add(documentId);
    }

    @Override
    public void pushEntries() {
        invertedIndexEntry.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    String term = entry.getKey();
                    Set<String> newValues = new HashSet<>(entry.getValue());

                    invertedIndex.merge(term, newValues, (existing, incoming) -> {
                        existing.addAll(incoming);
                        return existing;
                    });
                });

        invertedIndexEntry.clear();
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

    public void saveTokens(Integer tokenCount) {
        if (hz.getCPSubsystem().getAtomicLong("token_counter_activator").get() == 1L) {
            hz.getCPSubsystem().getAtomicLong("token_counter").addAndGet(tokenCount);
        }
    }
}
