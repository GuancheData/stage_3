package com.guanchedata.infrastructure.adapters.apiservices;

import com.guanchedata.infrastructure.adapters.activemq.ActiveMQBookIngestedNotifier;
import com.guanchedata.infrastructure.adapters.hazelcast.HazelcastManager;
import com.guanchedata.infrastructure.ports.BookDownloadStatusStore;
import com.guanchedata.infrastructure.ports.BookDownloader;
import com.guanchedata.infrastructure.ports.BookStorage;
import com.guanchedata.util.GutenbergBookProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;

public class IngestBookService implements BookDownloader {
    private static final Logger log = LoggerFactory.getLogger(IngestBookService.class);
    private final BookDownloadStatusStore bookDownloadLog;
    private final BookStorage storageDate;
    private final ActiveMQBookIngestedNotifier bookIngestedNotifier;
    private final HazelcastManager hazelcastManager;
    private final GutenbergBookProvider gutenbergBookProvider;

    public IngestBookService(BookStorage storageDate, BookDownloadStatusStore bookDownloadLog, ActiveMQBookIngestedNotifier bookIngestedNotifier, HazelcastManager hazelcastManager, GutenbergBookProvider gutenbergBookDownloader) {
        this.storageDate = storageDate;
        this.bookDownloadLog = bookDownloadLog;
        this.bookIngestedNotifier = bookIngestedNotifier;
        this.hazelcastManager = hazelcastManager;
        this.gutenbergBookProvider = gutenbergBookDownloader;
    }

    @Override
    public Map<String, Object> ingest(int bookId) {
        log.info("\ningest() - Start processing bookId={}", bookId);
        try {
            if (bookDownloadLog.isDownloaded(bookId)) {
                return alreadyDownloadedResponse(bookId);
            }
            String[] separatedContent = this.gutenbergBookProvider.getBook(bookId);
            // get separated content from book
            Path savedPath = storageDate.save(bookId, separatedContent); // save to disk
            this.hazelcastManager.uploadToMemory(bookId, separatedContent);
            this.hazelcastManager.getHazelcastReplicationExecuter().execute(bookId); // start background replication
            bookDownloadLog.registerDownload(bookId);
            this.bookIngestedNotifier.notify(bookId);
            return successResponse(bookId, savedPath);
        } catch (Exception e) {
            return errorResponse(bookId, e);
        } finally {
            log.info("ingest() - Finished processing bookId={}", bookId);
        }
    }

//    public String fetchBook(int bookId) throws Exception {
//        GutenbergConnection connection = new GutenbergConnection();
//        GutenbergFetch fetch = new GutenbergFetch();
//        return fetch.fetchBook(connection.createConnection(bookId));
//    }


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
