package com.guanchedata.infrastructure.adapters.metadata;

import com.guanchedata.model.BookMetadata;

public class MetadataParser {

    public BookMetadata parseFromHeader(String header) {

        String title = null;
        String author = null;
        String language = null;
        String year = null;

        String[] lines = header.split("\\R");

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("Title:")) {
                title = line.substring("Title:".length()).trim();
            } else if (line.startsWith("Author:")) {
                author = line.substring("Author:".length()).trim();
            } else if (line.startsWith("Language:")) {
                language = line.substring("Language:".length()).trim();
            } else if (line.startsWith("Release date:")) {
                int yearIndex = line.lastIndexOf(", ");
                if (yearIndex != -1 && yearIndex + 1 < line.length()) {
                    year = line.substring(yearIndex + 1, yearIndex + 6).trim();
                }
            }
        }

        return new BookMetadata(title, author, language, Integer.parseInt(year));
    }
}
