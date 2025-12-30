package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.adapters.metadata.HazelcastMetadataStore;
import com.guanchedata.infrastructure.ports.BookStore;
import com.guanchedata.infrastructure.ports.IndexStore;
import com.guanchedata.infrastructure.ports.Tokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import java.util.Set;

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
        //System.out.println("Starting indexing for document: " + documentId);

        try {
            String[] content = bookStore.getBookContent(documentId); // get header and body

            // inverted index method
            int tokenCount = generateInvertedIndex(content[1], documentId);
            // metadata method
            this.hazelcastMetadataStore.saveMetadata(documentId, content[0]);
            //System.out.println("Done indexing for document: " + documentId + ". Token count: " + tokenCount);
            log.info("Done indexing for document: {}. Token count: {}\n", documentId, tokenCount);

        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage(), e);
            throw new RuntimeException("Failed to index document: " + documentId, e);
        }
    }

    public int generateInvertedIndex(String body, int documentId) {
        Set<String> tokens = tokenizer.tokenize(body);

        int tokenCount = 0;
        for (String token : tokens) {
            String normalizedToken = token.toLowerCase();
            indexStore.addEntry(normalizedToken, String.valueOf(documentId));
            tokenCount++;
        }
        return tokenCount;
    }
}
