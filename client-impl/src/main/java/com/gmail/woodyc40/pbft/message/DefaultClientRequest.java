package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Client;

public class DefaultClientRequest<O> implements ClientRequest<O> {
    private final O operation;
    private final long timestamp;
    private final Client<?, ?, ?> client;

    public DefaultClientRequest(O operation, long timestamp, Client<?, ?, ?> client) {
        this.operation = operation;
        this.timestamp = timestamp;
        this.client = client;
    }

    @Override
    public O operation() {
        return this.operation;
    }

    @Override
    public long timestamp() {
        return this.timestamp;
    }

    @Override
    public <R, T> Client<O, R, T> client() {
        return (Client<O, R, T>) this.client;
    }
}
