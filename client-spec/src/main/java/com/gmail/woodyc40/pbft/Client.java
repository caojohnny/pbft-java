package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Reply;
import com.gmail.woodyc40.pbft.message.Request;
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
     * Sends a request for the given {@code request} to be
     * completed by replica state machines.
     *
     * @param request the operation to be fulfilled
     * @return the ticket representing the successful
     * dispatch of the {@code request}
     */
    Ticket<O, R> sendRequest(O request);

    /**
     * Called by a {@link Transport} implementor when the
     * {@link Reply} message is receieved from a replica.
     *
     * @param reply the reply that is received
     * @return the {@link Ticket} that was dispatched which
     * lead to the computation resulting in the given
     * {@link Reply}, or {@code null} if the reply has no
     * corresponding {@link Ticket} stored in this client
     */
    @Nullable
    Ticket<O, R> recvReply(Reply<R> reply);

    /**
     * Obtains the {@link Codec} type used by this client
     * to encode {@link Request} messages.
     *
     * @param <T> the transmissible type specified by the
     *            {@link Codec}
     * @return the {@link Codec} used by this {@link Client}
     */
    <T> Codec<T> codec();

    /**
     * Obtains the {@link Transport} type used by this
     * client to send messages.
     *
     * @param <T> the type of transmissible data type used
     *            by the {@link Transport}
     * @return the {@link Transport} used by this
     * {@link Client}
     */
    <T> Transport<T> transport();
}
