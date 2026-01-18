package com.guanchedata.infrastructure.adapters.web;
import io.javalin.http.Context;
import com.guanchedata.model.SearchCriteria;

public class SearchRequestMapper {

    public SearchCriteria map(Context ctx) {
        String query = ctx.queryParam("q");

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query parameter 'q' is required");
        }

        String author = ctx.queryParam("author");
        String language = ctx.queryParam("language");
        Integer year = parseYear(ctx.queryParam("year"));

        return new SearchCriteria(query, author, language, year);
    }

    private Integer parseYear(String yearStr) {
        if (yearStr == null || yearStr.isBlank()) return null;
        try {
            return Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid year format");
        }
    }
}