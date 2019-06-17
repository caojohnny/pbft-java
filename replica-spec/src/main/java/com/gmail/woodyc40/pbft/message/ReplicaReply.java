package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Replica;

/**
 * Represents a PBFT {@code REPLY} message sent by replicas
 * at the end of the computation to notify the requesting
 * client of the computation result.
 *
 * @param <R> the result type
 */
public interface ReplicaReply<R> {
    /**
     * The current view number of the state machine system.
     *
     * @return the current view number
     */
    int viewNumber();

    /**
     * The timestamp of the original {@link ReplicaRequest} that
     * prompted the computation.
     *
     * @return the timestamp
     */
    long timestamp();

    /**
     * The client ID from the original {@link ReplicaRequest} that
     * prompted the computation.
     *
     * @return the client ID
     */
    String clientId();

    /**
     * The ID number of the replica sending this message
     * given by {@link Replica#replicaId()}.
     *
     * @return the replica ID
     */
    int replicaId();

    /**
     * The result of running the computation given by
     * {@link Replica#compute(Object)}.
     *
     * @return the result
     */
    R result();
}
