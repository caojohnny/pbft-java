package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ClientRequest;

import java.util.stream.IntStream;

/**
 * Represents the link from a {@link Client} to the replicas
 * which will dispatch {@link ClientRequest}s.
 *
 * @param <T> the type of transmissible data that represents
 *            encoded messages
 */
public interface ClientTransport<T> {
    /**
     * Obtains an {@link IntStream} populated with the known
     * replica ID numbers.
     *
     * @return the known replica IDs
     */
    IntStream knownReplicaIds();

    /**
     * Obtains the number of replicas that are currently
     * known to this client.
     *
     * @return the number of replicas available
     */
    int countKnownReplicas();

    /**
     * Sends the given encoded {@link ClientRequest} message to
     * the replica that has the given ID number.
     *
     * @param replicaId the ID number of the replica
     * @param request   the encoded request message
     */
    void sendRequest(int replicaId, T request);

    /**
     * Sends the given encoded {@link ClientRequest} to all known
     * replicas.
     *
     * @param request the encoded request message
     */
    void multicastRequest(T request);
}
