package com.gmail.woodyc40.pbft.message;

public class DefaultReplicaCheckpoint implements ReplicaCheckpoint {
    private final long lastSeqNumber;
    private final byte[] digest;
    private final int replicaId;

    public DefaultReplicaCheckpoint(long lastSeqNumber, byte[] digest, int replicaId) {
        this.lastSeqNumber = lastSeqNumber;
        this.digest = digest;
        this.replicaId = replicaId;
    }

    @Override
    public long lastSeqNumber() {
        return this.lastSeqNumber;
    }

    @Override
    public byte[] digest() {
        return this.digest;
    }

    @Override
    public int replicaId() {
        return this.replicaId;
    }
}
