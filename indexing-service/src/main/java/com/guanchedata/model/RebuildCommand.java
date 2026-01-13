package com.guanchedata.model;

import java.io.Serializable;

public class RebuildCommand implements Serializable {
    private long epoch;
    private int expectedNodes;
    private String latchName;

    public RebuildCommand() {}

    public RebuildCommand(long epoch, int expectedNodes, String latchName) {
        this.epoch = epoch;
        this.expectedNodes = expectedNodes;
        this.latchName = latchName;
    }

    public long getEpoch() { 
        return epoch; 
    }
    
    public void setEpoch(long epoch) { 
        this.epoch = epoch; 
    }
    
    public int getExpectedNodes() { 
        return expectedNodes; 
    }
    
    public void setExpectedNodes(int expectedNodes) { 
        this.expectedNodes = expectedNodes; 
    }
    
    public String getLatchName() { 
        return latchName; 
    }
    
    public void setLatchName(String latchName) { 
        this.latchName = latchName; 
    }
}
