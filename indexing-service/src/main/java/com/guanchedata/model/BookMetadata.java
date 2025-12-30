package com.guanchedata.model;

public class BookMetadata {

    private final String author;
    private final String language;
    private final int year;

    public BookMetadata(String author, String language, int year) {
        this.author = author;
        this.language = language;
        this.year = year;
    }

    public String getAuthor() {
        return author;
    }

    public String getLanguage() {
        return language;
    }

    public int getYear() {
        return year;
    }
}
