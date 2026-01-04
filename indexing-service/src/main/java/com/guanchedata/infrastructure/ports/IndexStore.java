package com.guanchedata.infrastructure.ports;

import java.util.Collection;
import java.util.Set;

public interface IndexStore {
    void addEntry(String term, String documentId);

    void pushEntries();

    Set<String> getDocuments(String term);
    void clear();
    Collection<Integer> retrieveIndexingRegistry();
    void saveTokens(Integer tokenCount);
}
