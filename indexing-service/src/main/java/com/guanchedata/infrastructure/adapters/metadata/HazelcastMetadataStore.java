package com.guanchedata.infrastructure.adapters.metadata;

import com.guanchedata.infrastructure.ports.MetadataStore;
import com.guanchedata.model.BookContent;
import com.guanchedata.model.BookMetadata;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastMetadataStore implements MetadataStore {

    private static final Logger log = LoggerFactory.getLogger(HazelcastMetadataStore.class);
    private final MetadataParser parser;
    private final IMap<Integer, BookContent> datalake;
    private final IMap<Integer, BookMetadata> metadataMap;

    public HazelcastMetadataStore(HazelcastInstance hazelcastInstance, MetadataParser parser) {
        this.parser = parser;
        this.metadataMap = hazelcastInstance.getMap("bookMetadata");
        this.datalake = hazelcastInstance.getMap("datalake");
    }

    @Override
    public void saveMetadata(int bookId, String header) {
        BookMetadata metadata = parser.parseFromHeader(header);
        metadataMap.put(bookId, metadata);
        datalake.remove(bookId);
    }
}
