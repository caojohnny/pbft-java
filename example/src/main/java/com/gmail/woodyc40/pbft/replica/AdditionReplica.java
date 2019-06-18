package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.*;
import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public class AdditionReplica extends DefaultReplica<AdditionOperation, AdditionResult, String> {
    private final boolean faulty;

    public AdditionReplica(int replicaId,
                           int tolerance,
                           ReplicaMessageLog log,
                           ReplicaEncoder<AdditionOperation, AdditionResult, String> encoder,
                           ReplicaDigester<AdditionOperation> digester,
                           ReplicaTransport<String> transport,
                           boolean faulty) {
        super(replicaId, tolerance, log, encoder, digester, transport);
        this.faulty = faulty;
    }

    @Override
    public AdditionResult compute(AdditionOperation operation) {
        return new AdditionResult(this.faulty ? ThreadLocalRandom.current().nextInt() : operation.first() + operation.second());
    }

    private static ReplicaRequest<AdditionOperation> readRequest(JsonObject root) {
        JsonObject operation = root.get("operation").getAsJsonObject();
        int first = operation.get("first").getAsInt();
        int second = operation.get("second").getAsInt();
        AdditionOperation additionOperation = new AdditionOperation(first, second);
        long timestamp = root.get("timestamp").getAsLong();
        String clientId = root.get("client").getAsString();

        return new DefaultReplicaRequest<>(additionOperation, timestamp, clientId);
    }

    private static ReplicaPrePrepare<AdditionOperation> readPrePrepare(JsonObject root) {
        int viewNumber = root.get("view-number").getAsInt();
        long seqNumber = root.get("seq-number").getAsLong();
        byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
        ReplicaRequest<AdditionOperation> request = readRequest(root);

        return new DefaultReplicaPrePrepare<>(
                viewNumber,
                seqNumber,
                digest,
                request);
    }

    private static ReplicaPrepare readPrepare(JsonObject root) {
        int viewNumber = root.get("view-number").getAsInt();
        long seqNumber = root.get("seq-number").getAsLong();
        byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
        int replicaId = root.get("replica-id").getAsInt();

        return new DefaultReplicaPrepare(
                viewNumber,
                seqNumber,
                digest,
                replicaId);
    }

    private static ReplicaCommit readCommit(JsonObject root) {
        int viewNumber = root.get("view-number").getAsInt();
        long seqNumber = root.get("seq-number").getAsLong();
        byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
        int replicaId = root.get("replica-id").getAsInt();

        return new DefaultReplicaCommit(
                viewNumber,
                seqNumber,
                digest,
                replicaId);
    }

    public void handleIncomingMessage(String data) {
        System.out.println(String.format("Replica %d RECV: %s", this.replicaId(), data));

        Gson gson = new Gson();
        JsonObject root = gson.fromJson(data, JsonObject.class);

        String type = root.get("type").getAsString();
        if ("REQUEST".equals(type)) {
            ReplicaRequest<AdditionOperation> request = readRequest(root);
            this.recvRequest(request);
        } else if ("PRE-PREPARE".equals(type)) {
            ReplicaPrePrepare<AdditionOperation> prePrepare = readPrePrepare(root);
            this.recvPrePrepare(prePrepare);
        } else if ("PREPARE".equals(type)) {
            ReplicaPrepare prepare = readPrepare(root);
            this.recvPrepare(prepare);
        } else if ("COMMIT".equals(type)) {
            ReplicaCommit commit = readCommit(root);
            this.recvCommit(commit);
        } else {
            throw new IllegalArgumentException("Unrecognized type: " + type);
        }
    }

}
