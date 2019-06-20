package com.gmail.woodyc40.pbft.message;

import java.util.Collection;

public class DefaultReplicaViewChange implements ReplicaViewChange {
    private final int newViewNumber;
    private final long lastSeqNumber;
    private final Collection<ReplicaCheckpoint> checkpointProofs;
    private final Collection<Collection<ReplicaPhaseMessage>> preparedProofs;
    private final int replicaId;

    public DefaultReplicaViewChange(int newViewNumber,
                                    long lastSeqNumber,
                                    Collection<ReplicaCheckpoint> checkpointProofs,
                                    Collection<Collection<ReplicaPhaseMessage>> preparedProofs,
                                    int replicaId) {
        this.newViewNumber = newViewNumber;
        this.lastSeqNumber = lastSeqNumber;
        this.checkpointProofs = checkpointProofs;
        this.preparedProofs = preparedProofs;
        this.replicaId = replicaId;
    }

    @Override
    public int newViewNumber() {
        return this.newViewNumber;
    }

    @Override
    public long lastSeqNumber() {
        return this.lastSeqNumber;
    }

    @Override
    public Collection<ReplicaCheckpoint> checkpointProofs() {
        return this.checkpointProofs;
    }

    @Override
    public Collection<Collection<ReplicaPhaseMessage>> preparedProofs() {
        return this.preparedProofs;
    }

    @Override
    public int replicaId() {
        return this.replicaId;
    }
}
