package com.gmail.woodyc40.pbft;

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
}
