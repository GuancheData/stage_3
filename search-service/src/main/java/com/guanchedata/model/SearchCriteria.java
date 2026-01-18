package com.guanchedata.model;

public class SearchCriteria {
    private final String query;
    private final String author;
    private final String language;
    private final Integer year;

    public SearchCriteria(String query, String author, String language, Integer year) {
        this.query = query;
        this.author = author;
        this.language = language;
        this.year = year;
    }

    public String getQuery() { return query; }
    public String getAuthor() { return author; }
    public String getLanguage() { return language; }
    public Integer getYear() { return year; }
}
