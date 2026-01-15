package com.guanchedata.model;

public class BookContent {
    private String header;
    private String body;

    public BookContent(String header, String body) {
        this.header = header;
        this.body = body;
    }

    public String getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }
}
