package com.guanchedata.model;

public class RebuildResult {
    private final boolean success;
    private final String message;

    public RebuildResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}