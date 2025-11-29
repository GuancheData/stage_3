package com.guanchedata.infrastructure.adapters.bookprovider;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;

import java.io.IOException;

public class GutenbergFetch {
    public String fetchBook(Connection connection) throws IOException {
        Connection.Response response = connection.execute();
        if (response.statusCode() != 200){
            throw new HttpStatusException("Error HTTP.", response.statusCode(), response.url().toString());
        }
        return response.body();
    }
}
