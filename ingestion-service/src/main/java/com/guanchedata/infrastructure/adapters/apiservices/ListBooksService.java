package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.ports.BookDownloadStatusStore;
import com.guanchedata.infrastructure.ports.BookListProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class ListBooksService implements BookListProvider {
    private static final Logger log = LoggerFactory.getLogger(ListBooksService.class);
    private final BookDownloadStatusStore bookDownloadLog;

    public ListBooksService(BookDownloadStatusStore bookDownloadLog) {
        this.bookDownloadLog = bookDownloadLog;
    }

    @Override
    public Map<String, Object> list() {
        log.info("list() - Listing books in the datalake");
        try {
            List<Integer> downloadedBooks = bookDownloadLog.getAllDownloaded();
            return successResponse(downloadedBooks);
        } catch (Exception e) {
            log.error("list() - Error listing books: {}", e.getMessage(), e);
            return errorResponse(e.getMessage());
        } finally {
            log.info("list() - Finished execution");
        }
    }

    private Map<String, Object> successResponse(List<Integer> downloadedBooks){
        return Map.of(
                "count", downloadedBooks.size(),
                "books", downloadedBooks
        );
    }

    private Map<String, Object> errorResponse(String errorMessage){
        return Map.of(
                "status", "error",
                "message", errorMessage
        );
    }
}
