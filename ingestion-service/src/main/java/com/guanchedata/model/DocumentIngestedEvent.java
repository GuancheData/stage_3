package com.guanchedata.model;

import java.time.Instant;

public class DocumentIngestedEvent {
    private final int bookId;
    private final String event = "document.ingested";
    private final String  ts = Instant.now().toString();

    public DocumentIngestedEvent(int bookId) {
        this.bookId = bookId;
    }

    public int getBookId() { return bookId; }
    public String getEvent() { return event; }
    public String getTs() { return ts; }
}
