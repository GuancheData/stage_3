package com.guanchedata.infrastructure.ports;

import com.guanchedata.model.SearchCriteria;
import com.guanchedata.model.SearchResult;

import java.util.List;

public interface BookSearch {
    List<SearchResult> execute(SearchCriteria criteria);
}
