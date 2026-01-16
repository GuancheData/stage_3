package com.guanchedata.model;

public class BookMetadata {

    private String title;
    private String author;
    private String language;
    private Integer year;

    public BookMetadata(String title, String author, String language, Integer year) {
        this.title = title;
        this.author = author;
        this.language = language;
        this.year = year;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getLanguage() { return language; }
    public Integer getYear() { return year; }
}
