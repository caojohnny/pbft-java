package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

/**
 * Represents a pending request operation that is awaiting
 * to undergo the entire PBFT protocol to be processed.
 *
 * @param <O> the operation type for the request
 */
public interface Ticket<O> {
    /**
     * Obtains the sequence number assigned to this
     * particular request.
     *
     * @return the sequence number
     */
    long seqNumber();

    /**
     * The request that prompted the replica to insert this
     * {@link Ticket} into its queue.
     *
     * @return the original request
     */
    Request<O> request();
}
