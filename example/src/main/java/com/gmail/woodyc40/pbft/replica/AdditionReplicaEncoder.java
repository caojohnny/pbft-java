package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaEncoder;
import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;

public class AdditionReplicaEncoder implements ReplicaEncoder<AdditionOperation, AdditionResult, String> {
    private static void writeRequest(JsonObject root, ReplicaRequest<AdditionOperation> request) {
        AdditionOperation op = request.operation();
        long timestamp = request.timestamp();
        String clientId = request.clientId();

        JsonObject operation = new JsonObject();
        operation.addProperty("first", op.first());
        operation.addProperty("second", op.second());
        root.add("operation", operation);
        root.addProperty("timestamp", timestamp);
        root.addProperty("client", clientId);
    }

    @Override
    public String encodeRequest(ReplicaRequest<AdditionOperation> request) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "REQUEST");
        writeRequest(root, request);

        return root.toString();
    }

    private static void writePhaseMessage(JsonObject root, ReplicaPhaseMessage message) {
        root.addProperty("view-number", message.viewNumber());
        root.addProperty("seq-number", message.seqNumber());
        root.addProperty("digest", new String(message.digest(), StandardCharsets.UTF_8));
    }

    @Override
    public String encodePrePrepare(ReplicaPrePrepare<AdditionOperation> prePrepare) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "PRE-PREPARE");
        writePhaseMessage(root, prePrepare);
        writeRequest(root, prePrepare.request());

        return root.toString();
    }

    @Override
    public String encodePrepare(ReplicaPrepare prepare) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "PREPARE");
        writePhaseMessage(root, prepare);
        root.addProperty("replica-id", prepare.replicaId());

        return root.toString();
    }

    @Override
    public String encodeCommit(ReplicaCommit commit) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "COMMIT");
        writePhaseMessage(root, commit);
        root.addProperty("replica-id", commit.replicaId());

        return root.toString();
    }

    @Override
    public String encodeReply(ReplicaReply<AdditionResult> reply) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "REPLY");
        root.addProperty("view-number", reply.viewNumber());
        root.addProperty("timestamp", reply.timestamp());
        root.addProperty("client-id", reply.clientId());
        root.addProperty("replica-id", reply.replicaId());
        root.addProperty("result", reply.result().result());

        return root.toString();
    }
}
