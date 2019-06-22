package com.gmail.woodyc40.pbft.message;

import org.checkerframework.checker.nullness.qual.Nullable;

public class DefaultReplicaRequest<O> implements ReplicaRequest<O> {
    private final O operation;
    private final long timestamp;
    private final String clientId;

    public DefaultReplicaRequest(@Nullable O operation, long timestamp, String clientId) {
        this.operation = operation;
        this.timestamp = timestamp;
        this.clientId = clientId;
    }

    @Override
    public @Nullable O operation() {
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
