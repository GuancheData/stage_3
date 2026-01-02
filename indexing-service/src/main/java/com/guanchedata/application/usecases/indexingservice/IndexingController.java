package com.guanchedata.application.usecases.indexingservice;

import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);

    private final IndexingService indexingService;

    public IndexingController(IndexingService indexingService) {
        this.indexingService = indexingService;
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

    public void health(Context ctx) {
        ctx.json(Map.of(
                "status", "healthy",
                "service", "indexing"
        ));
    }
}
