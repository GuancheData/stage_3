package com.guanchedata.infrastructure.adapters.metadata;

import com.guanchedata.infrastructure.ports.MetadataStore;
import com.guanchedata.model.BookMetadata;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastMetadataStore implements MetadataStore {
    private static final Logger log = LoggerFactory.getLogger(HazelcastMetadataStore.class);

    private final IMap<Integer, BookMetadata> metadataMap;

    public HazelcastMetadataStore(HazelcastInstance hazelcastInstance) {
        this.metadataMap = hazelcastInstance.getMap("bookMetadata");
        log.info("Connected to Hazelcast metadata store");
    }

    @Override
    public BookMetadata getMetadata(String bookId) {
        log.info("Getting metadata for: {}", bookId);

        Integer key = Integer.parseInt(bookId);
        BookMetadata metadata = metadataMap.get(key);

        log.info("Result: {}", (metadata != null ? metadata.getAuthor() : "NULL"));
        return metadata;
    }
}
