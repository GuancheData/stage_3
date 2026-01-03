package com.guanchedata.infrastructure.ports;

import java.util.Set;

public interface IndexStore {
    Set<String> getDocuments(String term);
}
