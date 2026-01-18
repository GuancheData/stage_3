package com.guanchedata.infrastructure.ports;

import com.guanchedata.model.SearchResult;
import java.util.List;

public interface SortingStrategy {
    void sort(List<SearchResult> results);
}
