package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Reply;
import com.gmail.woodyc40.pbft.message.Request;

/**
 * A component that transforms the contents of a message
 * into a format transmissible through a {@link Transport}.
 *
 * <p>A {@link Codec} should be capable of handling message
 * cryptography, e.g. digesting, signing, and generating
 * MACs as it sees fit.</p>
 *
 * @param <T> the type of transmissible format that
 *            represents different messages
 */
public interface Codec<T> {
    /**
     * Encodes the given request into a transmissible
     * format specified by the class parameter.
     *
     * @param request the request to encode
     * @param <O>     the type of operation
     * @return the encoded request
     */
    <O> T encodeRequest(Request<O> request);

    /**
     * Decodes the {@link Transport} data into a
     * {@link Reply}.
     *
     * @param data the data that represents the encoded
     *             reply
     * @param <R>  the type of reply value
     * @return the decoded reply
     */
    <R> Reply<R> decodeReply(T data);
}
