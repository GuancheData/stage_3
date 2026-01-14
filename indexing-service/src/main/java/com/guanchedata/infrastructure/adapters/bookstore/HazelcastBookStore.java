package com.guanchedata.infrastructure.adapters.bookstore;

import com.guanchedata.model.BookContent;
import com.guanchedata.infrastructure.ports.BookStore;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HazelcastBookStore implements BookStore {
    private static final Logger log = LoggerFactory.getLogger(HazelcastBookStore.class);
    private final IMap<Integer, BookContent> datalake;

    public HazelcastBookStore(HazelcastInstance hazelcastInstance) {
        this.datalake = hazelcastInstance.getMap("datalake");
    }

    @Override
    public String[] getBookContent(int bookId) {
        try {
            log.info("Searching book {} in Hazelcast. Total books in datalake: {}", bookId, datalake.size());

            BookContent book = this.datalake.get(bookId);
            if (book == null) {
                log.error("Book {} not found in Hazelcast datalake", bookId);
                log.error("Available book IDs around {}: {}", bookId,
                        datalake.keySet().stream()
                                .filter(id -> id >= bookId - 5 && id <= bookId + 5)
                                .sorted()
                                .toList());
                throw new RuntimeException("Book not found in Hazelcast: " + bookId);
            }

            log.info("Retrieved book {} from Hazelcast datalake", bookId);
            return new String[]{book.getHeader(), book.getBody()};
        } catch (NumberFormatException e) {
            log.error("Invalid book ID format: {}", bookId, e);
            throw new RuntimeException("Invalid book ID format: " + bookId, e);
        }
    }
}
