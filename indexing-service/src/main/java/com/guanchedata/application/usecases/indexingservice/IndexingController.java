package com.guanchedata.application.usecases.indexingservice;

import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import com.guanchedata.infrastructure.adapters.recovery.RebuildCoordinator;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class IndexingController {

    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);
    private final IndexingService indexingService;
    private final RebuildCoordinator rebuildCoordinator;

    public IndexingController(IndexingService indexingService,
                              RebuildCoordinator rebuildCoordinator) {
        this.indexingService = indexingService;
        this.rebuildCoordinator = rebuildCoordinator;
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

    public void rebuild(Context ctx) {
        log.info("RECEIVED REBUILD REQUEST - Broadcasting to all nodes");
        try {
            rebuildCoordinator.initiateRebuild();
            ctx.status(200).json(Map.of(
                    "status", "success",
                    "message", "Rebuild completed across all nodes"
            ));
        } catch (IllegalStateException e) {
            ctx.status(409).json(Map.of(
                    "status", "conflict",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error during rebuild: {}", e.getMessage());
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
