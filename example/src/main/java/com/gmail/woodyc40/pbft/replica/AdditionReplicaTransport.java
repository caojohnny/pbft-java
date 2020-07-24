package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaTransport;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

public class AdditionReplicaTransport implements ReplicaTransport<String> {
    private final JedisPool pool;
    private final int replicas;

    public AdditionReplicaTransport(JedisPool pool, int replicas) {
        this.pool = pool;
        this.replicas = replicas;
    }

    @Override
    public int countKnownReplicas() {
        return this.replicas;
    }

    @Override
    public IntStream knownReplicaIds() {
        return IntStream.range(0, this.replicas);
    }

    private static String toChannel(int replicaId) {
        return "replica-" + replicaId;
    }

    @Override
    public void sendMessage(int replicaId, String data) {
        System.out.println(String.format("SEND: REPLICA -> %d: %s", replicaId, data));

        String channel = toChannel(replicaId);
        try (Jedis jedis = this.pool.getResource()) {
            jedis.publish(channel, data);
        }
    }

    @Override
    public void multicast(String data, int... ignoredReplicas) {
        Set<Integer> ignored = new HashSet<>(ignoredReplicas.length);
        for (int id : ignoredReplicas) {
            ignored.add(id);
        }

        for (int i = 0; i < this.replicas; i++) {
            if (!ignored.contains(i)) {
                this.sendMessage(i, data);
            }
        }
    }

    @Override
    public void sendReply(String clientId, String reply) {
        System.out.println(String.format("SEND: REPLY -> %s: %s", clientId, reply));

        try (Jedis jedis = this.pool.getResource()) {
            jedis.publish(clientId, reply);
        }
    }
}
