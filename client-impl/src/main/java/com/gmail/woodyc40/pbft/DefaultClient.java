package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientReply;
import com.gmail.woodyc40.pbft.message.ClientRequest;
import com.gmail.woodyc40.pbft.message.DefaultClientRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultClient<O, R, T> implements Client<O, R, T> {
    private final String clientId;
    private final int tolerance;
    private final long timeoutMs;
    private final ClientEncoder<O, T> codec;
    private final ClientTransport<T> transport;

    private volatile int primaryId;
    private final AtomicLong timestampCounter = new AtomicLong();
    private final Map<Long, ClientTicket<O, R>> tickets = new ConcurrentHashMap<>();

    public DefaultClient(String clientId,
                         int tolerance,
                         long timeoutMs,
                         ClientEncoder<O, T> encoder,
                         ClientTransport<T> transport) {
        this.clientId = clientId;
        this.tolerance = tolerance;
        this.timeoutMs = timeoutMs;
        this.codec = encoder;
        this.transport = transport;
    }

    @Override
    public String clientId() {
        return this.clientId;
    }

    @Override
    public int tolerance() {
        return this.tolerance;
    }

    @Override
    public long timeoutMs() {
        return this.timeoutMs;
    }

    @Override
    public void setPrimaryId(int primaryId) {
        this.primaryId = primaryId;
    }

    @Override
    public int primaryId() {
        return this.primaryId;
    }

    private long nextTimestamp() {
        /*
         * Timestamp is obtained using a counter simply to avoid any granularity
         * issues if requests are sent a higher rate than the system clock can
         * provide an updated time. This provides the ordering guarantees
         * specified in PBFT 4.1.
         */
        return this.timestampCounter.getAndIncrement();
    }

    @Override
    public ClientTicket<O, R> sendRequest(O operation) {
        /*
         * PBFT 4.1
         *
         * The client sends a request message containing the operation,
         * timestamp and the the client from which it is being dispatched
         * to the primary, determined based on best guess.
         */
        long timestamp = this.nextTimestamp();
        ClientRequest<O> req = new DefaultClientRequest<>(operation, timestamp, this);

        T encodedRequest = this.codec.encodeRequest(req);
        this.transport.sendRequest(this.primaryId, encodedRequest);

        /*
         * Non-standard behavior - PBFT 4.1 specifies that clients *may* allow
         * async requests, but is not specified in PBFT.
         *
         * Use a ticketing system to keep track of requests that have not yet
         * been fulfilled by the replicas and allow requests to be completed
         * asynchronously to allow this client to continue sending requests.
         *
         * Tickets are organized by their local timestamps as they are
         * guaranteed to be unique and ordered per PBFT 4.1.
         */
        ClientTicket<O, R> ticket = new DefaultClientTicket<>(this, req);
        this.tickets.put(timestamp, ticket);

        return ticket;
    }

    public boolean checkTimeout(ClientTicket<O, R> ticket) {
        long now = System.currentTimeMillis();
        long start = ticket.dispatchTime();
        long elapsed = now - start;
        if (elapsed >= this.timeoutMs) {
            ticket.updateDispatchTime();

            ClientRequest<O> request = ticket.request();
            T encodedRequest = this.codec.encodeRequest(request);
            this.transport.multicastRequest(encodedRequest);

            return true;
        }

        return false;
    }

    @Override
    public @Nullable ClientTicket<O, R> recvReply(ClientReply<R> reply) {
        long timestamp = reply.timestamp();

        /*
         * Non-standard behavior - PBFT 4.1 specifies that clients *may* allow
         * async requests, but is not specified in PBFT.
         *
         * Obtain the ticket referenced by the timestamp determined in
         * #sendRequest(...). If the ticket is null, then the request has
         * already been fulfilled by the previous replies OR that the ticket
         * was never sent in the first place. Non-faulty replicas will always
         * reply with the correct timestamp, so the incorrect timestamp value
         * can be disregarded without impacting the final result.
         */
        ClientTicket<O, R> ticket = this.tickets.get(timestamp);
        if (ticket == null) {
            return null;
        }

        // Update the client's guess at the primary with the new view number
        int viewNumber = reply.viewNumber();
        this.primaryId = viewNumber % this.transport.countKnownReplicas();

        // Process result
        int replicaId = reply.replicaId();
        R result = reply.result();
        ticket.recvResult(replicaId, result, this.tolerance);

        // Remove this ticket if the result has been computed successfully so
        // any additional replies don't take up extra processing time
        CompletableFuture<R> future = ticket.result();
        if (future.isDone()) {
            this.tickets.remove(timestamp);
        }

        return ticket;
    }

    @Override
    public ClientEncoder<O, T> encoder() {
        return this.codec;
    }

    @Override
    public ClientTransport<T> transport() {
        return this.transport;
    }
}
