package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaEncoder;
import com.gmail.woodyc40.pbft.message.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;

// TODO: Handle message encoding
public class AdditionReplicaEncoder implements ReplicaEncoder<AdditionOperation, AdditionResult, String> {
    @Override
    public String encodeRequest(ReplicaRequest<AdditionOperation> request) {
        return null;
    }

    @Override
    public String encodePrePrepare(ReplicaPrePrepare<AdditionOperation> prePrepare) {
        return null;
    }

    @Override
    public String encodePrepare(ReplicaPrepare prepare) {
        return null;
    }

    @Override
    public String encodeCommit(ReplicaCommit commit) {
        return null;
    }

    @Override
    public String encodeReply(ReplicaReply<AdditionResult> reply) {
        return null;
    }
}
