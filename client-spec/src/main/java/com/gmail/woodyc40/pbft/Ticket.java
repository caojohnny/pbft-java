package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link Ticket} represents a sort of receipt for sending
 * an operation type {@code O} using a {@link Client}. It
 * allows a user to reference the sent operation and
 * retrieve its result when the {@link Client} confirms that
 * quorum has been acheived.
 *
 * @param <O> the operation type
 * @param <R> the result type
 */
public interface Ticket<O, R> {
    /**
     * Obtains the client that created this {@link Ticket}.
     *
     * @return the client that sent the request represented
     * by this receipt
     */
    Client<O, R> client();

    /**
     * Obtains the representation of the dispatched
     * {@link Request} object as a result of calling
     * {@link Client#sendRequest(Object)}.
     *
     * @return the request
     */
    Request<O> request();

    /**
     * The result of the operation, queued as a
     * {@link CompletableFuture},
     *
     * @return the result of the operation, whether complete
     * or not
     */
    CompletableFuture<R> result();
}
