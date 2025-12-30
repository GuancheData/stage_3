package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.adapters.activemq.ActiveMQBookIngestedNotifier;
import com.guanchedata.model.NodeInfoProvider;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.multimap.MultiMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatalakeRecoveryNotifier {

    private final HazelcastInstance hazelcast;
    private final NodeInfoProvider nodeInfoProvider;
    private final ActiveMQBookIngestedNotifier notifier;

    public DatalakeRecoveryNotifier(HazelcastInstance hazelcast, NodeInfoProvider nodeInfoProvider, ActiveMQBookIngestedNotifier notifier) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.notifier = notifier;
    }

    public void reloadDatalakeFromDisk(String dataVolumePath) throws IOException {
        Path datalakePath = Paths.get(dataVolumePath);

        MultiMap<Integer, NodeInfoProvider> bookLocations = hazelcast.getMultiMap("bookLocations");

        if (!Files.exists(datalakePath) || !Files.isDirectory(datalakePath)) {
            throw new IOException("Datalake path doesn't exist: " + dataVolumePath);
        }

        Files.walk(datalakePath)
                .filter(path -> path.getFileName().toString().endsWith("_body.txt"))
                .forEach(bodyPath -> {
                    int bookId = extractBookId(bodyPath.getFileName().toString());

                    FencedLock lock = hazelcast.getCPSubsystem().getLock("lock:book:" + bookId);
                    lock.lock();
                    try {
                        bookLocations.put(bookId, this.nodeInfoProvider);
                    } finally {
                        lock.unlock();
                    }

                    if (bookLocations.get(bookId).size() == 1) {
                        notifier.notify(bookId);
                    }
                });
    }

    private int extractBookId(String filename) {
        String suffix = "_body.txt";
        int index = filename.indexOf(suffix);
        String idStr = filename.substring(0, index);
        return Integer.parseInt(idStr);
    }
}
