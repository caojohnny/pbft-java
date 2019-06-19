package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

/**
 * A component that transforms messages to and from a
 * format that is transmisisble using a
 * {@link ReplicaTransport}.
 *
 * <p>A {@link ReplicaEncoder} should be capable of handling message
 * cryptography, i.e. signing, and generating MACs as it
 * sees fit.</p>
 *
 * @param <O> the operation type handled by this encoder
 * @param <R> the response type handled by this encoder
 * @param <T> the common transmissible type
 */
public interface ReplicaEncoder<O, R, T> {
    /**
     * Encodes a request message, used for redirects.
     *
     * @param request the request to redirect
     * @return the encoded request message
     */
    T encodeRequest(ReplicaRequest<O> request);

    /**
     * Encodes a pre-prepare message to multicast to
     * non-primaries once a request has been received.
     *
     * @param prePrepare the pre-prepare message to encode
     * @return the encoded pre-prepare message
     */
    T encodePrePrepare(ReplicaPrePrepare<O> prePrepare);

    /**
     * Encodes a prepare message to indidcate that this
     * replica has entered the prepared phase.
     *
     * @param prepare the prepare message
     * @return the encoded prepare message
     */
    T encodePrepare(ReplicaPrepare prepare);

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
     * @return the encoded reply message
     */
    T encodeReply(ReplicaReply<R> reply);

    /**
     * Encodes the given checkpoint message.
     *
     * @param checkpoint the checkpoint message
     * @return the encoded checkpoint message
     */
    T encodeCheckpoint(ReplicaCheckpoint checkpoint);

    /**
     * Encodes the given view change message.
     *
     * @param viewChange the view change message
     * @return the encoded view change message
     */
    T encodeViewChange(ReplicaViewChange viewChange);

    /**
     * Encodes the new view message.
     *
     * @param newView the new view message
     * @return the encoded new view message
     */
    T encodeNewView(ReplicaNewView newView);
}
