package com.guanchedata.model;

import java.io.Serializable;

public class BookMetadata implements Serializable {

    private String title;
    private String author;
    private String language;
    private int year;

    public BookMetadata() {}

    public BookMetadata(String title, String author, String language, int year) {
        this.title = title;
        this.author = author;
        this.language = language;
        this.year = year;
    }

    public String getTitle() { return title; }

    public String getAuthor() { return author; }

    public String getLanguage() { return language; }

    public int getYear() { return year; }
}
