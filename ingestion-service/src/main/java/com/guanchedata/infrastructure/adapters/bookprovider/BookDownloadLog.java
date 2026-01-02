package com.guanchedata.infrastructure.adapters.bookprovider;

import com.guanchedata.infrastructure.ports.BookDownloadStatusStore;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.collection.ISet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BookDownloadLog implements BookDownloadStatusStore {
    private final ISet<Integer> downloadedBooks;

    public BookDownloadLog(HazelcastInstance hazelcastInstance, String setName) {
        this.downloadedBooks = hazelcastInstance.getSet(setName);
    }

    @Override
    public void registerDownload(int bookId) throws IOException {
        downloadedBooks.add(bookId);
    }

    @Override
    public boolean isDownloaded(int bookId) throws IOException {
        return downloadedBooks.contains(bookId);
    }

    @Override
    public List<Integer> getAllDownloaded() throws IOException {
        return new ArrayList<>(downloadedBooks);
    }
}
