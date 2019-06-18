package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultClientTicket<O, R> implements ClientTicket<O, R> {
    private final Client<O, R, ?> client;
    private final ClientRequest<O> request;
    private final CompletableFuture<R> future = new CompletableFuture<>();

    private final AtomicLong dispatchTime;
    private final Map<Integer, R> replies = new ConcurrentHashMap<>();

    public DefaultClientTicket(Client<O, R, ?> client, ClientRequest<O> request) {
        this.client = client;
        this.request = request;
        this.dispatchTime = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    public <T> Client<O, R, T> client() {
        return (Client<O, R, T>) this.client;
    }

    @Override
    public void updateDispatchTime() {
        this.dispatchTime.set(System.currentTimeMillis());
    }

    @Override
    public long dispatchTime() {
        return this.dispatchTime.get();
    }

    @Override
    public ClientRequest<O> request() {
        return this.request;
    }

    @Override
    public void recvResult(int replicaId, R result, int tolerance) {
        this.replies.put(replicaId, result);

        int quorum = tolerance + 1;
        Map<R, Integer> freqMap = new HashMap<>();
        for (R value : this.replies.values()) {
            int freq = freqMap.compute(value, (k, v) -> v == null ? 1 : v + 1);
            if (freq == quorum) {
                this.future.complete(value);
                return;
            }
        }
    }

    @Override
    public CompletableFuture<R> result() {
        return this.future;
    }
}
