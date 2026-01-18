package com.guanchedata.infrastructure.adapters.web;

import com.guanchedata.model.SearchCriteria;
import com.guanchedata.model.SearchResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchResponsePresenter {

    public Map<String, Object> formatSuccess(SearchCriteria criteria, List<SearchResult> results) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("query", criteria.getQuery());
        response.put("filters", extractFilters(criteria));
        response.put("count", results.size());

        List<Map<String, Object>> mappedResults = results.stream()
                .map(this::mapSingleResult)
                .collect(Collectors.toList());

        response.put("results", mappedResults);
        return response;
    }

    public Map<String, Object> formatError(String message) {
        return Map.of("status", "error", "message", message);
    }

    private Map<String, Object> extractFilters(SearchCriteria c) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (c.getAuthor() != null) filters.put("author", c.getAuthor());
        if (c.getLanguage() != null) filters.put("language", c.getLanguage());
        if (c.getYear() != null) filters.put("year", c.getYear());
        return filters;
    }

    private Map<String, Object> mapSingleResult(SearchResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("title", r.getTitle());
        m.put("author", r.getAuthor());
        m.put("language", r.getLanguage());
        m.put("year", r.getYear());
        m.put("frequency", r.getFrequency());
        return m;
    }
}