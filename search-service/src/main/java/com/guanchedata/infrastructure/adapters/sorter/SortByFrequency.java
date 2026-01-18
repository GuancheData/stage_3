package com.guanchedata.infrastructure.adapters.sorter;

import com.guanchedata.infrastructure.ports.SortingStrategy;
import com.guanchedata.model.SearchResult;

import java.util.List;

public class SortByFrequency implements SortingStrategy {
    @Override
    public void sort(List<SearchResult> results){
        results.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));
    }
}
