package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ReplicaRequest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

// TODO: Handle out-of-order message delivery
public class DefaultReplicaMessageLog implements ReplicaMessageLog {
    private final int bufferThreshold;

    private final Map<TicketKey, ReplicaTicket<?>> tickets = new ConcurrentHashMap<>();
    private final Deque<ReplicaRequest<?>> buffer = new ConcurrentLinkedDeque<>();

    // TODO: replicas remember the last reply message they sent to each client
    // TODO: Set the water marks
    private long lowWaterMark;
    private long highWaterMark;

    public DefaultReplicaMessageLog(int bufferThreshold, int highWaterMark) {
        this.bufferThreshold = bufferThreshold;
        this.highWaterMark = highWaterMark;
    }

    @Override
    public @Nullable <O> ReplicaTicket<O> getTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return (ReplicaTicket<O>) this.tickets.get(key);
    }

    @Override
    public @NonNull <O> ReplicaTicket<O> newTicket(int viewNumber, long seqNumber, ReplicaRequest<O> request) {
        ReplicaTicket<O> ticket = new DefaultReplicaTicket<>(viewNumber, seqNumber, request);

        TicketKey key = new TicketKey(viewNumber, seqNumber);
        this.tickets.put(key, ticket);

        return ticket;
    }

    @Override
    public boolean deleteTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return this.tickets.remove(key) != null;
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
