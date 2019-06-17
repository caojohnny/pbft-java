package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Client;

public class DefaultRequest<O> implements Request<O> {
    private final O operation;
    private final long timestamp;
    private final Client<?, ?> client;

    public DefaultRequest(O operation, long timestamp, Client<?, ?> client) {
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
    public <R> Client<O, R> client() {
        return (Client<O, R>) this.client;
    }
}
