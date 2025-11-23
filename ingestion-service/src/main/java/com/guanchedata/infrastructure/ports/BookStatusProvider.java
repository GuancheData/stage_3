package com.guanchedata.infrastructure.ports;

import java.util.Map;

public interface BookStatusProvider {
    Map<String, Object> status(int bookId);
}
