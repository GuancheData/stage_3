package com.guanchedata.infrastructure.adapters.web;

import com.guanchedata.infrastructure.ports.BookStore;
import com.guanchedata.infrastructure.ports.IndexStore;
import com.guanchedata.infrastructure.ports.MetadataStore;
import com.guanchedata.model.BookContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class IndexBook {

    private static final Logger log = LoggerFactory.getLogger(IndexBook.class);

    private final BookStore bookStore;
    private final IndexStore indexStore;
    private final MetadataStore metadataStore;
    private final IndexingStatusStore statusStore;
    private final TermFrequencyAnalyzer analyzer;

    public IndexBook(BookStore bookStore, IndexStore indexStore, MetadataStore metadataStore, IndexingStatusStore statusStore, TermFrequencyAnalyzer analyzer) {
        this.bookStore = bookStore;
        this.indexStore = indexStore;
        this.metadataStore = metadataStore;
        this.statusStore = statusStore;
        this.analyzer = analyzer;
    }

    public void execute(int documentId) {
        log.info("Starting local indexing for document: {}", documentId);

        if (!statusStore.markAsIndexed(documentId)) {
            log.info("Document {} already indexed. Skipping.", documentId);
            return;
        }

        try {
            BookContent content = bookStore.getBookContent(documentId);

            Map<String, Long> frequencies = analyzer.analyze(content.getBody());
            int totalTokens = analyzer.countTotalTokens(content.getBody());

            saveInvertedIndex(documentId, frequencies);
            metadataStore.saveMetadata(documentId, content.getHeader());

            indexStore.saveTokens(totalTokens);
            log.info("Done indexing for document: {}. Token count: {}\n", documentId, totalTokens);
        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage());
            throw new RuntimeException("Failed to index document: " + documentId, e);
        }
    }

    private void saveInvertedIndex(int documentId, Map<String, Long> frequencies) {
        String docIdStr = String.valueOf(documentId);
        frequencies.forEach((term, freq) ->
                indexStore.addEntry(term, docIdStr, freq)
        );
        indexStore.pushEntries();
    }
}