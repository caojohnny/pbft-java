package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientReply;
import com.gmail.woodyc40.pbft.message.ClientRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An abstract representation of a <em>client</em> that
 * sends requests of type {@code O} and retrieves a result
 * of type {@code R}.
 *
 * <p>Users are required to call the following methods:
 *   - {@link #recvReply(ClientReply)}</p>
 *
 * @param <O> the operation type
 * @param <R> the result type
 * @param <T> the transmissible communication type
 */
public interface Client<O, R, T> {
    /**
     * While not necessarily part of the PBFT spec,
     * this is more or less of a QoL field to ensure that
     * implementors require a client ID in order to
     * allow replicas to identify the message source.
     *
     * @return the client ID value
     */
    String clientId();

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
     * Sets the primary ID based on the view number found
     * in the {@link ClientReply} message.
     *
     * @param primaryId the new primary ID
     */
    void setPrimaryId(int primaryId);

    /**
     * Obtains the ID number of what is currently believed
     * to be the primary replica.
     *
     * @return the probable primary ID
     */
    int primaryId();


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
     * Obtains the {@link ClientEncoder} type used by this client
     * to encode {@link ClientRequest} messages.
     *
     * @return the {@link ClientEncoder} used by this {@link Client}
     */
    ClientEncoder<O, T> encoder();

    /**
     * Obtains the {@link ClientTransport} type used by this
     * client to send messages.
     *
     * @return the {@link ClientTransport} used by this
     * {@link Client}
     */
    ClientTransport<T> transport();
}
