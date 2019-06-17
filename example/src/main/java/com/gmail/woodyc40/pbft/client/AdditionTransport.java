package com.gmail.woodyc40.pbft.client;

import com.gmail.woodyc40.pbft.ClientTransport;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.stream.IntStream;

public class AdditionTransport implements ClientTransport<String> {
    private final JedisPool pool;
    private final int replicas;

    private int primaryId;

    public AdditionTransport(JedisPool pool, int replicas) {
        this.pool = pool;
        this.replicas = replicas;
    }

    @Override
    public void setPrimaryId(int primaryId) {
        this.primaryId = primaryId;
    }

    @Override
    public int primaryId() {
        return this.primaryId;
    }

    @Override
    public IntStream knownReplicaIds() {
        return IntStream.range(0, this.replicas);
    }

    @Override
    public int countKnownReplicas() {
        return this.replicas;
    }

    private static String toChannel(int replicaId) {
        return "replica-" + replicaId;
    }

    @Override
    public void sendRequest(int replicaId, String request) {
        String channel = toChannel(replicaId);
        try (Jedis jedis = this.pool.getResource()) {
            jedis.publish(channel, request);
        }
    }

    @Override
    public void multicastRequest(String request) {
        for (int i = 0; i < this.replicas; i++) {
            this.sendRequest(this.replicas, request);
        }
    }
}
