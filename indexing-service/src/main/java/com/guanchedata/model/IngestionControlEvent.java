package com.guanchedata.model;

import java.time.Instant;

public class IngestionControlEvent {
    private final String event = "ingestion.control";
    private final String ts = Instant.now().toString();
    private String type;

    public IngestionControlEvent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getTs() {
        return ts;
    }
}
