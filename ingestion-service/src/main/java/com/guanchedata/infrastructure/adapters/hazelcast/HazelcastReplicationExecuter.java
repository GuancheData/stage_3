package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.ports.ReplicationExecuter;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.model.BookReplicationCommand;
import com.hazelcast.collection.IQueue;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HazelcastReplicationExecuter implements ReplicationExecuter {

    private final HazelcastInstance hazelcast;
    private final NodeInfoProvider nodeInfoProvider;
    private final int replicationFactor;

    public HazelcastReplicationExecuter(HazelcastInstance hazelcast, NodeInfoProvider nodeInfoProvider, int replicationFactor) {
        this.hazelcast = hazelcast;
        this.nodeInfoProvider = nodeInfoProvider;
        this.replicationFactor = replicationFactor;
    }

    public void execute(int bookId) {
        addLocalNodeToReplicatedMap(bookId);
        replicate(bookId);
    }

    private void addLocalNodeToReplicatedMap(int bookId) {
        // Usamos Set para evitar duplicados si ejecutamos esto dos veces
        IMap<Integer, Set<String>> replicatedNodesMap = hazelcast.getMap("replicatedNodesMap");

        // BLOQUEO: Protegemos la entrada de este libro específico
        replicatedNodesMap.lock(bookId);
        try {
            Set<String> nodes = replicatedNodesMap.getOrDefault(bookId, new HashSet<>());
            nodes.add(nodeInfoProvider.getNodeId());
            replicatedNodesMap.put(bookId, nodes);
        } finally {
            replicatedNodesMap.unlock(bookId);
        }
    }

    @Override
    public void replicate(int bookId) {
        IQueue<BookReplicationCommand> booksToBeReplicated = hazelcast.getQueue("booksToBeReplicated");
        try {
            // Enviamos N-1 tareas. El comando ya NO lleva la lista dentro.
            // La lista vive en el IMap "replicatedNodesMap".
            for (int i = 1; i < replicationFactor; i++) {
                booksToBeReplicated.put(new BookReplicationCommand(bookId));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
