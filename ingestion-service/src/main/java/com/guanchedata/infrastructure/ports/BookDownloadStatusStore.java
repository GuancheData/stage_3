package com.guanchedata.infrastructure.ports;

import java.io.IOException;
import java.util.List;

public interface BookDownloadStatusStore {
    void registerDownload(int bookId) throws IOException;

    boolean isDownloaded(int bookId) throws IOException;

    List<Integer> getAllDownloaded() throws IOException;
}
