package com.gmail.woodyc40.pbft.message;

public class DefaultPrePrepare<O> implements PrePrepare<O> {
    private final int viewNumber;
    private final long seqNumber;
    private final byte[] digest;
    private final Request<O> request;

    public DefaultPrePrepare(int viewNumber, long seqNumber, byte[] digest, Request<O> request) {
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
    public Request<O> request() {
        return this.request;
    }
}
