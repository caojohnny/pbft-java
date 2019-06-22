package com.gmail.woodyc40.pbft.message;

public class DefaultReplicaPrePrepare<O> implements ReplicaPrePrepare<O> {
    private final int viewNumber;
    private final long seqNumber;
    private final byte[] digest;
    private final ReplicaRequest<O> request;

    public DefaultReplicaPrePrepare(int viewNumber, long seqNumber, byte[] digest, ReplicaRequest<O> request) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.digest = digest;
        this.request = request;
    }

    @Override
    public int viewNumber() {
        return this.viewNumber;
    }

    @Override
    public long seqNumber() {
        return this.seqNumber;
    }

    @Override
    public byte[] digest() {
        return this.digest;
    }

    @Override
    public ReplicaRequest<O> request() {
        return this.request;
    }
}
