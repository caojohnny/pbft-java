package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DefaultReplica<O, R, T> implements Replica<O, R, T> {
    private final int replicaId;
    private final int tolerance;
    private final ReplicaMessageLog log;
    private final ReplicaEncoder<O, R, T> encoder;
    private final ReplicaDigester<O> digester;
    private final ReplicaTransport<T> transport;

    private final AtomicLong seqCounter = new AtomicLong();

    public DefaultReplica(int replicaId,
                          int tolerance,
                          ReplicaMessageLog log,
                          ReplicaEncoder<O, R, T> encoder,
                          ReplicaDigester<O> digester,
                          ReplicaTransport<T> transport) {
        this.replicaId = replicaId;
        this.tolerance = tolerance;
        this.log = log;
        this.encoder = encoder;
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
        long seqNumber = this.seqCounter.getAndIncrement();

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
        T encodedPrePrepare = this.encoder.encodeRequest(request);
        this.transport.sendMessage(replicaId, encodedPrePrepare);
    }

    @Override
    public void sendPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        T encodedPrePrepare = this.encoder.encodePrePrepare(prePrepare);
        this.transport.multicast(encodedPrePrepare);
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
        T encodedPrepare = this.encoder.encodePrepare(prepare);
        this.transport.multicast(encodedPrepare);
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
        T encodedCommit = this.encoder.encodeCommit(commit);
        this.transport.multicast(encodedCommit);
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
        T encodedReply = this.encoder.encodeReply(reply);
        this.transport.sendReply(clientId, encodedReply);

        this.handleNextBufferedRequest();
    }

    @Override
    public ReplicaEncoder<O, R, T> encoder() {
        return this.encoder;
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
