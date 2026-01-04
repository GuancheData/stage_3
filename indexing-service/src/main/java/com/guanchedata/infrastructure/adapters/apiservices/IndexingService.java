package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.adapters.metadata.HazelcastMetadataStore;
import com.guanchedata.infrastructure.ports.BookStore;
import com.guanchedata.infrastructure.ports.IndexStore;
import com.guanchedata.infrastructure.ports.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

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
            indexResolvedDocument(documentId, content[0], content[1]);

        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to index document: " + documentId, e);
        }
    }

    public void indexLocalDocument(int documentId, String header, String body) {
        log.info("Starting local indexing for document: {}", documentId);
        try {
            indexResolvedDocument(documentId, header, body);
        } catch (Exception e) {
            log.error("Error indexing local document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to index local document: " + documentId, e);
        }
    }

    public void indexResolvedDocument(int documentId, String header, String body) {
        registerIndexAction(documentId);
        int tokenCount = generateInvertedIndex(body, documentId);
        hazelcastMetadataStore.saveMetadata(documentId, header);

        log.info(
                "Done indexing for document: {}. Token count: {}",
                documentId,
                tokenCount
        );
    }

    private int generateInvertedIndex(String body, int documentId) {
        List<String> tokens = tokenizer.tokenize(body);

        Map<String, Integer> frequencies = new HashMap<>();
        for (String token : tokens) {
            String normalizedToken = token.toLowerCase();
            frequencies.put(normalizedToken, frequencies.getOrDefault(normalizedToken, 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : frequencies.entrySet()) {
            String term = entry.getKey();
            Integer frequency = entry.getValue();

            indexStore.addEntry(term, String.valueOf(documentId) + ":" + frequency);
        }

        indexStore.pushEntries();

        return tokens.size();
    }


    private void registerIndexAction(int documentId) {
        Collection<Integer> indexingRegistry =
                indexStore.retrieveIndexingRegistry();

        indexingRegistry.add(documentId);
    }
}
