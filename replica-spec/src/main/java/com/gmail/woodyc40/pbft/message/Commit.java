package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Replica;

/**
 * Represents a PBFT {@code COMMIT} message sent to confirm
 * the prepared state of the replica.
 */
public interface Commit {
    /**
     * The current view number of the state machine system.
     *
     * @return the current view number
     */
    int viewNumber();

    /**
     * The sequence number assigned by the primary to the
     * original {@link PrePrepare} which prompted the
     * computation.
     *
     * @return the sequence number
     */
    long seqNumber();

    /**
     * The digest of the {@link Request} that prompted the
     * computation.
     *
     * @return the digest
     */
    byte[] digest();

    /**
     * The replica ID number for the replica multicasting
     * this message obtained through
     * {@link Replica#replicaId()}.
     *
     * @return the replica ID number
     */
    int replicaId();
}
