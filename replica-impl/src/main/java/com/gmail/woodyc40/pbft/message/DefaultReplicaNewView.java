package com.gmail.woodyc40.pbft.message;

import java.util.Collection;

public class DefaultReplicaNewView implements ReplicaNewView {
    private final int newViewNumber;
    private final Collection<ReplicaViewChange> viewChangeProofs;
    private final Collection<ReplicaPrePrepare<?>> preparedProofs;

    public DefaultReplicaNewView(int newViewNumber,
                                 Collection<ReplicaViewChange> viewChangeProofs,
                                 Collection<ReplicaPrePrepare<?>> preparedProofs) {
        this.newViewNumber = newViewNumber;
        this.viewChangeProofs = viewChangeProofs;
        this.preparedProofs = preparedProofs;
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
    public Collection<ReplicaPrePrepare<?>> preparedProofs() {
        return this.preparedProofs;
    }
}
