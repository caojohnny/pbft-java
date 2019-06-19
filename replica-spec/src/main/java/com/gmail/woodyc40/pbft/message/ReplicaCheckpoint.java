package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Replica;

/**
 * Represents a PBFT {@code CHECKPOINT} message that
 * indicates to other replicas that the current replica
 * has reached a stable checkpoint.
 */
public interface ReplicaCheckpoint {
    /**
     * The last sequence number of the request that was
     * handled by this replica.
     *
     * @return the sequence number
     */
    long lastSeqNumber();

    /**
     * The digest of the current replica state, obtained
     * using {@link Replica#digestState()} and compared
     * with other checkpoint messages when this message is
     * received by the target replicas.
     *
     * @return the state digest
     */
    byte[] digest();

    /**
     * The ID number of the replica sending this message.
     *
     * @return the replica ID number
     */
    int replicaId();
}
