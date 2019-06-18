package com.gmail.woodyc40.pbft.message;

public class DefaultReplicaRequest<O> implements ReplicaRequest<O> {
    private final O operation;
    private final long timestamp;
    private final String clientId;

    public DefaultReplicaRequest(O operation, long timestamp, String clientId) {
        this.operation = operation;
        this.timestamp = timestamp;
        this.clientId = clientId;
    }

    @Override
    public O operation() {
        return this.operation;
    }

    @Override
    public long timestamp() {
        return this.timestamp;
    }

    @Override
    public String clientId() {
        return this.clientId;
    }
}
