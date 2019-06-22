package com.gmail.woodyc40.pbft.message;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a request sent by a client in order to
 * initiate a service from the replica.
 *
 * @param <O> the operation type
 */
public interface ReplicaRequest<O> {
    /**
     * Obtains the operation serialized by the request.
     *
     * @return the operation
     */
    @Nullable
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
     * {@link ReplicaRequest}.
     *
     * @return the client ID value
     */
    String clientId();
}
