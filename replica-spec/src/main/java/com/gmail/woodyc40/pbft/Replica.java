package com.gmail.woodyc40.pbft;

/**
 * Represents a replicated state-machine in the PBFT
 * algorithm.
 */
public interface Replica<O, T> {
    /**
     * The ID number of this {@link Replica}.
     *
     * @return the replica ID
     */
    int replicaId();

    /**
     * Performs the compuation signified by the object which
     * represents the operation to perform on this replica.
     *
     * @param operation the operation to perform
     * @return the result of the operation
     */
    T compute(O operation);
}
