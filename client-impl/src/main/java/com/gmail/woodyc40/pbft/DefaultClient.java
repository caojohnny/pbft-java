package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.DefaultRequest;
import com.gmail.woodyc40.pbft.message.Reply;
import com.gmail.woodyc40.pbft.message.Request;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DefaultClient<O, R, T> implements Client<O, R> {
    private final int tolerance;
    private final long timeoutMs;
    private final Codec<T> codec;
    private final Transport<T> transport;

    private final Map<Long, Ticket<O, R>> tickets = new HashMap<>();

    public DefaultClient(int tolerance,
                         long timeoutMs,
                         Codec<T> codec,
                         Transport<T> transport) {
        this.tolerance = tolerance;
        this.timeoutMs = timeoutMs;
        this.codec = codec;
        this.transport = transport;
    }

    @Override
    public int tolerance() {
        return this.tolerance;
    }

    @Override
    public long timeoutMs() {
        return this.timeoutMs;
    }

    private long nextTimestamp() {
        long timestamp = System.currentTimeMillis();
        while (this.tickets.containsKey(timestamp)) {
            timestamp++;
        }

        return timestamp;
    }

    @Override
    public Ticket<O, R> sendRequest(O operation) {
        long timestamp = this.nextTimestamp();
        Request<O> req = new DefaultRequest<>(operation, timestamp, this);

        int primaryId = this.transport.primaryId();
        T encodedRequest = this.codec.encodeRequest(req);
        this.transport.sendRequest(primaryId, encodedRequest);

        Ticket<O, R> ticket = new DefaultTicket<>(this, req);
        this.tickets.put(timestamp, ticket);

        return ticket;
    }

    public boolean checkTimeout(Ticket<O, R> ticket) {
        long now = System.currentTimeMillis();
        long start = ticket.dispatchTime();
        long elapsed = now - start;
        if (elapsed >= this.timeoutMs) {
            ticket.updateDispatchTime();

            Request<O> request = ticket.request();
            T encodedRequest = this.codec.encodeRequest(request);
            this.transport.multicastRequest(encodedRequest);

            return true;
        }

        return false;
    }

    @Override
    public @Nullable Ticket<O, R> recvReply(Reply<R> reply) {
        long timestamp = reply.timestamp();
        Ticket<O, R> ticket = this.tickets.get(timestamp);
        if (ticket == null) {
            return null;
        }

        int replicaId = reply.replicaId();
        R result = reply.result();
        ticket.recvResult(replicaId, result, this.tolerance);

        CompletableFuture<R> future = ticket.result();
        if (future.isDone()) {
            this.tickets.remove(timestamp);
        }

        return ticket;
    }

    @Override
    public Codec<T> codec() {
        return this.codec;
    }

    @Override
    public Transport<T> transport() {
        return this.transport;
    }
}
