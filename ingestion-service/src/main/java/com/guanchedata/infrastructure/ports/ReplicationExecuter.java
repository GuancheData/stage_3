package com.guanchedata.infrastructure.ports;

public interface ReplicationExecuter {
    public void replicate(int bookId, String header, String body);
}
