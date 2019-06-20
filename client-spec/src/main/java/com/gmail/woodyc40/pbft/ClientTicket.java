package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientReply;
import com.gmail.woodyc40.pbft.message.ClientRequest;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link ClientTicket} represents a sort of receipt for sending
 * an operation type {@code O} using a {@link Client}. It
 * allows a user to reference the sent operation and
 * retrieve its result when the {@link Client} confirms that
 * quorum has been acheived.
 *
 * @param <O> the operation type
 * @param <R> the result type
 */
public interface ClientTicket<O, R> {
    /**
     * Obtains the client that created this {@link ClientTicket}.
     *
     * @param <T> the transmissible type used by the client
     * @return the client that sent the request represented
     * by this receipt
     */
    <T> Client<O, R, T> client();

    /**
     * Sets the dispatch time to
     * {@link System#currentTimeMillis()}
     */
    void updateDispatchTime();

    /**
     * Obtains the time at which the request was last sent,
     * might potentially change due to a timeout.
     *
     * @return the dispatch time, obtained from
     * {@link System#currentTimeMillis()}
     */
    long dispatchTime();

    /**
     * Obtains the representation of the dispatched
     * {@link ClientRequest} object as a result of calling
     * {@link Client#sendRequest(Object)}.
     *
     * @return the request
     */
    ClientRequest<O> request();

    /**
     * Called by a {@link Client} to indicate that it has
     * received a {@link ClientReply} containing a result which
     * should be handled by this pending request ticket.
     *
     * @param replicaId the ID of the replica sending the
     *                  reply
     * @param result the result
     * @param tolerance the number of allowable faulty
     *                  replies, {@code f}
     */
    void recvResult(int replicaId, R result, int tolerance);

    /**
     * The result of the operation, queued as a
     * {@link CompletableFuture},
     *
     * @return the result of the operation, whether complete
     * or not
     */
    CompletableFuture<R> result();
}
