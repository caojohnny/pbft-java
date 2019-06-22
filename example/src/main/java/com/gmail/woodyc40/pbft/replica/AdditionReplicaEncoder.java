package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaEncoder;
import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map.Entry;

public class AdditionReplicaEncoder implements ReplicaEncoder<AdditionOperation, AdditionResult, String> {
    private static void writeRequest(JsonObject root, ReplicaRequest<AdditionOperation> request) {
        AdditionOperation op = request.operation();
        long timestamp = request.timestamp();
        String clientId = request.clientId();

        JsonElement operation;
        if (op != null) {
            operation = new JsonObject();
            operation.getAsJsonObject().addProperty("first", op.first());
            operation.getAsJsonObject().addProperty("second", op.second());
        } else {
            operation = JsonNull.INSTANCE;
        }
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

    private static JsonObject writePrePrepare(ReplicaPrePrepare<AdditionOperation> prePrepare) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "PRE-PREPARE");
        writePhaseMessage(root, prePrepare);
        writeRequest(root, prePrepare.request());

        return root;
    }

    @Override
    public String encodePrePrepare(ReplicaPrePrepare<AdditionOperation> prePrepare) {
        return writePrePrepare(prePrepare).toString();
    }

    private static JsonObject writePrepare(ReplicaPrepare prepare) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "PREPARE");
        writePhaseMessage(root, prepare);
        root.addProperty("replica-id", prepare.replicaId());

        return root;
    }

    @Override
    public String encodePrepare(ReplicaPrepare prepare) {
        return writePrepare(prepare).toString();
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

    private static JsonObject writeCheckpoint(ReplicaCheckpoint checkpoint) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "CHECKPOINT");
        root.addProperty("last-seq-number", checkpoint.lastSeqNumber());
        root.addProperty("digest", new String(checkpoint.digest(), StandardCharsets.UTF_8));
        root.addProperty("replica-id", checkpoint.replicaId());
        return root;
    }

    @Override
    public String encodeCheckpoint(ReplicaCheckpoint checkpoint) {
        return writeCheckpoint(checkpoint).toString();
    }

    private static JsonObject writeViewChange(ReplicaViewChange viewChange) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "VIEW-CHANGE");
        root.addProperty("new-view-number", viewChange.newViewNumber());
        root.addProperty("last-seq-number", viewChange.lastSeqNumber());
        JsonArray checkpointProofs = new JsonArray();
        for (ReplicaCheckpoint checkpoint : viewChange.checkpointProofs()) {
            checkpointProofs.add(writeCheckpoint(checkpoint));
        }
        root.add("checkpoint-proofs", checkpointProofs);
        JsonArray preparedProofs = new JsonArray();
        for (Entry<Long, Collection<ReplicaPhaseMessage>> entry : viewChange.preparedProofs().entrySet()) {
            JsonObject proof = new JsonObject();
            proof.addProperty("seq-number", entry.getKey());
            JsonArray messages = new JsonArray();
            for (ReplicaPhaseMessage message : entry.getValue()) {
                if (message instanceof ReplicaPrePrepare) {
                    messages.add(writePrePrepare((ReplicaPrePrepare<AdditionOperation>) message));
                } else if (message instanceof ReplicaPrepare) {
                    messages.add(writePrepare((ReplicaPrepare) message));
                }
            }
            proof.add("messages", messages);
            preparedProofs.add(proof);
        }
        root.add("prepared-proofs", preparedProofs);
        root.addProperty("replica-id", viewChange.replicaId());

        return root;
    }

    @Override
    public String encodeViewChange(ReplicaViewChange viewChange) {
        return writeViewChange(viewChange).toString();
    }

    @Override
    public String encodeNewView(ReplicaNewView newView) {
        JsonObject root = new JsonObject();
        root.addProperty("type", "NEW-VIEW");
        root.addProperty("new-view-number", newView.newViewNumber());
        JsonArray viewChangeProofs = new JsonArray();
        for (ReplicaViewChange viewChange : newView.viewChangeProofs()) {
            viewChangeProofs.add(writeViewChange(viewChange));
        }
        root.add("view-change-proofs", viewChangeProofs);
        JsonArray preparedProofs = new JsonArray();
        for (ReplicaPrePrepare<?> prePrepare : newView.preparedProofs()) {
            preparedProofs.add(writePrePrepare((ReplicaPrePrepare<AdditionOperation>) prePrepare));
        }
        root.add("prepared-proofs", preparedProofs);

        return root.toString();
    }
}
