package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ReplicaRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a pending request operation that is awaiting
 * to undergo the entire PBFT protocol to be processed.
 *
 * @param <O> the operation type for the request
 */
public interface ReplicaTicket<O, R> {
    /**
     * Obtains the view number in which this ticket was
     * created.
     *
     * @return the view number
     */
    int viewNumber();

    /**
     * Obtains the sequence number assigned to the pending
     * request.
     *
     * @return the sequence number
     */
    long seqNumber();

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
     * @param tolerance the number of failures {@code f}
     *                  that should be guarded against
     *                  checking the prepared state
     * @return the {@code prepared} state of the operation
     */
    boolean isPrepared(int tolerance);

    /**
     * Checks the state of the ticket to determine whether
     * the condition {@code committed-local} is
     * {@code true}.
     *
     * @param tolerance the number of failures {@code f}
     *                  that should be guarded against
     *                  checking the prepared state
     * @return the {@code committed-local} state
     */
    boolean isCommittedLocal(int tolerance);

    /**
     * Obtains the current ticket phase.
     *
     * @return the current phase
     */
    ReplicaTicketPhase phase();

    /**
     * Performs a compare-and-set operation on the phase
     * of the ticket.
     *
     * @param old the old phase
     * @param next the next phase
     * @return {@code true} if the operation succeeded
     */
    boolean casPhase(ReplicaTicketPhase old, ReplicaTicketPhase next);

    /**
     * Obtains a collection of the messages pertaining to
     * the same operation referenced by this ticket.
     *
     * @return the collection of messages
     */
    Collection<Object> messages();

    /**
     * The request that prompted the replica to insert this
     * {@link ReplicaTicket} into its queue.
     *
     * @return the original request
     */
    @Nullable
    ReplicaRequest<O> request();

    /**
     * A future representing the computed value of the
     * result.
     *
     * @return a future result
     */
    CompletableFuture<R> result();
}
