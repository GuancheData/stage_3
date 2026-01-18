package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.model.BookMetadata;
import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

public class BookMetadataSerializer implements CompactSerializer<BookMetadata> {

    @Override
    public BookMetadata read(CompactReader reader) {
        String title = reader.readString("title");
        String author = reader.readString("author");
        String language = reader.readString("language");
        Integer year = reader.readNullableInt32("year");
        return new BookMetadata(title, author, language, year);
    }

    @Override
    public void write(CompactWriter writer, BookMetadata bookMetadata) {
        writer.writeString("title", bookMetadata.getTitle());
        writer.writeString("author", bookMetadata.getAuthor());
        writer.writeString("language", bookMetadata.getLanguage());
        writer.writeNullableInt32("year", bookMetadata.getYear());

    }

    @Override
    public String getTypeName() {
        return "BookMetadata";
    }

    @Override
    public Class<BookMetadata> getCompactClass() {
        return BookMetadata.class;
    }
}
