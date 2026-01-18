package com.guanchedata.application.usecases.searchservice;

import com.guanchedata.infrastructure.ports.IndexStore;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ContentSearchEngine {
    private final IndexStore indexStore;
    private final ExecutorService executor;

    public ContentSearchEngine(IndexStore indexStore, ExecutorService executor) {
        this.indexStore = indexStore;
        this.executor = executor;
    }

    public Map<String, Integer> findDocumentFrequencies(String query) {
        if (query == null || query.isBlank()) return Collections.emptyMap();

        String[] terms = tokenize(query);
        return processTerms(terms);
    }

    private String[] tokenize(String query) {
        return query.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private Map<String, Integer> processTerms(String[] terms) {
        Map<String, Integer> frequencySum = new ConcurrentHashMap<>();
        Map<String, Integer> termCount = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger validTermsCount = new AtomicInteger(0);

        for (String term : terms) {
            validTermsCount.incrementAndGet();
            futures.add(CompletableFuture.runAsync(() ->
                    processSingleTerm(term, frequencySum, termCount), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return filterByIntersection(frequencySum, termCount, validTermsCount.get());
    }

    private void processSingleTerm(String term, Map<String, Integer> frequencySum, Map<String, Integer> termCount) {
        Set<String> documents = indexStore.getDocuments(term);
        if (documents == null || documents.isEmpty()) return;

        for (String docEntry : documents) {
            String[] parts = docEntry.split(":");
            String docId = parts[0];
            int frequency = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

            frequencySum.merge(docId, frequency, Integer::sum);
            termCount.merge(docId, 1, Integer::sum);
        }
    }

    private Map<String, Integer> filterByIntersection(Map<String, Integer> frequencySum, Map<String, Integer> termCount, int requiredMatches) {
        if (requiredMatches == 0) return Collections.emptyMap();

        frequencySum.keySet().removeIf(docId ->
                termCount.getOrDefault(docId, 0) < requiredMatches
        );
        return frequencySum;
    }
}
