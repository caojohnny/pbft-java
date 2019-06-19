package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ReplicaCheckpoint;
import com.gmail.woodyc40.pbft.message.ReplicaRequest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an abstract message log using {@link ReplicaTicket}s
 * to organize pending operations from client requests.
 */
public interface ReplicaMessageLog {
    /**
     * Obtains an already completed ticket from the cached
     * tickets in this log.
     *
     * @param timestamp the timestamp of the request
     * @param <O> the requested operation type
     * @return the cached ticket
     */
    <O> ReplicaTicket<O> getTicketFromCache(long timestamp);

    /**
     * Obtains a pending request in the given view with the
     * given sequence number.
     *
     * @param viewNumber the view number
     * @param seqNumber the sequence number
     * @param <O> the requested operation type
     * @return the ticket, or {@code null} if no ticket
     *         exists
     */
    @Nullable
    <O> ReplicaTicket<O> getTicket(int viewNumber, long seqNumber);

    /**
     * Creates a new ticket in the given view number with
     * the given sequence number.
     *
     * @param viewNumber the view number
     * @param seqNumber the sequence number
     * @param <O> the requested operation type
     * @return the new ticket
     */
    @NonNull
    <O> ReplicaTicket<O> newTicket(int viewNumber, long seqNumber);

    /**
     * Removes the ticket for the pending request with the
     * given view and sequence numbers and stores it until
     * a checkpoint consensus has been reached.
     *
     * @param viewNumber the view number
     * @param seqNumber the sequence number
     * @return {@code ture} if a request was successfully
     * removed
     */
    boolean completeTicket(int viewNumber, long seqNumber);

    /**
     * Adds the checkpoint message to the log, clearing the
     * necessary state if a consensus is reached.
     *
     * @param checkpoint the checkpoint message to add
     */
    void recvCheckpoint(ReplicaCheckpoint checkpoint);

    // TODO: View changes

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
     * @param <O> the request operation type
     */
    <O> void buffer(ReplicaRequest<O> request);

    /**
     * Pops the next request from the FIFO buffer.
     *
     * @param <O> the request operation type
     * @return the buffered request, or {@code null} if the
     * buffer is empty
     */
    @Nullable
    <O> ReplicaRequest<O> popBuffer();

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
