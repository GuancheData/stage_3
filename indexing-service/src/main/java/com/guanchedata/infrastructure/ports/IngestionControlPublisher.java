package com.guanchedata.infrastructure.ports;

public interface IngestionControlPublisher {
    void publishPause();
    void publishResume();
}
