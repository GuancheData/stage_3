package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.adapters.bookprovider.GutenbergConnection;
import com.guanchedata.infrastructure.adapters.bookprovider.GutenbergFetch;
import com.guanchedata.infrastructure.ports.BookDownloadStatusStore;
import com.guanchedata.infrastructure.ports.BookDownloader;
import com.guanchedata.infrastructure.ports.BookStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class IngestBookService implements BookDownloader {
    private static final Logger log = LoggerFactory.getLogger(IngestBookService.class);
    private final BookDownloadStatusStore bookDownloadLog;
    private final BookStorage storageDate;

    public IngestBookService(BookStorage storageDate, BookDownloadStatusStore bookDownloadLog) {
        this.storageDate = storageDate;
        this.bookDownloadLog = bookDownloadLog;
    }

    @Override
    public Map<String, Object> ingest(int bookId) {
        log.info("ingest() - Start processing bookId={}", bookId);

        try {
            if (bookDownloadLog.isDownloaded(bookId)) {
                return alreadyDownloadedResponse(bookId);
            }
            String response = fetchBook(bookId);
            Path savedPath = storageDate.save(bookId, response);
            bookDownloadLog.registerDownload(bookId);

            return successResponse(bookId, savedPath);
        } catch (Exception e) {
            return errorResponse(bookId, e);
        } finally {
            log.info("ingest() - Finished processing bookId={}", bookId);
        }
    }

    private String fetchBook(int bookId) throws Exception {
        GutenbergConnection connection = new GutenbergConnection();
        GutenbergFetch fetch = new GutenbergFetch();
        return fetch.fetchBook(connection.createConnection(bookId));
    }


    private Map<String, Object> alreadyDownloadedResponse(int bookId) {
        log.warn("ingest() - Book {} is already downloaded, skipping ingestion", bookId);
        return Map.of(
                "book_id", bookId,
                "status", "already_downloaded",
                "message", "Book already exists in datalake"
        );
    }

    private Map<String, Object> successResponse(int bookId, Path savedPath) {
        log.info("ingest() - Book {} downloaded and saved at {}", bookId, savedPath);
        return Map.of(
                "book_id", bookId,
                "status", "downloaded",
                "path", savedPath.toString().replace("\\", "/")
        );
    }

    private Map<String, Object> errorResponse(int bookId, Exception e) {
        log.error("ingest() - Error processing bookId {}: {}", bookId, e.getMessage(), e);
        return Map.of(
                "book_id", bookId,
                "status", "error",
                "message", e.getMessage()
        );
    }
}
