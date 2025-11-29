package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.ports.BookDownloadStatusStore;
import com.guanchedata.infrastructure.ports.BookStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class BookStatusService implements BookStatusProvider {
    private static final Logger log = LoggerFactory.getLogger(BookStatusService.class);
    private final BookDownloadStatusStore bookDownloadLog;

    public BookStatusService(BookDownloadStatusStore bookDownloadLog) {
        this.bookDownloadLog = bookDownloadLog;
    }

    @Override
    public Map<String, Object> status(int bookId) {
        log.info("status() - Start execution for bookId={}", bookId);
        try {
            boolean isBookAvailable = bookDownloadLog.isDownloaded(bookId);
            if (isBookAvailable) {
                return successResponse(bookId);
            } else {
                log.warn("status() - Book {} is not available in datalake", bookId);
                return notAvailableResponse(bookId);
            }
        } catch (Exception e) {
            log.error("status() - Error retrieving status for bookId {}: {}", bookId, e.getMessage(), e);
            return errorResponse(bookId,e.getMessage());
        } finally {
            log.info("status() - Finished execution for bookId={}", bookId);
        }
    }

    private Map<String, Object> successResponse(int bookId){
        log.info("status() - Book {} is available in datalake", bookId);
        return Map.of(
                "book_id", bookId,
                "status", "available"
        );
    }

    private Map<String, Object> notAvailableResponse(int bookId){
        return Map.of(
                "book_id", bookId,
                "status", "not_available"
        );
    }

    private Map<String, Object> errorResponse(int bookId, String errorMessage){
        return Map.of(
                "book_id", bookId,
                "status", "error",
                "message", errorMessage
        );
    }
}
