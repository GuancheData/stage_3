package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.ports.IndexStore;
import com.guanchedata.infrastructure.ports.MetadataStore;
import com.guanchedata.model.BookMetadata;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SearchService {
    private static final Logger log = Logger.getLogger(SearchService.class.getName());

    private final IndexStore indexStore;
    private final MetadataStore metadataStore;
    private final String sortingCriteria;
    private final ExecutorService searchExecutor;

    public SearchService(IndexStore indexStore, MetadataStore metadataStore, String sortingCriteria) {
        this.indexStore = indexStore;
        this.metadataStore = metadataStore;
        this.sortingCriteria = sortingCriteria;
        int cores = Runtime.getRuntime().availableProcessors();
        int maxThreads = cores - 2;
        this.searchExecutor = Executors.newFixedThreadPool(maxThreads);
    }

    public List<SearchResult> search(String query, String author, String language, Integer year) {
        long startTime = System.currentTimeMillis();
        Map<String, Integer> contentResults = searchByContent(query);

        if (contentResults.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> docIds = contentResults.keySet().stream()
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        Map<Integer, BookMetadata> bulkMetadata = metadataStore.getMetadataBulk(docIds);

        List<SearchResult> finalResults = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : contentResults.entrySet()) {
            int docId = Integer.parseInt(entry.getKey());
            BookMetadata meta = bulkMetadata.get(docId);

            if (meta == null) continue;
            if (!matchesFilter(meta, author, language, year)) continue;

            finalResults.add(new SearchResult(
                    docId,
                    meta.getTitle(),
                    meta.getAuthor(),
                    meta.getLanguage(),
                    meta.getYear() != null ? meta.getYear() : 0,
                    entry.getValue()
            ));
        }

        sortResults(finalResults);

        long duration = System.currentTimeMillis() - startTime;
        log.info(String.format("Search: results=%d, time=%dms\n", finalResults.size(), duration));

        return finalResults;
    }

    private Map<String, Integer> searchByContent(String query) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyMap();

        String[] terms = query.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim()
                .split("\\s+");

        Map<String, Integer> frequencySum = new ConcurrentHashMap<>();
        Map<String, Integer> termCount = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger validTermsCount = new AtomicInteger(0);

        for (String term : terms) {
            if (term.length() <= 2) continue;

            validTermsCount.incrementAndGet();

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Set<String> documentsForTerm = indexStore.getDocuments(term);
                if (documentsForTerm != null && !documentsForTerm.isEmpty()) {
                    for (String docEntry : documentsForTerm) {
                        String[] parts = docEntry.split(":");
                        String docId = parts[0];
                        int frequency = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                        frequencySum.merge(docId, frequency, Integer::sum);
                        termCount.merge(docId, 1, Integer::sum);
                    }
                }
            }, searchExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        int requiredMatches = validTermsCount.get();
        if (requiredMatches == 0) return Collections.emptyMap();
        frequencySum.keySet().removeIf(docId ->
                termCount.getOrDefault(docId, 0) < requiredMatches
        );
        return frequencySum;
    }

    private boolean matchesFilter(BookMetadata meta, String author, String language, Integer year) {
        if (author != null && !author.isBlank() &&
                (meta.getAuthor() == null || !meta.getAuthor().toLowerCase().contains(author.toLowerCase()))) {
            return false;
        }
        if (language != null && !language.isBlank() &&
                (meta.getLanguage() == null || !meta.getLanguage().toLowerCase().contains(language.toLowerCase()))) {
            return false;
        }
        if (year != null && (meta.getYear() == null || !meta.getYear().equals(year))) {
            return false;
        }
        return true;
    }

    private void sortResults(List<SearchResult> results) {
        if ("frequency".equals(sortingCriteria)) {
            results.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));
        } else if ("id".equals(sortingCriteria)) {
            results.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        }
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