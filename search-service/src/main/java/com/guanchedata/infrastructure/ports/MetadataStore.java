package com.guanchedata.infrastructure.ports;

import com.guanchedata.model.BookMetadata;

public interface MetadataStore {
    BookMetadata getMetadata(String bookId);
}
