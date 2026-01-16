package com.guanchedata.infrastructure.adapters.recovery;

import com.guanchedata.infrastructure.adapters.apiservices.IndexingService;
import com.guanchedata.infrastructure.ports.RecoveryExecuter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InvertedIndexRecovery implements RecoveryExecuter {

    // low level classes
    // indexing service
    private final String dataVolumePath;
    private final IndexingService indexingService;

    public InvertedIndexRecovery (String dataVolumePath, IndexingService indexingService) {
        this.dataVolumePath = dataVolumePath;
        this.indexingService = indexingService;
    }

    @Override
    public int executeRecovery() {
        Path datalakePath = Paths.get(this.dataVolumePath);
        int maxBookId = 0;

        try (var paths = Files.walk(datalakePath)) {

            for (Path bodyPath : paths.filter(p -> p.getFileName().toString().endsWith("_body.txt")).toList()) {

                int bookId = extractBookId(bodyPath.getFileName().toString());
                maxBookId = Math.max(maxBookId, bookId);

                Path headerPath = bodyPath.getParent().resolve(bookId + "_header.txt");
                String header = Files.readString(headerPath);
                String body = Files.readString(bodyPath);

                indexingService.indexLocalDocument(bookId, header, body); }
        } catch (IOException e) {
            System.out.println("No local data found in disk");
        }

        return maxBookId;
    }


    private int extractBookId(String filename) {
        String suffix = "_body.txt";
        int index = filename.indexOf(suffix);
        String idStr = filename.substring(0, index);
        return Integer.parseInt(idStr);
    }
}


