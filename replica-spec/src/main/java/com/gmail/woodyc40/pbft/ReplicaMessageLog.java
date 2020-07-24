package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ReplicaCheckpoint;
import com.gmail.woodyc40.pbft.message.ReplicaNewView;
import com.gmail.woodyc40.pbft.message.ReplicaRequest;
import com.gmail.woodyc40.pbft.message.ReplicaViewChange;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an abstract message log using {@link ReplicaTicket}s
 * to organize pending operations from client requests.
 */
public interface ReplicaMessageLog {
    /**
     * Obtains the interval between sequence numbers for
     * which this replica will generate checkpoints.
     *
     * @return the modulo to generate checkpoints
     */
    int checkpointInterval();

    /**
     * The width between water marks, {@code k}. Each time
     * a checkpoint becomes stable, the range shifts
     * forward by {@link #watermarkInterval()}.
     *
     * @return the width of the water mark range
     */
    int watermarkInterval();

    /**
     * Obtains an already completed ticket from the cached
     * tickets in this log.
     *
     * @param key the key used to reference the ticket from
     *            the client
     * @param <O> the requested operation type
     * @param <R> the requested result type
     * @return the cached ticket
     */
    @Nullable <O, R> ReplicaTicket<O, R> getTicketFromCache(ReplicaRequestKey key);

    /**
     * Obtains a pending request in the given view with the
     * given sequence number.
     *
     * @param viewNumber the view number
     * @param seqNumber  the sequence number
     * @param <O>        the requested operation type
     * @param <R>        the requested result type
     * @return the ticket, or {@code null} if no ticket
     * exists
     */
    @Nullable <O, R> ReplicaTicket<O, R> getTicket(int viewNumber, long seqNumber);

    /**
     * Creates a new ticket in the given view number with
     * the given sequence number.
     *
     * @param viewNumber the view number
     * @param seqNumber  the sequence number
     * @param <O>        the requested operation type
     * @param <R>        the requested result type
     * @return the new ticket
     */
    @NonNull <O, R> ReplicaTicket<O, R> newTicket(int viewNumber, long seqNumber);

    /**
     * Removes the ticket for the pending request with the
     * given view and sequence numbers and stores it until
     * a checkpoint consensus has been reached.
     *
     * @param key        the replica request key used to
     *                   reference the ticket from the
     *                   client that dispatched the request
     * @param viewNumber the view number
     * @param seqNumber  the sequence number
     * @return {@code true} if a request was successfully
     * removed
     */
    boolean completeTicket(ReplicaRequestKey key, int viewNumber, long seqNumber);

    /**
     * Adds the checkpoint message to the log, clearing the
     * necessary state if a consensus is reached.
     *
     * @param checkpoint the checkpoint message to add
     * @param tolerance  the number of faulty nodes the
     *                   state machine system is capable
     *                   of tolerating, {@code f}
     */
    void appendCheckpoint(ReplicaCheckpoint checkpoint, int tolerance);

    /**
     * Creates a new {@code VIEW-CHANGE} message with the
     * required fields filled out.
     *
     * @param newViewNumber the new view number to vote
     *                      into
     * @param replicaId     the replica ID producing the
     *                      message
     * @param tolerance     the number of faulty nodes the
     *                      state machine system is capable
     *                      of tolerating, {@code f}
     * @return the new view change message
     */
    ReplicaViewChange produceViewChange(int newViewNumber, int replicaId, int tolerance);

    /**
     * Processes a view change message, adding it to the
     * log and deciding whether or not to bandwagon.
     *
     * @param viewChange    the message to add
     * @param curReplicaId  the current replica accepting
     *                      the view change
     * @param curViewNumber the current view number of the
     *                      replica accepting the view
     *                      change
     * @param tolerance     the number of faulty nodes the
     *                      state machine system is capable
     *                      of tolerating, {@code f}
     * @return a smallest view number that is being voted
     * if this replica should bandwagon, otherwise an
     * invalid view number to indicate that this replica
     * should not bandwagon
     */
    ReplicaViewChangeResult acceptViewChange(ReplicaViewChange viewChange, int curReplicaId, int curViewNumber, int tolerance);

    /**
     * Checks to see if the message log indicates that
     * enough {@link ReplicaViewChange} messages have been
     * received to constitute a quorum to change into a new
     * view.
     *
     * @param newViewNumber the new view to check for
     *                      quorum
     * @param replicaId     the replica ID of the sending
     *                      replica
     * @param tolerance     the number of faulty nodes the
     *                      state machine system is capable
     *                      of tolerating, {@code f}
     * @return a non-null {@link ReplicaNewView} message if
     * a quorum of replicas agree to change views
     */
    @Nullable
    ReplicaNewView produceNewView(int newViewNumber, int replicaId, int tolerance);

    /**
     * Processes the new view message as it pertains to the
     * log, such as garbage collection and advancing the
     * low water mark.
     *
     * @param newView the message to process
     * @return {@code true} if the new view is valid and it
     * should be further processed
     */
    boolean acceptNewView(ReplicaNewView newView);

    /**
     * Determines whether or not to buffer the next request
     * message.
     *
     * @return {@code true} if the message should be
     * buffered
     */
    boolean shouldBuffer();

    /**
     * Buffers the given request, putting it into a FIFO
     * queue for later handling.
     *
     * @param request the request to buffer
     * @param <O>     the request operation type
     */
    <O> void buffer(ReplicaRequest<O> request);

    /**
     * Pops the next request from the FIFO buffer.
     *
     * @param <O> the request operation type
     * @return the buffered request, or {@code null} if the
     * buffer is empty
     */
    @Nullable <O> ReplicaRequest<O> popBuffer();

    /**
     * Ensures that the given sequence number is between
     * the acceptable low-high water marks.
     *
     * @param seqNumber the sequence number to check
     * @return {@code true} if the given sequence number
     * is within the water marks
     */
    boolean isBetweenWaterMarks(long seqNumber);
}
