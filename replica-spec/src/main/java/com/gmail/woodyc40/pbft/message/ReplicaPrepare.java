package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Replica;

/**
 * Represents a PBFT {@code PREPARE} message sent in
 * response to a {@link ReplicaPrePrepare} message being
 * received.
 */
public interface ReplicaPrepare extends ReplicaPhaseMessage {
    /**
     * The ID number for the replica that multicasts this
     * message, obtainable through
     * {@link Replica#replicaId()}.
     *
     * @return the replica ID
     */
    int replicaId();
}
