package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DefaultReplica<O, R, T> implements Replica<O, R> {
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
            this.sendRequest(primaryId, request);
            return;
        }

        if (this.log.shouldBuffer()) {
            this.log.buffer(request);
            return;
        }

        PrePrepare<O> message = new PrePrepareImpl<>(
                this.transport.viewNumber(),
                this.seqCounter.getAndIncrement(),
                this.codec.digest(request),
                request);
        this.sendPrePrepare(message);
    }

    @Override
    public void sendRequest(int replicaId, Request<O> request) {
        T encodedPrePrepare = this.codec.encodeRequest(request);
        this.transport.redirectRequest(replicaId, encodedPrePrepare);
    }

    @Override
    public void sendPrePrepare(PrePrepare<O> prePrepare) {
        T encodedPrePrepare = this.codec.encodePrePrepare(prePrepare);
        this.transport.multicastPrePrepare(encodedPrePrepare);
    }

    private boolean verifyPhaseMessage(PhaseMessage message) {
        int currentViewNumber = this.transport.viewNumber();
        int viewNumber = message.viewNumber();
        if (currentViewNumber != viewNumber) {
            return false;
        }

        long seqNumber = message.seqNumber();
        if (!this.log.isBetweenWaterMarks(seqNumber)) {
            return false;
        }

        return true;
    }

    @Override
    public void recvPrePrepare(PrePrepare<O> prePrepare) {
        if (!this.verifyPhaseMessage(prePrepare)) {
            return;
        }

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = prePrepare.seqNumber();

        Ticket<O> ticket = this.log.getTicket(seqNumber);
        byte[] digest = prePrepare.digest();
        if (ticket != null && ticket.viewNumber() == currentViewNumber) {
            for (Object message : ticket.messages()) {
                if (!(message instanceof PrePrepare)) {
                    continue;
                }

                PrePrepare<O> prevPrePrepare = (PrePrepare<O>) message;
                byte[] prevDigest = prevPrePrepare.digest();
                if (!Arrays.equals(prevDigest, digest)) {
                    return;
                }
            }
        }

        byte[] computedDigest = this.codec.digest(prePrepare.request());
        if (!Arrays.equals(digest, computedDigest)) {
            return;
        }

        this.log.add(prePrepare);

        Prepare prepare = new PrepareImpl(
                currentViewNumber,
                seqNumber,
                digest,
                this.replicaId);
        this.sendPrepare(prepare);
    }

    @Override
    public void sendPrepare(Prepare prepare) {
        this.log.add(prepare);

        T encodedPrepare = this.codec.encodePrepare(prepare);
        this.transport.multicastPrePrepare(encodedPrepare);
    }

    @Override
    public void recvPrepare(Prepare prepare) {
        if (!this.verifyPhaseMessage(prepare)) {
            return;
        }

        this.log.add(prepare);

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = prepare.seqNumber();
        if (this.log.isPrepared(prepare)) {
            Commit commit = new CommitImpl(
                    currentViewNumber,
                    seqNumber,
                    prepare.digest(),
                    this.replicaId);
            this.sendCommit(commit);
        }
    }

    @Override
    public void sendCommit(Commit commit) {
        T encodedCommit = this.codec.encodeCommit(commit);
        this.transport.multicastPrePrepare(encodedCommit);
    }

    @Override
    public void recvCommit(Commit commit) {
        if (!this.verifyPhaseMessage(commit)) {
            return;
        }

        this.log.add(commit);

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = commit.seqNumber();

        if (this.log.isCommittedLocal(commit)) {
            Ticket<O> ticket = this.log.getTicket(seqNumber);
            if (ticket == null) {
                throw new IllegalStateException();
            }

            Request<O> request = ticket.request();
            R result = this.compute(request.operation());

            String clientId = request.clientId();
            Reply<R> reply = new ReplyImpl<>(
                    currentViewNumber,
                    request.timestamp(),
                    clientId,
                    this.replicaId,
                    result);
            this.sendReply(clientId, reply);
        }
    }

    @Override
    public void sendReply(String clientId, Reply<R> reply) {
        T encodedReply = this.codec.encodeReply(reply);
        this.transport.sendReply(clientId, encodedReply);
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
