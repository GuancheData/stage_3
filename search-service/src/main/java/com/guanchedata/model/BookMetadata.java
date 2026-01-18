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

    public boolean matches(String authorFilter, String languageFilter, Integer yearFilter) {
        if (authorFilter != null && !containsIgnoreCase(this.author, authorFilter)) return false;
        if (languageFilter != null && !containsIgnoreCase(this.language, languageFilter)) return false;
        if (yearFilter != null && !yearFilter.equals(this.year)) return false;
        return true;
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source != null && target != null &&
                source.toLowerCase().contains(target.toLowerCase());
    }
}
