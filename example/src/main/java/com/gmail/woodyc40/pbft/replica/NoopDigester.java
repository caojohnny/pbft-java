package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.ReplicaDigester;
import com.gmail.woodyc40.pbft.message.ReplicaRequest;
import com.gmail.woodyc40.pbft.type.AdditionOperation;

public class NoopDigester implements ReplicaDigester<AdditionOperation> {
    private static final byte[] EMPTY_DIGEST = new byte[0];

    @Override
    public byte[] digest(ReplicaRequest<AdditionOperation> request) {
        return EMPTY_DIGEST;
    }
}
