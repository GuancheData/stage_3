package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.ports.IndexStore;
import com.guanchedata.infrastructure.ports.MetadataStore;
import com.guanchedata.model.BookMetadata;

import java.util.*;
import java.util.logging.Logger;

public class SearchService {
    private static final Logger log = Logger.getLogger(SearchService.class.getName());

    private final IndexStore indexStore;
    private final MetadataStore metadataStore;
    private final String sortingCriteria;

    public SearchService(IndexStore indexStore, MetadataStore metadataStore, String sortingCriteria) {
        this.indexStore = indexStore;
        this.metadataStore = metadataStore;
        this.sortingCriteria = sortingCriteria;
    }

    public List<SearchResult> search(String query, String author, String language, Integer year) {
        long startTime = System.currentTimeMillis();

        Map<String, Integer> contentResults = searchByContent(query);
        Map<String, Integer> finalResults = filterByMetadata(contentResults, author, language, year);

        List<SearchResult> enrichedResults = enrichWithMetadata(finalResults);

        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("Search: query='%s', author='%s', language='%s', year=%s, results=%d, time=%dms",
                query, author, language, year, enrichedResults.size(), duration));

        return enrichedResults;
    }

    private Map<String, Integer> searchByContent(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyMap();
        }

        String[] terms = query.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim()
                .split("\\s+");

        Map<String, Integer> documentFrequencies = new HashMap<>();

        for (String term : terms) {
            if (term.isEmpty() || term.length() <= 2) continue;

            Set<String> documentsForTerm = indexStore.getDocuments(term);

            for (String docEntry : documentsForTerm) {
                String[] parts = docEntry.split(":");
                String docId = parts[0];
                int frequency = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                documentFrequencies.put(docId,
                        documentFrequencies.getOrDefault(docId, 0) + frequency);
            }
        }

        return documentFrequencies;
    }

    private Map<String, Integer> filterByMetadata(Map<String, Integer> documentFrequencies,
                                                  String author, String language, Integer year) {
        if ((author == null || author.trim().isEmpty()) &&
                (language == null || language.trim().isEmpty()) &&
                year == null) {
            return documentFrequencies;
        }

        Map<String, Integer> filtered = new HashMap<>();

        for (Map.Entry<String, Integer> entry : documentFrequencies.entrySet()) {
            String docId = entry.getKey();
            Integer frequency = entry.getValue();

            BookMetadata metadata = metadataStore.getMetadata(docId);

            if (metadata == null) continue;

            boolean matches = true;

            if (author != null && !author.trim().isEmpty()) {
                matches = metadata.getAuthor() != null &&
                        metadata.getAuthor().toLowerCase().contains(author.toLowerCase());
            }

            if (matches && language != null && !language.trim().isEmpty()) {
                matches = metadata.getLanguage() != null &&
                        metadata.getLanguage().toLowerCase().contains(language.toLowerCase());
            }

            if (matches && year != null) {
                matches = metadata.getYear() == year;
            }

            if (matches) {
                filtered.put(docId, frequency);
            }
        }

        return filtered;
    }

    private List<SearchResult> enrichWithMetadata(Map<String, Integer> documentFrequencies) {
        List<SearchResult> results = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : documentFrequencies.entrySet()) {
            String docId = entry.getKey();
            Integer frequency = entry.getValue();

            BookMetadata metadata = metadataStore.getMetadata(docId);

            SearchResult result = new SearchResult(
                    Integer.parseInt(docId),
                    metadata != null ? metadata.getTitle() : "Unknown",
                    metadata != null ? metadata.getAuthor() : "Unknown",
                    metadata != null ? metadata.getLanguage() : "Unknown",
                    metadata != null ? metadata.getYear() : 0,
                    frequency
            );

            results.add(result);
        }

        if ("frequency".equals(sortingCriteria)) {
            results.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));
        } else if ("id".equals(sortingCriteria)) {
            results.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        }

        return results;
    }

    public static class SearchResult {
        private final int id;
        private final String title;
        private final String author;
        private final String language;
        private final int year;
        private final int frequency;

        public SearchResult(int id, String title, String author, String language, int year, int frequency) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.language = language;
            this.year = year;
            this.frequency = frequency;
        }

        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getLanguage() { return language; }
        public int getYear() { return year; }
        public int getFrequency() { return frequency; }
    }
}
