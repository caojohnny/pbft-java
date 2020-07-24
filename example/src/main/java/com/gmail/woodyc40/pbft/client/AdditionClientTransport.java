package com.gmail.woodyc40.pbft.client;

import com.gmail.woodyc40.pbft.ClientTransport;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.stream.IntStream;

public class AdditionClientTransport implements ClientTransport<String> {
    private final JedisPool pool;
    private final int replicas;

    public AdditionClientTransport(JedisPool pool, int replicas) {
        this.pool = pool;
        this.replicas = replicas;
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
        System.out.println(String.format("SEND: CLIENT -> %d: %s", replicaId, request));

        String channel = toChannel(replicaId);
        try (Jedis jedis = this.pool.getResource()) {
            jedis.publish(channel, request);
        }
    }

    @Override
    public void multicastRequest(String request) {
        for (int i = 0; i < this.replicas; i++) {
            this.sendRequest(i, request);
        }
    }
}
