package com.gmail.woodyc40.pbft.message;

public class ReplyImpl<R> implements Reply<R> {
    private final int viewNumber;
    private final long timestamp;
    private final String clientId;
    private final int replicaId;
    private final R result;

    public ReplyImpl(int viewNumber, long timestamp, String clientId, int replicaId, R result) {
        this.viewNumber = viewNumber;
        this.timestamp = timestamp;
        this.clientId = clientId;
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
    public String clientId() {
        return this.clientId;
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
