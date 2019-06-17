package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientReply;
import com.gmail.woodyc40.pbft.message.ClientRequest;

/**
 * A component that transforms the contents of a message
 * into a format transmissible through a {@link ClientTransport}.
 *
 * <p>A {@link ClientCodec} should be capable of handling message
 * cryptography, i.e. signing, and generating MACs as it
 * sees fit.</p>
 *
 * @param <T> the type of transmissible format that
 *            represents different messages
 */
public interface ClientCodec<T> {
    /**
     * Encodes the given request into a transmissible
     * format specified by the class parameter.
     *
     * @param request the request to encode
     * @param <O>     the type of operation
     * @return the encoded request
     */
    <O> T encodeRequest(ClientRequest<O> request);

    /**
     * Decodes the {@link ClientTransport} data into a
     * {@link ClientReply}.
     *
     * @param data the data that represents the encoded
     *             reply
     * @param <R>  the type of reply value
     * @return the decoded reply
     */
    <R> ClientReply<R> decodeReply(T data);
}
