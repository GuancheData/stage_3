package com.guanchedata.application.usecases.ingestionservice;

import com.google.gson.Gson;
import com.guanchedata.infrastructure.ports.BookDownloader;
import com.guanchedata.infrastructure.ports.BookListProvider;
import com.guanchedata.infrastructure.ports.BookStatusProvider;
import io.javalin.http.Context;
import java.util.Map;

public class BookProviderController {
    private final BookDownloader ingestBookService;
    private final BookListProvider listBooksService;
    private final BookStatusProvider bookStatusService;
    private static final Gson gson = new Gson();

    public BookProviderController(
            BookDownloader ingestBookService,
            BookListProvider listBooksService,
            BookStatusProvider bookStatusService) {
        this.ingestBookService = ingestBookService;
        this.listBooksService = listBooksService;
        this.bookStatusService = bookStatusService;
    }

    public void ingestBook(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = ingestBookService.ingest(bookId);
        ctx.result(gson.toJson(result));
    }

    public void listAllBooks(Context ctx) {
        Map<String, Object> result = listBooksService.list();
        ctx.result(gson.toJson(result));
    }

    public void status(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = bookStatusService.status(bookId);
        ctx.result(gson.toJson(result));
    }
}
