package com.guanchedata.model;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

public class NodeInfoProviderSerializer implements CompactSerializer<NodeInfoProvider> {

    @Override
    public NodeInfoProvider read(CompactReader reader) {
        String nodeId = reader.readString("nodeId");
        return new NodeInfoProvider(nodeId);
    }

    @Override
    public void write(CompactWriter writer, NodeInfoProvider nodeInfoProvider) {
        writer.writeString("nodeId", nodeInfoProvider.getNodeId());
    }

    @Override
    public String getTypeName() {
        return "NodeInfoProvider";
    }

    @Override
    public Class<NodeInfoProvider> getCompactClass() {
        return NodeInfoProvider.class;
    }
}