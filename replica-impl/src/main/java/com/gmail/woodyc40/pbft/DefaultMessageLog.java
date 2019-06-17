package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

public class DefaultMessageLog implements MessageLog {
    private final int bufferThreshold;

    private final Map<TicketKey, Ticket<?>> tickets = new HashMap<>();
    private final Deque<Request<?>> buffer = new LinkedList<>();

    // TODO: replicas remember the last reply message they sent to each client
    // TODO: Set the water marks
    private long lowWaterMark;
    private long highWaterMark;

    public DefaultMessageLog(int bufferThreshold, int highWaterMark) {
        this.bufferThreshold = bufferThreshold;
        this.highWaterMark = highWaterMark;
    }

    @Override
    public @Nullable <O> Ticket<O> getTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return (Ticket<O>) this.tickets.get(key);
    }

    @Override
    public @NonNull <O> Ticket<O> newTicket(int viewNumber, long seqNumber, Request<O> request) {
        Ticket<O> ticket = new DefaultTicket<>(viewNumber, seqNumber, request);

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
    public <O> void buffer(Request<O> request) {
        this.buffer.addLast(request);
    }

    @Override
    public @Nullable <O> Request<O> popBuffer() {
        return (Request<O>) this.buffer.pollFirst();
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
