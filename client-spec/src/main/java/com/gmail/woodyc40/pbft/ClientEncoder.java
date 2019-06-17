package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientRequest;

/**
 * A component that transforms the contents of a message
 * into a format transmissible through a {@link ClientTransport}.
 *
 * <p>A {@link ClientEncoder} should be capable of handling message
 * cryptography, i.e. signing, and generating MACs as it
 * sees fit.</p>
 *
 * @param <O> the type of operation this codec handles
 * @param <T> the type of transmissible format that
 *            represents different messages
 */
public interface ClientEncoder<O, T> {
    /**
     * Encodes the given request into a transmissible
     * format specified by the class parameter.
     *
     * @param request the request to encode
     * @return the encoded request
     */
    T encodeRequest(ClientRequest<O> request);
}
