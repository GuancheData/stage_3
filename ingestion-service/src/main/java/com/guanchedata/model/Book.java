package com.guanchedata.model;
import java.time.Instant;

public class Book {

    private final int bookId;
    private final String headerFileName;
    private final String bodyFileName;
    private final String sourceNode;
    private final String headerDownloadUrl;
    private final String bodyDownloadUrl;
    private final Instant ts = Instant.now();
    private int replicaCount = 0;

    public Book(int bookId, String headerFileName, String bodyFileName, String sourceNode, String headerDownloadUrl, String bodyDownloadUrl) {
        this.bodyDownloadUrl = bodyDownloadUrl;
        this.headerDownloadUrl = headerDownloadUrl;
        this.sourceNode = sourceNode;
        this.bodyFileName = bodyFileName;
        this.bookId = bookId;
        this.headerFileName = headerFileName;
    }

    public int getBookId() {
        return bookId;
    }

    public String getHeaderFileName() {
        return headerFileName;
    }

    public String getBodyFileName() {
        return bodyFileName;
    }

    public String getSourceNode() {
        return sourceNode;
    }

    public String getHeaderDownloadUrl() {
        return headerDownloadUrl;
    }

    public String getBodyDownloadUrl() {
        return bodyDownloadUrl;
    }

    public Instant getTs() {
        return ts;
    }

    public int getReplicaCount() {
        return replicaCount;
    }

    public void incrementReplicaCount() {
        this.replicaCount++;
    }
}

