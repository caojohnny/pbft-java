package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DefaultReplicaMessageLog implements ReplicaMessageLog {
    private static final byte[] NULL_DIGEST = new byte[0];
    public static final DefaultReplicaRequest<Object> NULL_REQ = new DefaultReplicaRequest<>(null, 0, "");

    private final int bufferThreshold;
    private final int checkpointInterval;
    private final int watermarkInterval;

    private final Deque<ReplicaRequest<?>> buffer = new ConcurrentLinkedDeque<>();

    private final Map<Long, ReplicaTicket<?, ?>> ticketCache = new ConcurrentHashMap<>();
    private final Map<TicketKey, ReplicaTicket<?, ?>> tickets = new ConcurrentHashMap<>();

    private final Map<Long, Collection<ReplicaCheckpoint>> checkpoints = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, ReplicaViewChange>> viewChanges = new ConcurrentHashMap<>();

    private volatile long lowWaterMark;
    private volatile long highWaterMark;

    public DefaultReplicaMessageLog(int bufferThreshold, int checkpointInterval, int watermarkInterval) {
        this.bufferThreshold = bufferThreshold;
        this.checkpointInterval = checkpointInterval;
        this.watermarkInterval = watermarkInterval;

        this.lowWaterMark = 0;
        this.highWaterMark = this.lowWaterMark + watermarkInterval;
    }

    @Override
    public int checkpointInterval() {
        return this.checkpointInterval;
    }

    @Override
    public int watermarkInterval() {
        return this.watermarkInterval;
    }

    @Override
    public <O, R> @Nullable ReplicaTicket<O, R> getTicketFromCache(long timestamp) {
        return (ReplicaTicket<O, R>) this.ticketCache.get(timestamp);
    }

    @Override
    public @Nullable <O, R> ReplicaTicket<O, R> getTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return (ReplicaTicket<O, R>) this.tickets.get(key);
    }

    @Override
    public @NonNull <O, R> ReplicaTicket<O, R> newTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return (ReplicaTicket<O, R>) this.tickets.computeIfAbsent(key, k -> new DefaultReplicaTicket<>(viewNumber, seqNumber));
    }

    @Override
    public boolean completeTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return this.tickets.remove(key) != null;
    }

    private void gcCheckpoint(long checkpoint) {
        for (Entry<Long, ReplicaTicket<?, ?>> entry : this.ticketCache.entrySet()) {
            ReplicaTicket<?, ?> ticket = entry.getValue();
            if (ticket.seqNumber() <= checkpoint) {
                this.ticketCache.remove(entry.getKey());
            }
        }

        for (Long seqNumber : this.checkpoints.keySet()) {
            if (seqNumber < checkpoint) {
                this.checkpoints.remove(seqNumber);
            }
        }

        this.highWaterMark = checkpoint + this.checkpointInterval;
        this.lowWaterMark = checkpoint;
    }

    @Override
    public void appendCheckpoint(ReplicaCheckpoint checkpoint, int tolerance) {
        long seqNumber = checkpoint.lastSeqNumber();
        Collection<ReplicaCheckpoint> checkpointProofs = this.checkpoints.computeIfAbsent(seqNumber, k -> new ConcurrentLinkedQueue<>());
        checkpointProofs.add(checkpoint);

        final int stableCount = 2 * tolerance + 1;
        int matching = 0;
        for (ReplicaCheckpoint proof : checkpointProofs) {
            if (Arrays.equals(proof.digest(), checkpoint.digest())) {
                matching++;

                if (matching == stableCount) {
                    this.gcCheckpoint(seqNumber);
                    return;
                }
            }
        }
    }

    @Nullable
    private Collection<ReplicaPhaseMessage> selectPreparedProofs(ReplicaTicket<?, ?> ticket, int requiredMatches) {
        Collection<ReplicaPhaseMessage> proof = new ArrayList<>();
        for (Object prePrepareObject : ticket.messages()) {
            if (!(prePrepareObject instanceof ReplicaPrePrepare)) {
                continue;
            }

            ReplicaPrePrepare<?> prePrepare = (ReplicaPrePrepare<?>) prePrepareObject;
            proof.add(prePrepare);

            int matchingPrepares = 0;
            for (Object prepareObject : ticket.messages()) {
                if (!(prepareObject instanceof ReplicaPrepare)) {
                    continue;
                }

                ReplicaPrepare prepare = (ReplicaPrepare) prepareObject;
                if (!Arrays.equals(prePrepare.digest(), prepare.digest())) {
                    continue;
                }

                matchingPrepares++;
                proof.add(prepare);

                if (matchingPrepares == requiredMatches) {
                    return proof;
                }
            }
        }

        return null;
    }

    @Override
    public ReplicaViewChange produceViewChange(int newViewNumber, int replicaId, int tolerance) {
        long checkpoint = this.lowWaterMark;

        Collection<ReplicaCheckpoint> checkpointProofs = this.checkpoints.get(checkpoint);
        if (checkpoint != 0 && checkpointProofs == null) {
            throw new IllegalStateException("Checkpoint has diverged without any proof");
        }

        final int requiredMatches = 2 * tolerance;
        Map<Long, Collection<ReplicaPhaseMessage>> preparedProofs = new HashMap<>();
        for (ReplicaTicket<?, ?> ticket : this.ticketCache.values()) {
            long seqNumber = ticket.seqNumber();
            if (seqNumber > checkpoint) {
                Collection<ReplicaPhaseMessage> proofs = this.selectPreparedProofs(ticket, requiredMatches);
                if (proofs == null) {
                    continue;
                }

                preparedProofs.put(seqNumber, proofs);
            }
        }

        for (ReplicaTicket<?, ?> ticket : this.tickets.values()) {
            ReplicaTicketPhase phase = ticket.phase();
            if (phase == ReplicaTicketPhase.PRE_PREPARE) {
                continue;
            }

            long seqNumber = ticket.seqNumber();
            if (seqNumber > checkpoint) {
                Collection<ReplicaPhaseMessage> proofs = this.selectPreparedProofs(ticket, requiredMatches);
                if (proofs == null) {
                    continue;
                }

                preparedProofs.put(seqNumber, proofs);
            }
        }

        return new DefaultReplicaViewChange(
                newViewNumber,
                checkpoint,
                checkpointProofs,
                preparedProofs,
                replicaId);
    }

    @Override
    public void appendViewChange(ReplicaViewChange viewChange) {
        int newViewNumber = viewChange.newViewNumber();
        int replicaId = viewChange.replicaId();

        Map<Integer, ReplicaViewChange> newViewSet = this.viewChanges.computeIfAbsent(newViewNumber, k -> new ConcurrentHashMap<>());
        newViewSet.put(replicaId, viewChange);
    }

    private Collection<ReplicaPrePrepare<?>> selectPreparedProofs(int newViewNumber, long minS, long maxS, Map<Long, ReplicaPrePrepare<?>> prePrepareMap) {
        Collection<ReplicaPrePrepare<?>> sequenceProofs = new ArrayList<>();
        for (long i = minS; i < maxS; i++) {
            ReplicaPrePrepare<?> prePrepareProofMessage = prePrepareMap.get(i);
            if (prePrepareProofMessage == null) {
                prePrepareProofMessage = new DefaultReplicaPrePrepare<>(
                        newViewNumber,
                        i,
                        NULL_DIGEST,
                        NULL_REQ);
            }

            sequenceProofs.add(prePrepareProofMessage);

            ReplicaTicket<Object, Object> ticket = this.newTicket(newViewNumber, i);
            ticket.append(prePrepareProofMessage);
        }

        return sequenceProofs;
    }

    @Override
    public @Nullable ReplicaNewView produceNewView(int newViewNumber, int replicaId, int tolerance) {
        Map<Integer, ReplicaViewChange> newViewSet = this.viewChanges.get(newViewNumber);

        int count = 0;
        long minS = Long.MAX_VALUE;
        long maxS = Long.MIN_VALUE;
        Collection<ReplicaCheckpoint> minSProof = null;
        Map<Long, ReplicaPrePrepare<?>> prePrepareMap = new HashMap<>();
        for (ReplicaViewChange viewChange : newViewSet.values()) {
            count++;

            long seqNumber = viewChange.lastSeqNumber();
            Collection<ReplicaCheckpoint> proofs = viewChange.checkpointProofs();
            if (seqNumber < minS) {
                minS = seqNumber;
                minSProof = proofs;
            }

            if (seqNumber > maxS) {
                maxS = seqNumber;
            }

            for (Entry<Long, Collection<ReplicaPhaseMessage>> entry : viewChange.preparedProofs().entrySet()) {
                long prePrepareSeqNumber = entry.getKey();
                if (prePrepareSeqNumber > maxS) {
                    maxS = prePrepareSeqNumber;
                }

                for (ReplicaPhaseMessage phaseMessage : entry.getValue()) {
                    if (!(phaseMessage instanceof ReplicaPrePrepare)) {
                        continue;
                    }

                    prePrepareMap.put(prePrepareSeqNumber, (ReplicaPrePrepare<?>) phaseMessage);
                    break;
                }
            }
        }

        final int quorum = 2 * tolerance;
        if (count < quorum) {
            return null;
        }

        this.gcNewView(newViewNumber);
        if (minS > this.lowWaterMark) {
            this.checkpoints.put(minS, minSProof);
            this.gcCheckpoint(minS);
        }

        Collection<ReplicaViewChange> viewChangeProofs = new ArrayList<>(newViewSet.values());
        viewChangeProofs.add(this.produceViewChange(newViewNumber, replicaId, tolerance));

        Collection<ReplicaPrePrepare<?>> preparedProofs = this.selectPreparedProofs(newViewNumber, minS, maxS, prePrepareMap);

        return new DefaultReplicaNewView(
                newViewNumber,
                viewChangeProofs,
                preparedProofs);
    }

    private void gcNewView(int newViewNumber) {
        this.viewChanges.remove(newViewNumber);

        for (TicketKey key : this.tickets.keySet()) {
            if (key.viewNumber() != newViewNumber) {
                this.tickets.remove(key);
            }
        }
    }

    @Override
    public void acceptNewView(ReplicaNewView newView) {
        this.gcNewView(newView.newViewNumber());

        long minS = Integer.MAX_VALUE;
        Collection<ReplicaCheckpoint> checkpointProofs = null;
        for (ReplicaViewChange viewChange : newView.viewChangeProofs()) {
            long seqNumber = viewChange.lastSeqNumber();
            if (seqNumber < minS) {
                minS = seqNumber;
                checkpointProofs = viewChange.checkpointProofs();
            }
        }

        if (minS > this.lowWaterMark) {
            this.checkpoints.put(minS, checkpointProofs);
            this.gcCheckpoint(minS);
        }
    }

    @Override
    public boolean shouldBuffer() {
        return this.tickets.size() >= this.bufferThreshold;
    }

    @Override
    public <O> void buffer(ReplicaRequest<O> request) {
        this.buffer.addLast(request);
    }

    @Override
    public @Nullable <O> ReplicaRequest<O> popBuffer() {
        return (ReplicaRequest<O>) this.buffer.pollFirst();
    }

    @Override
    public boolean isBetweenWaterMarks(long seqNumber) {
        return seqNumber >= this.lowWaterMark && seqNumber <= this.highWaterMark;
    }

    private static class TicketKey {
        private final int viewNumber;
        private final long seqNumber;

        private TicketKey(int viewNumber, long seqNumber) {
            this.viewNumber = viewNumber;
            this.seqNumber = seqNumber;
        }

        public int viewNumber() {
            return this.viewNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;

            TicketKey ticketKey = (TicketKey) o;

            if (this.viewNumber != ticketKey.viewNumber) return false;
            return this.seqNumber == ticketKey.seqNumber;
        }

        @Override
        public int hashCode() {
            int result = this.viewNumber;
            result = 31 * result + (int) (this.seqNumber ^ (this.seqNumber >>> 32));
            return result;
        }
    }
}
