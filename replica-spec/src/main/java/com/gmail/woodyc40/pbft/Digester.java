package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

/**
 * Represents a component which provides digesting
 * capability for {@link Replica}s receving requests.
 *
 * @param <O> the operation type which to digest
 */
public interface Digester<O> {
    /**
     * Produces a digest of the given request message.
     *
     * @param request the request to digest
     * @return the digest as a byte array
     */
    byte[] digest(Request<O> request);
}
