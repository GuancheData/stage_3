package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.adapters.metadata.HazelcastMetadataStore;
import com.guanchedata.infrastructure.ports.BookStore;
import com.guanchedata.infrastructure.ports.IndexStore;
import com.guanchedata.infrastructure.ports.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IndexingService {
    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private final IndexStore indexStore;
    private final Tokenizer tokenizer;
    private final BookStore bookStore;
    private final HazelcastMetadataStore hazelcastMetadataStore;

    public IndexingService(IndexStore indexStore, Tokenizer tokenizer, BookStore bookStore, HazelcastMetadataStore hazelcastMetadataStore) {
        this.indexStore = indexStore;
        this.tokenizer = tokenizer;
        this.bookStore = bookStore;
        this.hazelcastMetadataStore = hazelcastMetadataStore;
    }

    public void indexDocument(int documentId) {
        log.info("Starting indexing for document: " + documentId);

        try {
            String[] content = bookStore.getBookContent(documentId);

            int tokenCount = generateInvertedIndex(content[1], documentId);
            this.hazelcastMetadataStore.saveMetadata(documentId, content[0]);
            log.info("Done indexing for document: {}. Token count: {}\n", documentId, tokenCount);

        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to index document: " + documentId, e);
        }
    }

    public int generateInvertedIndex(String body, int documentId) {
        List<String> tokens = tokenizer.tokenize(body);
        Map<String, Long> frequencies =
                tokens.parallelStream()
                        .map(String::toLowerCase)
                        .collect(Collectors.groupingBy(
                                t -> t,
                                Collectors.counting()
                        ));
        for (Map.Entry<String, Long> entry : frequencies.entrySet()) {
            String term = entry.getKey();
            Long frequency = entry.getValue();

            indexStore.addEntry(
                    term,
                    documentId + ":" + frequency
            );
        }
        indexStore.pushEntries();
        return tokens.size();
    }
}
