package com.guanchedata.model;

import java.io.Serializable;

public class ReplicatedBook implements Serializable {
    private final String header;
    private final String body;
    private final String sourceNode;

    public ReplicatedBook(String header, String body, String sourceNode) {
        this.header = header;
        this.body = body;
        this.sourceNode = sourceNode;
    }

    public String getHeader() {
        return this.header;
    }

    public String getBody() {
        return this.body;
    }

    public String getSourceNode() {
        return this.sourceNode;
    }
}
