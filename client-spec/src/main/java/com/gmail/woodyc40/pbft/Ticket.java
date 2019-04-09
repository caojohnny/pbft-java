package com.gmail.woodyc40.pbft;

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
     * Obtains the operation that has been requested to be
     * fulfilled.
     *
     * @return the original operation
     */
    O operation();

    /**
     * The result of the operation, queued as a
     * {@link CompletableFuture},
     *
     * @return the result of the operation, whether complete
     * or not
     */
    CompletableFuture<R> result();
}
