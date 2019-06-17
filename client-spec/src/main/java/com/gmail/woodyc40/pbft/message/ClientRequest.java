package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Client;

/**
 * Represents a {@code REQUEST} message in the PBFT protocol
 * which is dispatched by a {@link Client} wishing to
 * transmit an operation to be fulfilled by the replicas.
 *
 * @param <O> the operation type
 */
public interface ClientRequest<O> {
    /**
     * The operation, {@code o}, that the {@link Client}
     * wishes to have fulfilled by the replicas.
     *
     * @return the original dispatched operation
     */
    O operation();

    /**
     * The unique timestamp, {@code t}, which is assigned by
     * the {@link Client}.
     *
     * @return the timestamp
     */
    long timestamp();

    /**
     * The {@link Client}, {@code c}, that dispatched this
     * request.
     *
     * @param <R> the reply type for the {@link Client}
     * @param <T> the transmissible type used by the client
     * @return the dispatching client
     */
    <R, T> Client<O, R, T> client();
}
