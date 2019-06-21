package com.gmail.woodyc40.pbft.message;

import java.util.Collection;
import java.util.Map;

/**
 * Represents a PBFT {@code VIEW-CHANGE} message, used by
 * replicas when an operation times out and it wishes to
 * vote out a potentially faulty primary.
 */
public interface ReplicaViewChange {
    /**
     * The new view number that all replicas should switch
     * into.
     *
     * @return the new view number
     */
    int newViewNumber();

    /**
     * The last sequence number of the stable checkpoint.
     *
     * @return the last stable checkpoint
     */
    long lastSeqNumber();

    /**
     * A set of {@code 2f + 1} valid checkpoint messages
     * that prove that the {@link #lastSeqNumber()} is
     * valid.
     *
     * @return the checkpoint message proof set
     */
    Collection<ReplicaCheckpoint> checkpointProofs();

    /**
     * A collection of {@link ReplicaPrePrepare} and
     * {@link ReplicaPrepare} messages with sequence
     * numbers greater than the {@link #lastSeqNumber()}.
     *
     * @return a set of proofs prepared phase proofs
     */
    Map<Long, Collection<ReplicaPhaseMessage>> preparedProofs();

    /**
     * The replica ID of the replica sending this message.
     *
     * @return the replica ID number
     */
    int replicaId();
}
