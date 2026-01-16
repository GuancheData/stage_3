package com.guanchedata.model;


public class RebuildCommand {
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
