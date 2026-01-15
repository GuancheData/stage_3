package com.guanchedata.model;

import java.io.Serializable;

public class BookReplicationCommand implements Serializable {
    private final int id;

    public BookReplicationCommand(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }
}
