package com.guanchedata.infrastructure.ports;

import com.guanchedata.model.BookMetadata;
import java.util.Map;
import java.util.Set;

public interface MetadataStore {
    Map<Integer, BookMetadata> getMetadata(Set<Integer> bookIds);
}