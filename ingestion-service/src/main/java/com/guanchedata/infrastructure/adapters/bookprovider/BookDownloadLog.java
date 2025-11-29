package com.guanchedata.infrastructure.adapters.bookprovider;

import com.guanchedata.infrastructure.ports.BookDownloadStatusStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class BookDownloadLog implements BookDownloadStatusStore {
    private final String downloadedBookFile;

    public BookDownloadLog(String downloadedBookFile) {
        this.downloadedBookFile = downloadedBookFile;
    }

    @Override
    public void registerDownload(int bookId) throws IOException {
        Set<Integer> books = loadBooks();
        books.add(bookId);
        saveBooks(bookId);
    }

    @Override
    public boolean isDownloaded(int bookId) throws IOException {
        return loadBooks().contains(bookId);
    }

    @Override
    public List<Integer> getAllDownloaded() throws IOException {
        return new ArrayList<>(loadBooks());
    }

    private Set<Integer> loadBooks() throws IOException {
        Path path = Paths.get(downloadedBookFile);
        if (!Files.exists(path)) {
            return new HashSet<>();
        }
        List<String> lines = Files.readAllLines(path);
        Set<Integer> books = new HashSet<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                books.add(Integer.parseInt(line.trim()));
            }
        }
        return books;
    }


    private void saveBooks(int bookId) throws IOException {
        Path parent = Paths.get(downloadedBookFile).getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        String line = bookId + System.lineSeparator();

        Files.writeString(Paths.get(downloadedBookFile), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
