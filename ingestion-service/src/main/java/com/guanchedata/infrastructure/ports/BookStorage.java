package com.guanchedata.infrastructure.ports;

import java.io.IOException;
import java.nio.file.Path;

public interface BookStorage {
    Path save(int book_id, String content) throws IOException;
}
