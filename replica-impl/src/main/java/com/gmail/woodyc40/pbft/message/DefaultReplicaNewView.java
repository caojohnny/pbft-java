package com.gmail.woodyc40.pbft.message;

import java.util.Collection;

public class DefaultReplicaNewView implements ReplicaNewView {
    private final int newViewNumber;
    private final Collection<ReplicaViewChange> viewChangeProofs;
    private final Collection<ReplicaPrePrepare<?>> sequenceProofs;

    public DefaultReplicaNewView(int newViewNumber,
                                 Collection<ReplicaViewChange> viewChangeProofs,
                                 Collection<ReplicaPrePrepare<?>> sequenceProofs) {
        this.newViewNumber = newViewNumber;
        this.viewChangeProofs = viewChangeProofs;
        this.sequenceProofs = sequenceProofs;
    }

    @Override
    public int newViewNumber() {
        return this.newViewNumber;
    }

    @Override
    public Collection<ReplicaViewChange> viewChangeProofs() {
        return this.viewChangeProofs;
    }

    @Override
    public Collection<ReplicaPrePrepare<?>> sequenceProofs() {
        return this.sequenceProofs;
    }
}
