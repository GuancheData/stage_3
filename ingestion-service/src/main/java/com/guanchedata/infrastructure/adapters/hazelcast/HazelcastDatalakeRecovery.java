package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.ReplicatedBook;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HazelcastDatalakeRecovery {

    private final HazelcastInstance hazelcast;
    private final NodeInfoProvider nodeInfoProvider;
    private final int replicationFactor;

    public HazelcastDatalakeRecovery(HazelcastInstance hazelcast, NodeInfoProvider nodeInfoProvider, int replicationFactor) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.replicationFactor = replicationFactor;
    }

    public void reloadMemoryFromDisk(String dataVolumePath) throws IOException {
        Path datalakePath = Paths.get(dataVolumePath);

        IMap<Integer, Integer> replicaCount = hazelcast.getMap("replication-count");
        MultiMap<Integer, ReplicatedBook> datalake = hazelcast.getMultiMap("datalake");

        if (!Files.exists(datalakePath) || !Files.isDirectory(datalakePath)) {
            throw new IOException("Datalake path doesn't exist: " + dataVolumePath);
        }

        Files.walk(datalakePath)
                .filter(path -> path.getFileName().toString().endsWith("_body.txt"))
                .forEach(bodyPath -> {
                    try {
                        int bookId = extractBookId(bodyPath.getFileName().toString());
                        int currentReplicas = replicaCount.getOrDefault(bookId, 0);

                        if (currentReplicas < replicationFactor) {
                            Path headerPath = bodyPath.getParent().resolve(bookId + "_header.txt");
                            String header = Files.readString(headerPath);
                            String body = Files.readString(bodyPath);

                            FencedLock lock = hazelcast.getCPSubsystem().getLock("lock:book:" + bookId);
                            lock.lock();
                            try {
                                datalake.put(bookId, new ReplicatedBook(header, body, nodeInfoProvider.getNodeId()));
                                replicaCount.put(bookId, currentReplicas + 1);
                            } finally {
                                lock.unlock();
                            }
                        }
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
