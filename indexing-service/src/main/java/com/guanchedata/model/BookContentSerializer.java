package com.guanchedata.model;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

public class BookContentSerializer implements CompactSerializer<BookContent> {

    @Override
    public BookContent read(CompactReader reader) {
        String header = reader.readString("header");
        String body = reader.readString("body");
        return new BookContent(header, body);
    }

    @Override
    public void write(CompactWriter writer, BookContent bookContent) {
        writer.writeString("header", bookContent.getHeader());
        writer.writeString("body", bookContent.getBody());
    }

    @Override
    public String getTypeName() {
        return "BookContent";
    }

    @Override
    public Class<BookContent> getCompactClass() {
        return BookContent.class;
    }
}
