package com.guanchedata.infrastructure.ports;

public interface BookStore {
    String[] getBookContent(int bookId);
}