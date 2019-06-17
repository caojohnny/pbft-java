package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

import java.util.Collection;

/**
 * Represents a pending request operation that is awaiting
 * to undergo the entire PBFT protocol to be processed.
 *
 * @param <O> the operation type for the request
 */
public interface Ticket<O> {
    /**
     * Appends the given message to the log associated
     * with the pending operation represented by this
     * ticket.
     *
     * @param message the message to append
     */
    void append(Object message);

    /**
     * Checks the state of the ticket to determine whether
     * the condition {@code prepared} is {@code true}.
     *
     * @return the {@code prepared} state of the operation
     */
    boolean isPrepared();

    /**
     * Checks the state of the ticket to determine whether
     * the condition {@code committed-local} is
     * {@code true}.
     *
     * @return the {@code committed-local} state
     */
    boolean isCommittedLocal();

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
