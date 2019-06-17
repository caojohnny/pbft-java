package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

/**
 * A component that transforms messages to and from a
 * format that is transmisisble using a
 * {@link ReplicaTransport}.
 *
 * <p>A {@link ReplicaCodec} should be capable of handling message
 * cryptography, i.e. signing, and generating MACs as it
 * sees fit.</p>
 *
 * @param <T> the transmissible type
 */
public interface ReplicaCodec<T> {
    /**
     * Decodes a request, usually either from a client or
     * from a replica redirect.
     *
     * @param request the request data to decode
     * @param <O> the request operation type
     * @return the decoded request message
     */
    <O> ReplicaRequest<O> decodeRequest(T request);

    /**
     * Encodes a request message, used for redirects.
     *
     * @param request the request to redirect
     * @param <O> the request operation type
     * @return the encoded request message
     */
    <O> T encodeRequest(ReplicaRequest<O> request);

    /**
     * Decodes a pre-prepare message, sent the primary once
     * it has received a request.
     *
     * @param prePrepare the encoded pre-prepare message
     * @param <O> the requested operation type
     * @return the decoded pre-prepare message
     */
    <O> ReplicaPrePrepare<O> decodePrePrepare(T prePrepare);

    /**
     * Encodes a pre-prepare message to multicast to
     * non-primaries once a request has been received.
     *
     * @param prePrepare the pre-prepare message to encode
     * @param <O> the requested operation type
     * @return the encoded pre-prepare message
     */
    <O> T encodePrePrepare(ReplicaPrePrepare<O> prePrepare);

    /**
     * Decodes a prepare message sent by other replicas
     * upon switching to the prepared phase.
     *
     * @param prepare the encoded prepare message
     * @return the decoded prepare message
     */
    ReplicaPrepare decodePrepare(T prepare);

    /**
     * Encodes a prepare message to indidcate that this
     * replica has entered the prepared phase.
     *
     * @param prepare the prepare message
     * @return the encoded prepare message
     */
    T encodePrepare(ReplicaPrepare prepare);

    /**
     * Decodes a commit message sent to indicate that other
     * replicas have received all of their prepare
     * messages.
     *
     * @param commit the encoded commit message
     * @return the decoded commit message
     */
    ReplicaCommit decodeCommit(T commit);

    /**
     * Encodes the commit message to indicate that this
     * replica has received all of the necessary prepare
     * messages to proceed with computation.
     *
     * @param commit the commit message
     * @return the encoded commit message
     */
    T encodeCommit(ReplicaCommit commit);

    /**
     * Encodes the given reply to notify the client of the
     * result of the requested computation.
     *
     * @param reply the reply
     * @param <R> the result type
     * @return the encoded reply message
     */
    <R> T encodeReply(ReplicaReply<R> reply);
}