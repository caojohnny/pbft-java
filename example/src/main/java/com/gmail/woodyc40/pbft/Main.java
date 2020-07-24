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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final int TOLERANCE = 1;
    private static final long TIMEOUT_MS = 1000;
    private static final int REPLICA_COUNT = 3 * TOLERANCE + 1;

    public static void main(String[] args) throws InterruptedException {
        try (JedisPool pool = new JedisPool()) {
            setupReplicas(pool);

            // Wait for PubSub listeners to setup
            Thread.sleep(1000);

            Set<ClientTicket<AdditionOperation, AdditionResult>> tickets = new HashSet<>();

            Client<AdditionOperation, AdditionResult, String> client = setupClient(pool);
            for (int i = 1; i <= 3; i++) {
                AdditionOperation operation = new AdditionOperation(i, i);
                ClientTicket<AdditionOperation, AdditionResult> ticket = client.sendRequest(operation);
                tickets.add(ticket);

                ticket.result().thenAccept(result -> {
                    synchronized (System.out) {
                        System.out.println("==========================");
                        System.out.println("==========================");
                        System.out.println(operation.first() + " + " + operation.second() + " = " + result.result());
                        System.out.println("==========================");
                        System.out.println("==========================");
                    }
                }).exceptionally(t -> {
                    throw new RuntimeException(t);
                });
            }

            waitTimeouts(client, tickets);
        }
    }

    private static long determineSleepTime(ClientTicket<?, ?> ticket) {
        long start = ticket.dispatchTime();
        long elapsed = System.currentTimeMillis() - start;
        return Math.max(0, TIMEOUT_MS - elapsed);
    }

    private static <O, R, T> void waitTimeouts(Client<O, R, T> client, Collection<ClientTicket<O, R>> tickets) {
        Set<ClientTicket<O, R>> completed = new HashSet<>();

        while (true) {
            long minTime = TIMEOUT_MS;
            for (ClientTicket<O, R> ticket : tickets) {
                long sleepTime = determineSleepTime(ticket);
                if (sleepTime > 0 && sleepTime < minTime) {
                    minTime = sleepTime;
                }

                if (ticket.result().isDone()) {
                    completed.add(ticket);
                    continue;
                }

                client.checkTimeout(ticket);
            }

            if (completed.size() == tickets.size()) {
                break;
            }

            try {
                Thread.sleep(minTime);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static <O, R, T> void waitTimeouts(Replica<O, R, T> replica) {
        while (true) {
            long minTime = TIMEOUT_MS;
            for (ReplicaRequestKey key : replica.activeTimers()) {
                long waitTime = replica.checkTimeout(key);
                if (waitTime > 0 && waitTime < minTime) {
                    minTime = waitTime;
                }
            }

            try {
                Thread.sleep(minTime);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private static void setupReplicas(JedisPool pool) {
        CountDownLatch readyLatch = new CountDownLatch(REPLICA_COUNT - 1);

        AdditionReplicaEncoder replicaEncoder = new AdditionReplicaEncoder();
        NoopDigester digester = new NoopDigester();
        AdditionReplicaTransport replicaTransport = new AdditionReplicaTransport(pool, REPLICA_COUNT);

        for (int i = 1; i < REPLICA_COUNT; i++) {
            DefaultReplicaMessageLog log = new DefaultReplicaMessageLog(100, 100, 200);
            AdditionReplica replica = new AdditionReplica(
                    i,
                    TOLERANCE,
                    TIMEOUT_MS,
                    log,
                    replicaEncoder,
                    digester,
                    replicaTransport,
                    i == 0);
            Thread listenerThread = new Thread(() -> {
                try (Jedis jedis = pool.getResource()) {
                    String channel = "replica-" + replica.replicaId();
                    JedisPubSub listener = new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            replica.handleIncomingMessage(message);
                        }
                    };

                    readyLatch.countDown();
                    jedis.subscribe(listener, channel);
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();

            Thread timeoutThread = new Thread(() -> waitTimeouts(replica));
            timeoutThread.setDaemon(true);
            timeoutThread.start();
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
        Thread thread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                String channel = client.clientId();
                JedisPubSub listener = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        client.handleIncomingMessage(message);
                    }
                };

                readyLatch.countDown();
                jedis.subscribe(listener, channel);
            }
        });
        thread.setDaemon(true);
        thread.start();

        try {
            readyLatch.await();
        } catch (InterruptedException ignored) {
        }

        return client;
    }
}
