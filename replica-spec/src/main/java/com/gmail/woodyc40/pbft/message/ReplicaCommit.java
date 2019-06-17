package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Replica;

/**
 * Represents a PBFT {@code COMMIT} message sent to confirm
 * the prepared state of the replica.
 */
public interface ReplicaCommit extends ReplicaPhaseMessage {
    /**
     * The replica ID number for the replica multicasting
     * this message obtained through
     * {@link Replica#replicaId()}.
     *
     * @return the replica ID number
     */
    int replicaId();
}
