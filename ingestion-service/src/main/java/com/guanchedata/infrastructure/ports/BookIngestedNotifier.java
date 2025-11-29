package com.guanchedata.infrastructure.ports;

public interface BookIngestedNotifier {
    void notify(int bookId);
}
