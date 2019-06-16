package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Represents a pending request operation that is awaiting
 * to undergo the entire PBFT protocol to be processed.
 *
 * @param <O> the operation type for the request
 */
public interface Ticket<O> {
    /**
     * Obtains the view number of the replica system at the
     * time that the ticket was initialized.
     *
     * @return the view number
     */
    int viewNumber();

    /**
     * Obtains the sequence number assigned to this
     * particular request.
     *
     * @return the sequence number
     */
    long seqNumber();

    /**
     * Obtains a collection of the messages pertaining to
     * the same operation referenced by this ticket.
     *
     * @return the collection of messages
     */
    Collection<Object> messages();

    /**
     * The request that prompted the replica to insert this
     * {@link Ticket} into its queue.
     *
     * @return the original request
     */
    Request<O> request();
}
