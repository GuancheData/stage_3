package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.adapters.activemq.ActiveMQBookIngestedNotifier;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.ReplicatedBook;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.multimap.MultiMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HazelcastDatalakeRecovery {

    private final HazelcastInstance hazelcast;
    private final NodeInfoProvider nodeInfoProvider;
    private final ActiveMQBookIngestedNotifier notifier;

    public HazelcastDatalakeRecovery(HazelcastInstance hazelcast, NodeInfoProvider nodeInfoProvider, ActiveMQBookIngestedNotifier notifier) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.notifier = notifier;
    }

    public void reloadMemoryFromDisk(String dataVolumePath) throws IOException {
        Path datalakePath = Paths.get(dataVolumePath);
        MultiMap<Integer, ReplicatedBook> datalake = hazelcast.getMultiMap("datalake");

        if (!Files.exists(datalakePath) || !Files.isDirectory(datalakePath)) {
            throw new IOException("Datalake path doesn't exist: " + dataVolumePath);
        }

        Files.walk(datalakePath)
                .filter(path -> path.getFileName().toString().endsWith("_body.txt"))
                .forEach(bodyPath -> {
                    try {
                        int bookId = extractBookId(bodyPath.getFileName().toString());

                        FencedLock lock = hazelcast.getCPSubsystem().getLock("lock:book:" + bookId);
                        lock.lock();
                        try {
                            if (datalake.containsKey(bookId)) {
                                System.out.println("Book " + bookId + " already in the in-memory datalake. Skip.");
                                return;
                            }

                            Path headerPath = bodyPath.getParent().resolve(bookId + "_header.txt");
                            String header = Files.readString(headerPath);
                            String body = Files.readString(bodyPath);
                            datalake.put(bookId, new ReplicatedBook(header, body, nodeInfoProvider.getNodeId()));

                        } finally {
                            lock.unlock();
                        }

                        notifier.notify(bookId);

                    } catch (IOException e) {
                        throw new RuntimeException("Error reading from disk: " + bodyPath, e);
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
