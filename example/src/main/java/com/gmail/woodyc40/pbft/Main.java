package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.client.AdditionClientEncoder;
import com.gmail.woodyc40.pbft.client.AdditionClientTransport;
import com.gmail.woodyc40.pbft.message.DefaultClientReply;
import com.gmail.woodyc40.pbft.replica.AdditionReplica;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaEncoder;
import com.gmail.woodyc40.pbft.replica.AdditionReplicaTransport;
import com.gmail.woodyc40.pbft.replica.NoopDigester;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.CompletableFuture;

public class Main {
    private static final int TOLERANCE = 1;
    private static final long TIMEOUT_MS = 1000;
    private static final int REPLICA_COUNT = 3 * TOLERANCE + 1;

    private static final ThreadLocal<Gson> GSON_PROVIDER = ThreadLocal.withInitial(Gson::new);

    public static void main(String[] args) {
        try (JedisPool pool = new JedisPool()) {
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
                        replicaTransport);
                new Thread(() -> {
                    try (Jedis jedis = pool.getResource()) {
                        jedis.subscribe(new JedisPubSub() {
                            @Override
                            public void onMessage(String channel, String message) {
                                handleReplica(replica, message);
                            }
                        }, "replica-" + replica.replicaId());
                    }
                }).start();
            }

            AdditionClientEncoder clientEncoder = new AdditionClientEncoder();
            AdditionClientTransport clientTransport = new AdditionClientTransport(pool, REPLICA_COUNT);

            Client<AdditionOperation, AdditionResult, String> client = new DefaultClient<>(
                    "client-0",
                    TOLERANCE,
                    TIMEOUT_MS,
                    clientEncoder,
                    clientTransport);

            new Thread(() -> executeOperation(client)).start();

            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        handleClient(client, message);
                    }
                }, client.clientId());
            }
        }
    }

    private static void executeOperation(Client<AdditionOperation, AdditionResult, String> client) {
        AdditionOperation operation = new AdditionOperation(1, 1);
        ClientTicket<AdditionOperation, AdditionResult> ticket = client.sendRequest(operation);

        CompletableFuture<AdditionResult> future = ticket.result();
        future.thenAccept(result -> System.out.println("Result: " + result.result()));

        while (!future.isDone()) {
            try {
                Thread.sleep(TIMEOUT_MS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            client.checkTimeout(ticket);
        }
    }

    private static void handleReplica(Replica<AdditionOperation, AdditionResult, String> replica,
                                      String data) {
        Gson gson = GSON_PROVIDER.get();
        JsonObject root = gson.fromJson(data, JsonObject.class);

        // TODO: Handle incoming messages
    }

    private static void handleClient(Client<AdditionOperation, AdditionResult, String> client,
                                     String data) {
        Gson gson = GSON_PROVIDER.get();
        JsonObject root = gson.fromJson(data, JsonObject.class);

        String type = root.get("type").getAsString();
        if ("REPLY".equals(type)) {
            int viewNumber = root.get("view-number").getAsInt();
            long timestamp = root.get("timestamp").getAsLong();
            int replicaId = root.get("replica-id").getAsInt();
            int result = root.get("result").getAsInt();

            AdditionResult additionResult = new AdditionResult(result);
            DefaultClientReply<AdditionResult> reply = new DefaultClientReply<>(
                    viewNumber,
                    timestamp,
                    client,
                    replicaId,
                    additionResult);
            client.recvReply(reply);
        }

        throw new IllegalArgumentException("Unrecognized type: " + type);
    }
}
