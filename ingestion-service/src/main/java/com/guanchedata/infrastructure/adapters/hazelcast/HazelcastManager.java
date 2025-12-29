package com.guanchedata.infrastructure.adapters.hazelcast;

import com.guanchedata.infrastructure.adapters.bookprovider.BookStorageDate;
import com.guanchedata.model.NodeInfoProvider;
import com.guanchedata.util.GutenbergBookProvider;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.multimap.MultiMap;

public class HazelcastManager {

    HazelcastInstance hazelcastInstance;
    NodeInfoProvider nodeInfoProvider;
    HazelcastDatalakeListener hazelcastDatalakeListener;
    HazelcastReplicationExecuter hazelcastReplicationExecuter;

    public HazelcastManager(String clusterName, int replicationFactor, GutenbergBookProvider gutenbergBookDownloader, BookStorageDate bookStorageDate) {
        this.nodeInfoProvider = new NodeInfoProvider(System.getenv("PUBLIC_IP")); // NODE IDENTIFIER
        this.hazelcastInstance = new HazelcastConfig().initHazelcast(clusterName); // CREATE HAZELCAST MEMBER
        this.hazelcastDatalakeListener = new HazelcastDatalakeListener(this.hazelcastInstance, this.nodeInfoProvider, gutenbergBookDownloader, bookStorageDate);
        this.hazelcastDatalakeListener.registerListener();
        this.hazelcastReplicationExecuter = new HazelcastReplicationExecuter(this.hazelcastInstance, this.nodeInfoProvider,replicationFactor);
    }

    public void uploadToMemory(int bookId, String[] contentSeparated) {
        String header = contentSeparated[0];
        String body = contentSeparated[1];
        MultiMap<Integer,String> datalake = this.hazelcastInstance.getMultiMap("datalake");
        datalake.put(bookId, header);
        datalake.put(bookId, body);
    }

    public HazelcastInstance getHazelcastInstance() {
        return this.hazelcastInstance;
    }

    public NodeInfoProvider getNodeInfoProvider() {
        return this.nodeInfoProvider;
    }

    public HazelcastReplicationExecuter getHazelcastReplicationExecuter() {
        return hazelcastReplicationExecuter;
    }
}
