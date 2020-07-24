package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.*;
import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AdditionReplica extends DefaultReplica<AdditionOperation, AdditionResult, String> {
    private final boolean faulty;

    public AdditionReplica(int replicaId,
                           int tolerance,
                           long timeout,
                           ReplicaMessageLog log,
                           ReplicaEncoder<AdditionOperation, AdditionResult, String> encoder,
                           ReplicaDigester<AdditionOperation> digester,
                           ReplicaTransport<String> transport,
                           boolean faulty) {
        super(replicaId, tolerance, timeout, log, encoder, digester, transport);
        this.faulty = faulty;
    }

    @Override
    public AdditionResult compute(AdditionOperation operation) {
        return new AdditionResult(this.faulty ? ThreadLocalRandom.current().nextInt() : operation.first() + operation.second());
    }

    private static ReplicaRequest<AdditionOperation> readRequest(JsonObject root) {
        JsonElement operation = root.get("operation");
        AdditionOperation additionOperation = null;
        if (!operation.isJsonNull()) {
            JsonObject operationObject = operation.getAsJsonObject();
            int first = operationObject.get("first").getAsInt();
            int second = operationObject.get("second").getAsInt();
            additionOperation = new AdditionOperation(first, second);
        }
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

    private static ReplicaCheckpoint readCheckpoint(JsonObject root) {
        long lastSeqNumber = root.get("last-seq-number").getAsLong();
        byte[] digest = root.get("digest").getAsString().getBytes(StandardCharsets.UTF_8);
        int replicaId = root.get("replica-id").getAsInt();

        return new DefaultReplicaCheckpoint(
                lastSeqNumber,
                digest,
                replicaId);
    }

    private static ReplicaViewChange readViewChange(JsonObject root) {
        int newViewNumber = root.get("new-view-number").getAsInt();
        long lastSeqNumber = root.get("last-seq-number").getAsLong();

        Collection<ReplicaCheckpoint> checkpointProofs = new ArrayList<>();
        JsonArray checkpointProofsArray = root.get("checkpoint-proofs").getAsJsonArray();
        for (JsonElement checkpoint : checkpointProofsArray) {
            checkpointProofs.add(readCheckpoint(checkpoint.getAsJsonObject()));
        }

        Map<Long, Collection<ReplicaPhaseMessage>> preparedProofs = new HashMap<>();
        JsonArray preparedProofsArray = root.get("prepared-proofs").getAsJsonArray();
        for (JsonElement element : preparedProofsArray) {
            JsonObject proof = element.getAsJsonObject();
            long seqNumber = proof.get("seq-number").getAsLong();

            Collection<ReplicaPhaseMessage> messages = new ArrayList<>();
            JsonArray messagesArray = proof.get("messages").getAsJsonArray();
            for (JsonElement message : messagesArray) {
                String type = message.getAsJsonObject().get("type").getAsString();
                if ("PRE-PREPARE".equals(type)) {
                    messages.add(readPrePrepare(message.getAsJsonObject()));
                } else if ("PREPARE".equals(type)) {
                    messages.add(readPrepare(message.getAsJsonObject()));
                }
            }

            preparedProofs.put(seqNumber, messages);
        }
        int replicaId = root.get("replica-id").getAsInt();

        return new DefaultReplicaViewChange(
                newViewNumber,
                lastSeqNumber,
                checkpointProofs,
                preparedProofs,
                replicaId);
    }

    private static ReplicaNewView readNewView(JsonObject root) {
        int newViewNumber = root.get("new-view-number").getAsInt();

        Collection<ReplicaViewChange> viewChangeProofs = new ArrayList<>();
        JsonArray viewChangesArray = root.get("view-change-proofs").getAsJsonArray();
        for (JsonElement element : viewChangesArray) {
            viewChangeProofs.add(readViewChange(element.getAsJsonObject()));
        }

        Collection<ReplicaPrePrepare<?>> preparedProofs = new ArrayList<>();
        JsonArray preparedArray = root.get("prepared-proofs").getAsJsonArray();
        for (JsonElement element : preparedArray) {
            preparedProofs.add(readPrePrepare(element.getAsJsonObject()));
        }

        return new DefaultReplicaNewView(
                newViewNumber,
                viewChangeProofs,
                preparedProofs);
    }

    public void handleIncomingMessage(String data) {
        // System.out.println(String.format("RECV: REPLICA %d: %s", this.replicaId(), data));

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
        } else if ("CHECKPOINT".equals(type)) {
            ReplicaCheckpoint checkpoint = readCheckpoint(root);
            this.recvCheckpoint(checkpoint);
        } else if ("VIEW-CHANGE".equals(type)) {
            ReplicaViewChange viewChange = readViewChange(root);
            this.recvViewChange(viewChange);
        } else if ("NEW-VIEW".equals(type)) {
            ReplicaNewView newView = readNewView(root);
            this.recvNewView(newView);
        } else {
            throw new IllegalArgumentException("Unrecognized type: " + type);
        }
    }
}
