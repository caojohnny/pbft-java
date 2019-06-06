package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

/**
 * The transport used to control messaging between replicas
 * as well as clients.
 *
 * @param <T> the encoded message type
 */
public interface Transport<T> {
    /**
     * Called by the underlying layer upon receiving an
     * inbound {@link Request} message.
     *
     * @param request the encoded request
     */
    void recvRequest(T request);

    /**
     * Redirects a service request to the given replica
     * which this {@link Replica} believes to be the
     * actual primary replica.
     *
     * @param replicaId the replica to send the request
     * @param request the encoded request to send
     */
    void redirectRequest(int replicaId, T request);
}
