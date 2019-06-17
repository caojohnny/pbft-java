package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Client;

public class DefaultClientReply<R> implements ClientReply<R> {
    private final int viewNumber;
    private final long timestamp;
    private final Client<?, ?, ?> client;
    private final int replicaId;
    private final R result;

    public DefaultClientReply(int viewNumber, long timestamp, Client<?, ?, ?> client, int replicaId, R result) {
        this.viewNumber = viewNumber;
        this.timestamp = timestamp;
        this.client = client;
        this.replicaId = replicaId;
        this.result = result;
    }

    @Override
    public int viewNumber() {
        return this.viewNumber;
    }

    @Override
    public long timestamp() {
        return this.timestamp;
    }

    @Override
    public <O, T> Client<O, R, T> client() {
        return (Client<O, R, T>) this.client;
    }

    @Override
    public int replicaId() {
        return this.replicaId;
    }

    @Override
    public R result() {
        return this.result;
    }
}
