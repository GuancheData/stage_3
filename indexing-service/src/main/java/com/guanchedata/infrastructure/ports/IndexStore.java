package com.guanchedata.infrastructure.ports;

import java.util.Collection;
import java.util.Set;

public interface IndexStore {
    void addEntry(String term, String documentId);
    Set<String> getDocuments(String term);
    void clear();
    Collection<Integer> retrieveIndexingRegistry();
}
