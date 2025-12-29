package com.guanchedata.infrastructure.ports;

public interface MetadataStore {
    public void saveMetadata(int bookId, String header);
}
