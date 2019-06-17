package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DefaultReplica<O, R, T> implements Replica<O, R> {
    private final int replicaId;
    private final MessageLog log;
    private final Codec<T> codec;
    private final Digester<O> digester;
    private final Transport<T> transport;

    private final AtomicLong seqCounter = new AtomicLong();

    public DefaultReplica(int replicaId,
                          MessageLog log,
                          Codec<T> codec,
                          Digester<O> digester,
                          Transport<T> transport) {
        this.replicaId = replicaId;
        this.log = log;
        this.codec = codec;
        this.digester = digester;
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

    private void recvRequest(Request<O> request, boolean buffered) {
        int primaryId = this.getPrimaryId();

        // We are not the primary replica, redirect
        if (this.replicaId != primaryId) {
            this.sendRequest(primaryId, request);
            return;
        }

        if (!buffered) {
            if (this.log.shouldBuffer()) {
                this.log.buffer(request);
                return;
            }
        }

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = this.seqCounter.getAndIncrement();

        Ticket<O> ticket = this.log.newTicket(currentViewNumber, seqNumber);
        ticket.append(request);

        PrePrepare<O> message = new PrePrepareImpl<>(
                currentViewNumber,
                seqNumber,
                this.digester.digest(request),
                request);
        this.sendPrePrepare(message);
    }

    @Override
    public void recvRequest(Request<O> request) {
        this.recvRequest(request, false);
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

        Ticket<O> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        byte[] digest = prePrepare.digest();
        if (ticket != null) {
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
        } else {
            ticket = this.log.newTicket(currentViewNumber, seqNumber);
        }

        byte[] computedDigest = this.digester.digest(prePrepare.request());
        if (!Arrays.equals(digest, computedDigest)) {
            return;
        }

        ticket.append(prePrepare);

        Prepare prepare = new PrepareImpl(
                currentViewNumber,
                seqNumber,
                digest,
                this.replicaId);
        this.sendPrepare(prepare);

        ticket.append(prepare);
    }

    @Override
    public void sendPrepare(Prepare prepare) {
        T encodedPrepare = this.codec.encodePrepare(prepare);
        this.transport.multicastPrePrepare(encodedPrepare);
    }

    @Override
    public void recvPrepare(Prepare prepare) {
        if (!this.verifyPhaseMessage(prepare)) {
            return;
        }

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = prepare.seqNumber();

        Ticket<O> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket == null) {
            throw new IllegalStateException("Received PREPARE in the incorrect order");
        }

        ticket.append(prepare);
        if (ticket.isPrepared()) {
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

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = commit.seqNumber();

        Ticket<O> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket == null) {
            throw new IllegalStateException("Received COMMIT in the incorrect order");
        }

        ticket.append(commit);
        if (ticket.isCommittedLocal()) {
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

    private void handleNextBufferedRequest() {
        Request<O> bufferedRequest = this.log.popBuffer();
        if (bufferedRequest != null) {
            this.recvRequest(bufferedRequest, true);
        }
    }

    @Override
    public void sendReply(String clientId, Reply<R> reply) {
        T encodedReply = this.codec.encodeReply(reply);
        this.transport.sendReply(clientId, encodedReply);

        this.handleNextBufferedRequest();
    }

    @Override
    public Codec<T> codec() {
        return this.codec;
    }

    @Override
    public Digester<O> digester() {
        return this.digester;
    }

    @Override
    public Transport<T> transport() {
        return this.transport;
    }

    private int getPrimaryId() {
        return this.transport.viewNumber() % this.transport.countKnownReplicas();
    }
}
