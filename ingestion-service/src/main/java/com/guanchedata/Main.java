package com.guanchedata;

import com.guanchedata.application.usecases.ingestionservice.BookIngestionPeriodicExecutor;
import com.guanchedata.application.usecases.ingestionservice.BookProviderController;
import com.guanchedata.infrastructure.adapters.activemq.ActiveMQBookIngestedNotifier;
import com.guanchedata.infrastructure.adapters.apiservices.BookStatusService;
import com.guanchedata.infrastructure.adapters.apiservices.IngestBookService;
import com.guanchedata.infrastructure.adapters.apiservices.ListBooksService;
import com.guanchedata.infrastructure.adapters.bookprovider.*;
import com.guanchedata.infrastructure.adapters.hazelcast.HazelcastManager;
import com.guanchedata.infrastructure.ports.*;
import com.guanchedata.util.DateTimePathGenerator;
import com.guanchedata.util.GutenbergBookProvider;
import io.javalin.Javalin;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        PathGenerator pathGenerator = new DateTimePathGenerator(args[0]);

        BookStorageDate storageDate = new BookStorageDate(pathGenerator);
        GutenbergBookProvider gutenbergBookProvider = new GutenbergBookProvider(new GutenbergFetch(), new GutenbergConnection(), new GutenbergBookContentSeparator());

        ActiveMQBookIngestedNotifier notifier =  new ActiveMQBookIngestedNotifier(System.getenv("BROKER_URL"));
        HazelcastManager hazelcastManager = new HazelcastManager("SearchEngine", Integer.parseInt(System.getenv("REPLICATION_FACTOR")), gutenbergBookProvider, storageDate);
        BookDownloadStatusStore bookDownloadLog = new BookDownloadLog(hazelcastManager.getHazelcastInstance(), "log");
        BookDownloader ingestBookService = new IngestBookService(storageDate, bookDownloadLog, notifier, hazelcastManager, gutenbergBookProvider);


        BookListProvider listBooksService = new ListBooksService(bookDownloadLog);
        BookStatusProvider bookStatusService = new BookStatusService(bookDownloadLog);

        BookProviderController controller = new BookProviderController(
                ingestBookService,
                listBooksService,
                bookStatusService
        );

        BookIngestionPeriodicExecutor bookIngestionExecutor = new BookIngestionPeriodicExecutor(hazelcastManager.getHazelcastInstance(),ingestBookService);

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
        }).start(7001);

        // bookIngestionExecutor.setupBookQueue();
        bookIngestionExecutor.startPeriodicExecution();

        app.post("/ingest/{book_id}", controller::ingestBook);
        app.get("/ingest/status/{book_id}", controller::status);
        app.get("/ingest/list", controller::listAllBooks);
    }
}

