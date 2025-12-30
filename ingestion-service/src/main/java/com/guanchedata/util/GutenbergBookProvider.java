package com.guanchedata.util;

import com.guanchedata.infrastructure.adapters.bookprovider.GutenbergBookContentSeparator;
import com.guanchedata.infrastructure.adapters.bookprovider.GutenbergConnection;
import com.guanchedata.infrastructure.adapters.bookprovider.GutenbergFetch;

import java.io.IOException;

public class GutenbergBookProvider {

    private final GutenbergFetch gutenbergFetch;
    private final GutenbergConnection gutenbergConnection;
    private final GutenbergBookContentSeparator gutenbergBookContentSeparator;

    public GutenbergBookProvider(GutenbergFetch gutenbergFetch, GutenbergConnection gutenbergConnection, GutenbergBookContentSeparator gutenbergBookContentSeparator) {
        this.gutenbergFetch = gutenbergFetch;
        this.gutenbergConnection = gutenbergConnection;
        this.gutenbergBookContentSeparator = gutenbergBookContentSeparator;
    }

    public String[] getBook (int bookId) {
        return separateContent(fetchBook(bookId));
    }

    public String fetchBook(int bookId) {
        try {
            GutenbergConnection connection = new GutenbergConnection();
            GutenbergFetch fetch = new GutenbergFetch();
            return fetch.fetchBook(connection.createConnection(bookId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String[] separateContent (String content) {
        return this.gutenbergBookContentSeparator.separateContent(content);
    }
}
