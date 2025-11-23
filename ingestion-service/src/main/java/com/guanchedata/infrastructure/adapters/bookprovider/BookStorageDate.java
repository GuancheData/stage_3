package com.guanchedata.infrastructure.adapters.bookprovider;

import com.guanchedata.infrastructure.ports.BookStorage;
import com.guanchedata.infrastructure.ports.PathGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BookStorageDate implements BookStorage {
    private final PathGenerator pathGenerator;
    private final GutenbergBookContentSeparator contentSeparator;

    public BookStorageDate(PathGenerator pathGenerator, GutenbergBookContentSeparator contentSeparator){
        this.pathGenerator = pathGenerator;
        this.contentSeparator = contentSeparator;
    }

    @Override
    public Path save(int bookId, String content) throws IOException {
        String[] contentSeparated = contentSeparator.separateContent(content);
        String header = contentSeparated[0];
        String body = contentSeparated[1];

        Path path = pathGenerator.generatePath();

        Path headerPath = path.resolve(String.format("%d_header.txt", bookId));
        Path contentPath = path.resolve(String.format("%d_body.txt", bookId));

        Files.writeString(headerPath, header);
        Files.writeString(contentPath, body);
        return path;
    }
}
