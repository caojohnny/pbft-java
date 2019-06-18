package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.client.AdditionClient;
import com.gmail.woodyc40.pbft.client.AdditionClientEncoder;
import com.gmail.woodyc40.pbft.client.AdditionClientTransport;
import com.gmail.woodyc40.pbft.replica.AdditionReplica;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaEncoder;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaTransport;
import com.gmail.woodyc40.pbft.replica.NoopDigester;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final int TOLERANCE = 1;
    private static final long TIMEOUT_MS = 50000;
    private static final int REPLICA_COUNT = 3 * TOLERANCE + 1;

    public static void main(String[] args) {
        try (JedisPool pool = new JedisPool()) {
            setupReplicas(pool);

            Client<AdditionOperation, AdditionResult, String> client = setupClient(pool);
            AdditionOperation operation = new AdditionOperation(1, 1);
            ClientTicket<AdditionOperation, AdditionResult> ticket = client.sendRequest(operation);

            CompletableFuture<AdditionResult> future = ticket.result();
            future.thenAccept(result -> System.out.println("Result: " + result.result()));

            while (true) {
                try {
                    Thread.sleep(determineSleepTime(ticket));
                } catch (InterruptedException e) {
                    break;
                }

                if (future.isDone()) {
                    break;
                }

                client.checkTimeout(ticket);
            }
        }
    }

    private static void setupReplicas(JedisPool pool) {
        CountDownLatch readyLatch = new CountDownLatch(REPLICA_COUNT);
        for (int i = 0; i < REPLICA_COUNT; i++) {
            DefaultReplicaMessageLog log = new DefaultReplicaMessageLog(100, 100);
            AdditionReplicaEncoder replicaEncoder = new AdditionReplicaEncoder();
            NoopDigester digester = new NoopDigester();
            AdditionReplicaTransport replicaTransport = new AdditionReplicaTransport(pool, REPLICA_COUNT);

            AdditionReplica replica = new AdditionReplica(
                    i,
                    TOLERANCE,
                    log,
                    replicaEncoder,
                    digester,
                    replicaTransport,
                    false);
            new Thread(() -> {
                try (Jedis jedis = pool.getResource()) {
                    readyLatch.countDown();
                    jedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            replica.handleIncomingMessage(message);
                        }
                    }, "replica-" + replica.replicaId());
                }
            }).start();
        }

        try {
            readyLatch.await();
        } catch (InterruptedException ignored) {
        }
    }

    private static Client<AdditionOperation, AdditionResult, String> setupClient(JedisPool pool) {
        AdditionClientEncoder clientEncoder = new AdditionClientEncoder();
        AdditionClientTransport clientTransport = new AdditionClientTransport(pool, REPLICA_COUNT);

        AdditionClient client = new AdditionClient(
                "client-0",
                TOLERANCE,
                TIMEOUT_MS,
                clientEncoder,
                clientTransport);

        CountDownLatch readyLatch = new CountDownLatch(1);
        new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                readyLatch.countDown();
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        client.handleIncomingMessage(message);
                    }
                }, client.clientId());
            }
        }).start();

        try {
            readyLatch.await();
        } catch (InterruptedException ignored) {
        }

        return client;
    }

    private static long determineSleepTime(ClientTicket<AdditionOperation, AdditionResult> ticket) {
        long start = ticket.dispatchTime();
        long elapsed = System.currentTimeMillis() - start;
        return Math.max(0, TIMEOUT_MS - elapsed);
    }
}
