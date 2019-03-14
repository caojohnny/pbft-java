package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Reply;
import com.gmail.woodyc40.pbft.protocol.Request;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultClient<Op, R, T> extends AbstractClient<Op, R, T> {
    private final AtomicLong timestampCounter = new AtomicLong();
    private final Map<Long, ResultTicket<Op, R>> ticketMap =
            new ConcurrentHashMap<>();

    public DefaultClient(NodeOptions<Op, R, T> options) {
        super(options);
    }

    protected long nextTimestamp() {
        return this.timestampCounter.incrementAndGet();
    }

    public long send(Op operation) {
        Request<Op, R, T> request = this.sendRequest(operation);
        long timestamp = request.timestamp();
        this.ticketMap.put(timestamp, new ResultTicket<>(request, this.tolerance()));

        return timestamp;
    }

    public R recv(long id) throws InterruptedException {
        ResultTicket<Op, R> ticket = this.ticketMap.get(id);

        long beginTime = System.currentTimeMillis();
        long remainingTimeout = this.timeout();
        synchronized (ticket) {
            while (!ticket.isReady()) {
                ticket.wait(remainingTimeout);

                // Calculate the time left on the timer
                long endTime = System.currentTimeMillis();
                long elapsed = endTime - beginTime;
                remainingTimeout = this.timeout() - elapsed;

                // If we timeout, multicast to all replicas
                if (remainingTimeout <= 0) {
                    // Keep trying infinitely
                    remainingTimeout = this.timeout();
                    ticket.reset();

                    this.multicastRequest(ticket.request());
                }
            }

            return ticket.result();
        }
    }

    @Override
    public Request<Op, R, T> sendRequest(Op operation) {
        long timestamp = this.nextTimestamp();
        Request<Op, R, T> request = new Request<>(operation, timestamp, this);

        T encoded = this.encoder().encode(request);
        this.transport().send(this.transport().primaryId(), encoded);

        return request;
    }

    protected void multicastRequest(Request<Op, R, T> request) {
        T encoded = this.encoder().encode(request);
        this.transport().multicast(this.roster(), encoded);
    }

    @Override
    public void recvReply(Reply<R> reply) {
        long timestamp = reply.timestamp();

        ResultTicket<Op, R> ticket = this.ticketMap.get(timestamp);
        if (ticket == null) {
            throw new IllegalStateException("Received result before a request was made");
        }

        ticket.recvReply(reply);
    }
}
