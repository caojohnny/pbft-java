package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Request;

import java.util.stream.IntStream;

/**
 * The transport used to control messaging between replicas
 * as well as clients.
 *
 * @param <T> the encoded message type
 */
public interface Transport<T> {
    /**
     * Obtains the current view number, used to determine
     * the primary ID number.
     *
     * @return the current view number
     */
    int viewNumber();

    /**
     * Obtains the number of replicas known by the
     * transport layer.
     *
     * @return the known number of replicas
     */
    int countKnownReplicas();

    /**
     * Obtains an {@link IntStream} populated with the
     * known replica ID numbers.
     *
     * @return a stream of known replica IDs
     */
    IntStream knownReplicaIds();

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

    /**
     * Multicasts a PBFT {@code PRE-PREPARE} message in
     * response to a request.
     *
     * @param prePrepare the encoded preprepare message
     */
    void multicastPrePrepare(T prePrepare);

    /**
     * Called by the transmission layer to notify the
     * {@link Replica} that a pre-prepare message has
     * arrived from the primary.
     *
     * @param prePrepare the encoded pre-prepare
     *                   message
     */
    void recvPrePrepare(T prePrepare);

    /**
     * Multicasts a PBFT {@code PREPARE} message in
     * response to a pre-prepare message.
     *
     * @param prepare the encoded prepare message
     */
    void multicastPrepare(T prepare);

    /**
     * Called by the transmission layer to notify the
     * {@link Replica} that a prepare message has arrived
     * from another replica.
     *
     * @param prepare the encoded prepare message
     */
    void recvPrepare(T prepare);

    /**
     * Multicasts a PBFT {@code COMMIT} message in response
     * to a prepare message.
     *
     * @param commit the encoded commit message
     */
    void multicastCommit(T commit);

    /**
     * Called by the transmission layer to notify the
     * {@link Replica} that a commit message has arrived
     * from another replica.
     *
     * @param commit the encoded commit message
     */
    void recvCommit(T commit);

    /**
     * Sends a reply message to the client with the given
     * client ID String.
     *
     * @param clientId the client ID String
     * @param reply the encoded reply message
     */
    void sendReply(String clientId, T reply);
}
