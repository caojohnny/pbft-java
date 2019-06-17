package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

import java.util.Arrays;

public abstract class DefaultReplica<O, R, T> implements Replica<O, R> {
    private final int replicaId;
    private final int tolerance;
    private final ReplicaMessageLog log;
    private final ReplicaCodec<T> codec;
    private final ReplicaDigester<O> digester;
    private final ReplicaTransport<T> transport;

    private long seqCounter;

    public DefaultReplica(int replicaId,
                          int tolerance,
                          ReplicaMessageLog log,
                          ReplicaCodec<T> codec,
                          ReplicaDigester<O> digester,
                          ReplicaTransport<T> transport) {
        this.replicaId = replicaId;
        this.tolerance = tolerance;
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
    public int tolerance() {
        return this.tolerance;
    }

    @Override
    public ReplicaMessageLog log() {
        return this.log;
    }

    private void recvRequest(ReplicaRequest<O> request, boolean buffered) {
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
        long seqNumber = this.seqCounter++;

        ReplicaTicket<O> ticket = this.log.newTicket(currentViewNumber, seqNumber, request);
        ticket.append(request);

        ReplicaPrePrepare<O> message = new DefaultReplicaPrePrepare<>(
                currentViewNumber,
                seqNumber,
                this.digester.digest(request),
                request);
        this.sendPrePrepare(message);
    }

    @Override
    public void recvRequest(ReplicaRequest<O> request) {
        this.recvRequest(request, false);
    }

    @Override
    public void sendRequest(int replicaId, ReplicaRequest<O> request) {
        T encodedPrePrepare = this.codec.encodeRequest(request);
        this.transport.redirectRequest(replicaId, encodedPrePrepare);
    }

    @Override
    public void sendPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        T encodedPrePrepare = this.codec.encodePrePrepare(prePrepare);
        this.transport.multicastPrePrepare(encodedPrePrepare);
    }

    private boolean verifyPhaseMessage(ReplicaPhaseMessage message) {
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
    public void recvPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        if (!this.verifyPhaseMessage(prePrepare)) {
            return;
        }

        int currentViewNumber = this.transport.viewNumber();
        byte[] digest = prePrepare.digest();
        ReplicaRequest<O> request = prePrepare.request();
        long seqNumber = prePrepare.seqNumber();

        ReplicaTicket<O> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket != null) {
            for (Object message : ticket.messages()) {
                if (!(message instanceof ReplicaPrePrepare)) {
                    continue;
                }

                ReplicaPrePrepare<O> prevPrePrepare = (ReplicaPrePrepare<O>) message;
                byte[] prevDigest = prevPrePrepare.digest();
                if (!Arrays.equals(prevDigest, digest)) {
                    return;
                }
            }
        } else {
            ticket = this.log.newTicket(currentViewNumber, seqNumber, request);
        }

        byte[] computedDigest = this.digester.digest(request);
        if (!Arrays.equals(digest, computedDigest)) {
            return;
        }

        ticket.append(prePrepare);

        ReplicaPrepare prepare = new DefaultReplicaPrepare(
                currentViewNumber,
                seqNumber,
                digest,
                this.replicaId);
        this.sendPrepare(prepare);

        ticket.append(prepare);
    }

    @Override
    public void sendPrepare(ReplicaPrepare prepare) {
        T encodedPrepare = this.codec.encodePrepare(prepare);
        this.transport.multicastPrePrepare(encodedPrepare);
    }

    @Override
    public void recvPrepare(ReplicaPrepare prepare) {
        if (!this.verifyPhaseMessage(prepare)) {
            return;
        }

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = prepare.seqNumber();

        ReplicaTicket<O> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket == null) {
            throw new IllegalStateException("Received PREPARE in the incorrect order");
        }

        ticket.append(prepare);
        if (ticket.isPrepared(this.tolerance)) {
            ReplicaCommit commit = new DefaultReplicaCommit(
                    currentViewNumber,
                    seqNumber,
                    prepare.digest(),
                    this.replicaId);
            this.sendCommit(commit);

            ticket.append(commit);
        }
    }

    @Override
    public void sendCommit(ReplicaCommit commit) {
        T encodedCommit = this.codec.encodeCommit(commit);
        this.transport.multicastPrePrepare(encodedCommit);
    }

    @Override
    public void recvCommit(ReplicaCommit commit) {
        if (!this.verifyPhaseMessage(commit)) {
            return;
        }

        int currentViewNumber = this.transport.viewNumber();
        long seqNumber = commit.seqNumber();

        ReplicaTicket<O> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket == null) {
            throw new IllegalStateException("Received COMMIT in the incorrect order");
        }

        ticket.append(commit);
        if (ticket.isCommittedLocal(this.tolerance)) {
            ReplicaRequest<O> request = ticket.request();
            R result = this.compute(request.operation());

            String clientId = request.clientId();
            ReplicaReply<R> reply = new DefaultReplicaReply<>(
                    currentViewNumber,
                    request.timestamp(),
                    clientId,
                    this.replicaId,
                    result);

            this.log.deleteTicket(currentViewNumber, seqNumber);
            this.sendReply(clientId, reply);
        }
    }

    private void handleNextBufferedRequest() {
        ReplicaRequest<O> bufferedRequest = this.log.popBuffer();
        if (bufferedRequest != null) {
            this.recvRequest(bufferedRequest, true);
        }
    }

    @Override
    public void sendReply(String clientId, ReplicaReply<R> reply) {
        T encodedReply = this.codec.encodeReply(reply);
        this.transport.sendReply(clientId, encodedReply);

        this.handleNextBufferedRequest();
    }

    @Override
    public ReplicaCodec<T> codec() {
        return this.codec;
    }

    @Override
    public ReplicaDigester<O> digester() {
        return this.digester;
    }

    @Override
    public ReplicaTransport<T> transport() {
        return this.transport;
    }

    private int getPrimaryId() {
        return this.transport.viewNumber() % this.transport.countKnownReplicas();
    }
}
