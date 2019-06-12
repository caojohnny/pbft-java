package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DefaultReplica<O, R, T> implements Replica<O, R> {
    private static final byte[] EMPTY_DIGEST = new byte[0];

    private final int replicaId;
    private final MessageLog log;
    private final Codec<T> codec;
    private final Transport<T> transport;

    private final AtomicLong seqCounter = new AtomicLong();

    public DefaultReplica(int replicaId, MessageLog log, Codec<T> codec, Transport<T> transport) {
        this.replicaId = replicaId;
        this.log = log;
        this.codec = codec;
        this.transport = transport;
    }

    @Override
    public int replicaId() {
        return this.replicaId;
    }

    @Override
    public MessageLog log() {
        return this.log;
    }

    @Override
    public void recvRequest(Request<O> request) {
        int primaryId = this.getPrimaryId();

        // We are not the primary replica, redirect
        if (this.replicaId != primaryId) {
            T encodedRequest = this.codec.encodeRequest(request);
            this.transport.redirectRequest(primaryId, encodedRequest);
            return;
        }

        // TODO: Check with the MessageLog, make sure there aren't >X concurrent requests

        PrePrepare<O> message = new PrePrepareImpl<>(
                this.transport.viewNumber(),
                this.seqCounter.getAndIncrement(),
                EMPTY_DIGEST,
                request);
        T encodedPrePrepare = this.codec.encodePrePrepare(message);
        this.transport.multicastPrePrepare(encodedPrePrepare);
    }

    @Override
    public void sendPrePrepare(int replicaId, PrePrepare<O> prePrepare) {

    }

    @Override
    public void recvPrePrepare(PrePrepare<O> prePrepare) {

    }

    @Override
    public void sendPrepare(Prepare prepare) {

    }

    @Override
    public void recvPrepare(Prepare prepare) {

    }

    @Override
    public void sendCommit(Commit commit) {

    }

    @Override
    public void recvCommit(Commit commit) {

    }

    @Override
    public void sendReply(String clientId, Reply<R> reply) {

    }

    @Override
    public Codec<T> codec() {
        return this.codec;
    }

    @Override
    public Transport<T> transport() {
        return this.transport;
    }

    private int getPrimaryId() {
        return this.transport.viewNumber() % this.transport.countKnownReplicas();
    }
}
