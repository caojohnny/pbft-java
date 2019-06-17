package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientReply;
import com.gmail.woodyc40.pbft.message.ClientRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract representation of a <em>client</em> that
 * sends requests of type {@code O} and retrieves a result
 * of type {@code R}.
 *
 * @param <O> the operation type
 * @param <R> the result type
 */
public interface Client<O, R> {
    /**
     * The configured maximum failure tolerance, or the
     * maximum number of faulty nodes that the client is
     * configured to work with.
     *
     * @return the max number of faulty nodes, {@code f}
     */
    int tolerance();

    /**
     * Obtains the number of milliseconds that elapses
     * before a timeout occurs, in which case a pending
     * request will multicast the request to all replicas
     * and reset the timer.
     *
     * @return the timeout millis
     */
    long timeoutMs();

    /**
     * Sends a request for the given {@code request} to be
     * completed by replica state machines.
     *
     * <p>Users should call {@link #checkTimeout(ClientTicket)}
     * in a loop after calling this method in order to
     * ensure that the operation is served.</p>
     *
     * @param operation the operation to be fulfilled
     * @return the ticket representing the successful
     * dispatch of the {@code request}
     */
    ClientTicket<O, R> sendRequest(O operation);

    /**
     * Checks on the completion state of the ticket against
     * the timeout specified by the client.
     *
     * <p>Users should run this in a loop to enusre that a
     * request is served in a timely manner.</p>
     *
     * @return {@code true} if a timeout occurred and the
     * request has been re-multicast to the replicas
     */
    boolean checkTimeout(ClientTicket<O, R> ticket);

    /**
     * Called by a {@link ClientTransport} implementor when the
     * {@link ClientReply} message is receieved from a replica.
     *
     * @param reply the reply that is received
     * @return the {@link ClientTicket} that was dispatched which
     * lead to the computation resulting in the given
     * {@link ClientReply}, or {@code null} if the reply has no
     * corresponding {@link ClientTicket} stored in this client
     */
    @Nullable
    ClientTicket<O, R> recvReply(ClientReply<R> reply);

    /**
     * Obtains the {@link ClientCodec} type used by this client
     * to encode {@link ClientRequest} messages.
     *
     * @param <T> the transmissible type specified by the
     *            {@link ClientCodec}
     * @return the {@link ClientCodec} used by this {@link Client}
     */
    <T> ClientCodec<T> codec();

    /**
     * Obtains the {@link ClientTransport} type used by this
     * client to send messages.
     *
     * @param <T> the type of transmissible data type used
     *            by the {@link ClientTransport}
     * @return the {@link ClientTransport} used by this
     * {@link Client}
     */
    <T> ClientTransport<T> transport();
}
