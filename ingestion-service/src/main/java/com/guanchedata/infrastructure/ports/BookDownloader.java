package com.guanchedata.infrastructure.ports;

import java.util.Map;

public interface BookDownloader {
    Map<String, Object> ingest(int bookId);
}
