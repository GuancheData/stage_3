package com.guanchedata.application.usecases.indexingservice;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.adapters.recovery.CoordinateRebuild;
import com.guanchedata.infrastructure.adapters.web.IndexBook;
import com.guanchedata.model.RebuildResult;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class IndexingController {
    private static final Logger log = LoggerFactory.getLogger(IndexingController.class);
    private static final Gson gson = new Gson();

    private final IndexBook indexBook;
    private final CoordinateRebuild rebuildUseCase;

    public IndexingController(IndexBook indexBook, CoordinateRebuild rebuildUseCase) {
        this.indexBook = indexBook;
        this.rebuildUseCase = rebuildUseCase;
    }

    public void indexDocument(Context ctx) {
        String docIdParam = ctx.pathParam("documentId");
        log.info("WEB REQUEST: Index document {}", docIdParam);

        try {
            int documentId = Integer.parseInt(docIdParam);
            indexBook.execute(documentId);
            ctx.status(200).result(gson.toJson(Map.of(
                    "status", "success",
                    "message", "Document indexed successfully",
                    "documentId", documentId
            )));

        } catch (NumberFormatException e) {
            log.warn("Invalid document ID format: {}", docIdParam);
            ctx.status(400).result(gson.toJson(Map.of(
                    "status", "error",
                    "message", "Invalid ID format. Must be an integer."
            )));

        } catch (Exception e) {
            log.error("Error indexing document via Web", e);
            ctx.status(500).result(gson.toJson(Map.of(
                    "status", "error",
                    "message", e.getMessage() != null ? e.getMessage() : "Internal Server Error"
            )));
        }
    }

    public void rebuild(Context ctx) {
        log.info("WEB: Request received to rebuild index");
        try {
            RebuildResult result = rebuildUseCase.execute();

            ctx.status(200).result(gson.toJson(Map.of(
                    "status", "success",
                    "message", result.getMessage()
            )));
        } catch (Exception e) {
            ctx.status(500).result(gson.toJson(Map.of("status", "error", "message", e.getMessage())));
        }
    }

    public void health(Context ctx) {
        ctx.result(gson.toJson(Map.of("status", "healthy")));
    }
}