package com.guanchedata.infrastructure.adapters.web;

import com.guanchedata.infrastructure.ports.BookSearch;
import com.guanchedata.model.SearchCriteria;
import com.guanchedata.model.SearchResult;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SearchController {
    private static final Logger log = Logger.getLogger(SearchController.class.getName());

    private final BookSearch bookSearch;
    private final SearchRequestMapper mapper;
    private final SearchResponsePresenter presenter;

    public SearchController(BookSearch bookSearch) {
        this.bookSearch = bookSearch;
        this.mapper = new SearchRequestMapper();
        this.presenter = new SearchResponsePresenter();
    }

    public void search(Context ctx) {
        try {
            SearchCriteria criteria = mapper.map(ctx);
            log.info("Executing search for: " + criteria.getQuery());

            List<SearchResult> results = bookSearch.execute(criteria);

            Map<String, Object> jsonResponse = presenter.formatSuccess(criteria, results);

            ctx.json(jsonResponse);
        } catch (IllegalArgumentException e) {
            ctx.status(400).json(presenter.formatError(e.getMessage()));
        } catch (Exception e) {
            log.severe("Error: " + e.getMessage());
            ctx.status(500).json(presenter.formatError(e.getMessage()));
        }
    }

    public void health(Context ctx) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "healthy");
        response.put("service", "execute");
        ctx.json(response);
    }
}
