package com.gmail.woodyc40.pbft;

import java.util.stream.IntStream;

/**
 * The transport used to control messaging between replicas
 * as well as clients.
 *
 * @param <T> the encoded message type
 */
public interface ReplicaTransport<T> {
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
     * Redirects a service request to the given replica
     * which this {@link Replica} believes to be the
     * actual primary replica.
     *
     * @param replicaId the replica to send the request
     * @param data the encoded request to send
     */
    void sendMessage(int replicaId, T data);

    /**
     * Multicasts a PBFT {@code PRE-PREPARE} message in
     * response to a request.
     *
     * @param data the encoded preprepare message
     * @param ignoredReplicas the replicas to ignored in
     *                        the multicast operation
     */
    void multicast(T data, int... ignoredReplicas);

    /**
     * Sends a reply message to the client with the given
     * client ID String.
     *
     * @param clientId the client ID String
     * @param reply the encoded reply message
     */
    void sendReply(String clientId, T reply);
}
