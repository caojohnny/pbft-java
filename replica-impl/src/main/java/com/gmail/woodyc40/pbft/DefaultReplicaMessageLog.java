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

    @Override
    public void appendViewChange(ReplicaViewChange viewChange) {
        int newViewNumber = viewChange.newViewNumber();
        int replicaId = viewChange.replicaId();

        Map<Integer, ReplicaViewChange> newViewSet = this.viewChanges.computeIfAbsent(newViewNumber, k -> new ConcurrentHashMap<>());
        newViewSet.put(replicaId, viewChange);
    }

    @Override
    public @Nullable ReplicaNewView produceNewView(int newViewNumber, int tolerance) {
        Map<Integer, ReplicaViewChange> newViewSet = this.viewChanges.get(newViewNumber);

        int count = 0;
        long minS = Long.MAX_VALUE;
        long maxS = Long.MIN_VALUE;
        for (ReplicaViewChange viewChange : newViewSet.values()) {
            count++;

            long seqNumber = viewChange.lastSeqNumber();
            if (seqNumber < minS) {
                minS = seqNumber;
            }

            if (seqNumber > maxS) {
                maxS = seqNumber;
            }
        }

        final int quorum = 2 * tolerance;
        if (count >= quorum) {
            Collection<ReplicaViewChange> viewChangeProofs = new ArrayList<>(newViewSet.values());
            // TODO: Figure this out??
        }

        return null;
    }

    private void gcNewView() {
        // TODO: Implement
    }

    @Override
    public void appendNewView(ReplicaNewView newView) {
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
