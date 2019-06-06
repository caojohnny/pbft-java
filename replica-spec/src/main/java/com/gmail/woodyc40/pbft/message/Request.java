package com.gmail.woodyc40.pbft.message;

/**
 * Represents a request sent by a client in order to
 * initiate a service from the replica.
 *
 * @param <O> the operation type
 */
public interface Request<O> {
    /**
     * Obtains the operation serialized by the request.
     *
     * @return the operation
     */
    O operation();

    /**
     * Obtains a unique timestamp value used for total
     * ordering of service requests.
     *
     * @return the timestamp encoded by the client
     */
    long timestamp();

    /**
     * Obtains an identifier for whichever client sent this
     * {@link Request}.
     *
     * @return the client ID value
     */
    String clientId();
}
