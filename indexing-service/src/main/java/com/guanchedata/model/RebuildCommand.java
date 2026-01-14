package com.guanchedata.model;

import java.io.Serializable;

public class RebuildCommand implements Serializable {
    private long epoch;

    public RebuildCommand(long epoch) {
        this.epoch = epoch;
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long epoch) {
        this.epoch = epoch;
    }
}
