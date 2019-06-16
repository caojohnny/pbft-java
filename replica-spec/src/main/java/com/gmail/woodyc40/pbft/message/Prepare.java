package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Replica;
import com.gmail.woodyc40.pbft.Transport;

/**
 * Represents a PBFT {@code PREPARE} message sent in
 * response to a {@link PrePrepare} message being
 * received.
 */
public interface Prepare extends PhaseMessage {
    /**
     * The ID number for the replica that multicasts this
     * message, obtainable through
     * {@link Replica#replicaId()}.
     *
     * @return the replica ID
     */
    int replicaId();
}
