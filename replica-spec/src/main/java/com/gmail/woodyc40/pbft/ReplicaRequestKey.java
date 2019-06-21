package com.gmail.woodyc40.pbft;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a unique identifying key for each request
 * that a client sends to a replica.
 */
public interface ReplicaRequestKey {
    /**
     * The client where the request originated from.
     *
     * @return the client ID
     */
    @NonNull
    String clientId();

    /**
     * The timestamp when the client created the request.
     *
     * @return the request timestamp
     */
    long timestamp();
}
