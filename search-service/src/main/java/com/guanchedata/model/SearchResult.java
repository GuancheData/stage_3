package com.guanchedata.model;

public class SearchResult {
    private final int id;
    private final String title;
    private final String author;
    private final String language;
    private final int year;
    private final int frequency;

    public SearchResult(int id, String title, String author, String language, int year, int frequency) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.language = language;
        this.year = year;
        this.frequency = frequency;
    }
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getLanguage() { return language; }
    public int getYear() { return year; }
    public int getFrequency() { return frequency; }
}