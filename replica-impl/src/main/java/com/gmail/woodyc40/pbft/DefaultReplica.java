package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DefaultReplica<O, R, T> implements Replica<O, R, T> {
    private static final byte[] EMPTY_DIGEST = new byte[0];

    private final int replicaId;
    private final int tolerance;
    private final long timeout;
    private final ReplicaMessageLog log;
    private final ReplicaEncoder<O, R, T> encoder;
    private final ReplicaDigester<O> digester;
    private final ReplicaTransport<T> transport;

    private volatile int viewNumber;
    private volatile boolean disgruntled;
    private final AtomicLong seqCounter = new AtomicLong();
    private final Map<ReplicaRequestKey, ExpBackoff> timeouts = new ConcurrentHashMap<>();

    public DefaultReplica(int replicaId,
                          int tolerance,
                          long timeout,
                          ReplicaMessageLog log,
                          ReplicaEncoder<O, R, T> encoder,
                          ReplicaDigester<O> digester,
                          ReplicaTransport<T> transport) {
        this.replicaId = replicaId;
        this.tolerance = tolerance;
        this.timeout = timeout;
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
    public long timeoutMs() {
        return this.timeout;
    }

    @Override
    public ReplicaMessageLog log() {
        return this.log;
    }

    @Override
    public void setViewNumber(int newViewNumber) {
        this.viewNumber = newViewNumber;
    }

    @Override
    public int viewNumber() {
        return this.viewNumber;
    }

    @Override
    public void setDisgruntled(boolean disgruntled) {
        this.disgruntled = disgruntled;
    }

    @Override
    public boolean isDisgruntled() {
        return this.disgruntled;
    }

    @Override
    public Collection<ReplicaRequestKey> activeTimers() {
        return Collections.unmodifiableCollection(this.timeouts.keySet());
    }

    @Override
    public long checkTimeout(ReplicaRequestKey key) {
        int currentViewNumber = this.viewNumber;

        ExpBackoff backoff = this.timeouts.get(key);
        if (backoff == null) {
            return 0L;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - backoff.startTime();
        if (elapsed >= backoff.timeout()) {
            int viewDelta = backoff.viewDelta();
            ReplicaViewChange viewChange = this.log.produceViewChange(
                    currentViewNumber + viewDelta,
                    this.replicaId,
                    this.tolerance);
            this.sendViewChange(viewChange);

            this.disgruntled = true;
            backoff.advanceTimeout();
        }

        return backoff.timeout() - elapsed;
    }

    private void resendReply(String clientId, ReplicaTicket<O, R> ticket) {
        ticket.result().thenAccept(result -> {
            int viewNumber = ticket.viewNumber();
            ReplicaRequest<O> request = ticket.request();
            long timestamp = request.timestamp();
            ReplicaReply<R> reply = new DefaultReplicaReply<>(
                    viewNumber,
                    timestamp,
                    clientId,
                    this.replicaId,
                    result);
            this.sendReply(clientId, reply);
        });
    }

    private void recvRequest(ReplicaRequest<O> request, boolean wasRequestBuffered) {
        String clientId = request.clientId();
        long timestamp = request.timestamp();
        ReplicaTicket<O, R> cachedTicket = this.log.getTicketFromCache(clientId, timestamp);
        if (cachedTicket != null) {
            this.resendReply(clientId, cachedTicket);
            return;
        }

        ReplicaRequestKey key = new DefaultReplicaRequestKey(clientId, timestamp);
        this.timeouts.computeIfAbsent(key, k -> new ExpBackoff(this.timeout));

        int primaryId = this.getPrimaryId();

        // We are not the primary replica, redirect
        if (this.replicaId != primaryId) {
            this.sendRequest(primaryId, request);
            return;
        }

        if (!wasRequestBuffered) {
            if (this.log.shouldBuffer()) {
                this.log.buffer(request);
                return;
            }
        }

        int currentViewNumber = this.viewNumber;
        long seqNumber = this.seqCounter.getAndIncrement();

        ReplicaTicket<O, R> ticket = this.log.newTicket(currentViewNumber, seqNumber);
        ticket.append(request);

        ReplicaPrePrepare<O> prePrepare = new DefaultReplicaPrePrepare<>(
                currentViewNumber,
                seqNumber,
                this.digester.digest(request),
                request);
        this.sendPrePrepare(prePrepare);

        ticket.append(prePrepare);
    }

    @Override
    public void recvRequest(ReplicaRequest<O> request) {
        if (this.disgruntled) {
            return;
        }

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
        this.transport.multicast(encodedPrePrepare, this.replicaId);
    }

    private boolean verifyPhaseMessage(ReplicaPhaseMessage message) {
        if (this.disgruntled) {
            return false;
        }

        int currentViewNumber = this.viewNumber;
        int viewNumber = message.viewNumber();
        if (currentViewNumber != viewNumber) {
            return false;
        }

        long seqNumber = message.seqNumber();
        return this.log.isBetweenWaterMarks(seqNumber);
    }

    @Override
    public void recvPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        if (!this.verifyPhaseMessage(prePrepare)) {
            return;
        }

        int currentViewNumber = this.viewNumber;
        byte[] digest = prePrepare.digest();
        ReplicaRequest<O> request = prePrepare.request();
        long seqNumber = prePrepare.seqNumber();

        byte[] computedDigest = this.digester.digest(request);
        if (!Arrays.equals(digest, computedDigest)) {
            return;
        }

        ReplicaTicket<O, R> ticket = this.log.getTicket(currentViewNumber, seqNumber);
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
            ticket = this.log.newTicket(currentViewNumber, seqNumber);
        }

        ticket.append(prePrepare);

        ReplicaPrepare prepare = new DefaultReplicaPrepare(
                currentViewNumber,
                seqNumber,
                digest,
                this.replicaId);
        this.sendPrepare(prepare);

        ticket.append(prepare);

        this.tryAdvanceState(ticket, prePrepare);
    }

    @Override
    public void sendPrepare(ReplicaPrepare prepare) {
        T encodedPrepare = this.encoder.encodePrepare(prepare);
        this.transport.multicast(encodedPrepare, this.replicaId);
    }

    @Override
    public void recvPrepare(ReplicaPrepare prepare) {
        ReplicaTicket<O, R> ticket = this.recvPhaseMessage(prepare);

        if (ticket != null) {
            this.tryAdvanceState(ticket, prepare);
        }
    }

    @Override
    public void sendCommit(ReplicaCommit commit) {
        T encodedCommit = this.encoder.encodeCommit(commit);
        this.transport.multicast(encodedCommit, this.replicaId);
    }

    @Override
    public void recvCommit(ReplicaCommit commit) {
        ReplicaTicket<O, R> ticket = this.recvPhaseMessage(commit);

        if (ticket != null) {
            this.tryAdvanceState(ticket, commit);
        }
    }

    private @Nullable ReplicaTicket<O, R> recvPhaseMessage(ReplicaPhaseMessage message) {
        if (!this.verifyPhaseMessage(message)) {
            return null;
        }

        int currentViewNumber = message.viewNumber();
        long seqNumber = message.seqNumber();

        ReplicaTicket<O, R> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket == null) {
            ticket = this.log.newTicket(currentViewNumber, seqNumber);
        }

        ticket.append(message);
        return ticket;
    }

    private void tryAdvanceState(ReplicaTicket<O, R> ticket, ReplicaPhaseMessage message) {
        int currentViewNumber = message.viewNumber();
        long seqNumber = message.seqNumber();
        byte[] digest = message.digest();

        ReplicaTicketPhase phase = ticket.phase();
        if (phase == ReplicaTicketPhase.PRE_PREPARE) {
            if (ticket.isPrepared(this.tolerance) && ticket.casPhase(phase, ReplicaTicketPhase.PREPARE)) {
                ReplicaCommit commit = new DefaultReplicaCommit(
                        currentViewNumber,
                        seqNumber,
                        digest,
                        this.replicaId);
                this.sendCommit(commit);

                ticket.append(commit);
            }
        }

        phase = ticket.phase();
        if (phase == ReplicaTicketPhase.PREPARE) {
            if (ticket.isCommittedLocal(this.tolerance) && ticket.casPhase(phase, ReplicaTicketPhase.COMMIT)) {
                ReplicaRequest<O> request = ticket.request();
                if (request != null) {
                    O operation = request.operation();
                    R result = this.compute(operation);

                    String clientId = request.clientId();
                    long timestamp = request.timestamp();
                    ReplicaReply<R> reply = new DefaultReplicaReply<>(
                            currentViewNumber,
                            timestamp,
                            clientId,
                            this.replicaId,
                            result);

                    this.log.completeTicket(currentViewNumber, seqNumber);
                    this.sendReply(clientId, reply);

                    ReplicaRequestKey key = new DefaultReplicaRequestKey(clientId, timestamp);
                    this.timeouts.remove(key);
                }

                if (seqNumber % this.log.checkpointInterval() == 0) {
                    ReplicaCheckpoint checkpoint = new DefaultReplicaCheckpoint(
                            seqNumber,
                            this.digestState(),
                            this.replicaId);
                    this.sendCheckpoint(checkpoint);
                    this.log.appendCheckpoint(checkpoint, this.tolerance);
                }
            }
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
    public void recvCheckpoint(ReplicaCheckpoint checkpoint) {
        this.log.appendCheckpoint(checkpoint, this.tolerance);
    }

    @Override
    public void sendCheckpoint(ReplicaCheckpoint checkpoint) {
        T encodedCheckpoint = this.encoder.encodeCheckpoint(checkpoint);
        this.transport.multicast(encodedCheckpoint, this.replicaId);
    }

    private void enterNewView(int newViewNumber) {
        this.disgruntled = false;
        this.viewNumber = newViewNumber;
        this.timeouts.clear();
    }

    @Override
    public void recvViewChange(ReplicaViewChange viewChange) {
        int newViewNumber = viewChange.newViewNumber();
        int newPrimaryId = getPrimaryId(newViewNumber, this.transport.countKnownReplicas());

        // We're not the new primary
        if (newPrimaryId != this.replicaId) {
            return;
        }

        boolean shouldBandwagon = this.log.acceptViewChange(viewChange, this.tolerance);
        if (shouldBandwagon) {
            ReplicaViewChange bandwagonViewChange = this.log.produceViewChange(newViewNumber, this.replicaId, this.tolerance);
            this.sendViewChange(bandwagonViewChange);
        }

        ReplicaNewView newView = this.log.produceNewView(newViewNumber, this.tolerance, this.replicaId);
        if (newView != null) {
            this.sendNewView(newView);
            this.enterNewView(newViewNumber);
        }
    }

    @Override
    public void sendViewChange(ReplicaViewChange viewChange) {
        T encodedViewChange = this.encoder.encodeViewChange(viewChange);
        this.transport.multicast(encodedViewChange, this.replicaId);
    }

    @Override
    public void recvNewView(ReplicaNewView newView) {
        this.log.acceptNewView(newView);

        int newViewNumber = newView.newViewNumber();
        for (ReplicaPrePrepare<?> prePrepare : newView.preparedProofs()) {
            ReplicaRequest<O> request = (ReplicaRequest<O>) prePrepare.request();
            O operation = request.operation();

            // No-op
            if (operation == null) {
                continue;
            }

            byte[] digest = prePrepare.digest();
            if (!Arrays.equals(digest, this.digester.digest(request))) {
                continue;
            }

            long seqNumber = prePrepare.seqNumber();
            ReplicaTicket<O, R> ticket = this.log.newTicket(newViewNumber, seqNumber);
            ticket.append(prePrepare);

            ReplicaPrepare prepare = new DefaultReplicaPrepare(
                    newViewNumber,
                    seqNumber,
                    digest,
                    this.replicaId);
            this.sendPrepare(prepare);
        }

        this.enterNewView(newViewNumber);
    }

    @Override
    public void sendNewView(ReplicaNewView newView) {
        T encodedNewView = this.encoder.encodeNewView(newView);
        this.transport.multicast(encodedNewView, this.replicaId);
    }

    @Override
    public byte[] digestState() {
        return EMPTY_DIGEST;
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

    private static int getPrimaryId(int viewNumber, int knownReplicas) {
        return viewNumber % knownReplicas;
    }

    private int getPrimaryId() {
        return getPrimaryId(this.viewNumber, this.transport.countKnownReplicas());
    }

    private static class ExpBackoff {
        private int viewDelta = 1;
        private long timeout;
        private long startTime;

        public ExpBackoff(long timeout) {
            this.timeout = timeout;
            this.startTime = System.currentTimeMillis();
        }

        private void advanceTimeout() {
            this.viewDelta++;
            this.timeout <<= 1;
            this.startTime = System.currentTimeMillis();
        }

        public int viewDelta() {
            return this.viewDelta;
        }

        public long timeout() {
            return this.timeout;
        }

        public long startTime() {
            return this.startTime;
        }
    }
}
