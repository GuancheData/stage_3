package com.guanchedata.application.usecases.indexingservice;

import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import com.guanchedata.infrastructure.adapters.apiservices.SearchService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);

    private final IndexingService indexingService;
    private final SearchService searchService;

    public IndexingController(IndexingService indexingService, SearchService searchService) {
        this.indexingService = indexingService;
        this.searchService = searchService;
    }

    public void indexDocument(Context ctx) {
        int documentId = Integer.parseInt(ctx.pathParam("documentId"));
        log.info("Received index request for document: {}", documentId);

        try {
            indexingService.indexDocument(documentId);
            ctx.status(200).json(Map.of(
                "status", "success",
                "message", "Document indexed successfully",
                "documentId", documentId
            ));
        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage());
            ctx.status(500).json(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    public void search(Context ctx) {
        String query = ctx.queryParam("q");
        if (query == null || query.trim().isEmpty()) {
            ctx.status(400).json(Map.of("error", "Query parameter 'q' is required"));
            return;
        }

        log.info("Received search query: {}", query);

        try {
            Set<String> results = searchService.search(query);
            ctx.json(Map.of(
                "query", query,
                "results", results,
                "count", results.size()
            ));
        } catch (Exception e) {
            log.error("Error executing search: {}", e.getMessage());
            ctx.status(500).json(Map.of("error", e.getMessage()));
        }
    }
}
