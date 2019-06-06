package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

/**
 * Represents a replicated state-machine in the PBFT
 * algorithm.
 *
 * @param <O> the operation type
 * @param <R> the result type of the operation
 */
public interface Replica<O, R> {
    /**
     * The ID number of this {@link Replica}.
     *
     * @return the replica ID
     */
    int replicaId();

    /**
     * Obtains this {@link Replica}'s message log as
     * specified in the PBFT algorithm;
     *
     * @return the message log
     */
    MessageLog log();

    /**
     * Called by the replica {@link Transport} to indicate
     * that a PBFT {@code REQUEST} has been received.
     *
     * @param request the received request message
     */
    void recvRequest(Request<O> request);

    /**
     * Performs the computation signified by the object
     * which represents the operation to perform on this
     * replica.
     *
     * @param operation the operation to perform
     * @return the result of the operation
     */
    R compute(O operation);

    /**
     * The type of transport used for communication between
     * replicas as well as clients.
     *
     * @param <T> the encoded type used by the
     *            {@link Transport}
     * @return the transport used by this replica
     */
    <T> Transport<T> transport();
}
