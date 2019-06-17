package com.gmail.woodyc40.pbft.replica;

import com.gmail.woodyc40.pbft.*;
import com.gmail.woodyc40.pbft.type.AdditionOperation;
import com.gmail.woodyc40.pbft.type.AdditionResult;

public class AdditionReplica extends DefaultReplica<AdditionOperation, AdditionResult, String> {
    public AdditionReplica(int replicaId,
                           int tolerance,
                           ReplicaMessageLog log,
                           ReplicaEncoder<AdditionOperation, AdditionResult, String> encoder,
                           ReplicaDigester<AdditionOperation> digester,
                           ReplicaTransport<String> transport) {
        super(replicaId, tolerance, log, encoder, digester, transport);
    }

    @Override
    public AdditionResult compute(AdditionOperation operation) {
        return new AdditionResult(operation.first() + operation.second());
    }
}
