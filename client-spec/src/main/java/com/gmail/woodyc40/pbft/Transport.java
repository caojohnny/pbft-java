package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Reply;
import com.gmail.woodyc40.pbft.message.Request;

import java.util.stream.IntStream;

/**
 * Represents the link from a {@link Client} to the replicas
 * which will dispatch {@link Request}s and receive
 * {@link Reply}s.
 *
 * @param <T> the type of transmissible data that represents
 *            encoded messages
 */
public interface Transport<T> {
    /**
     * Obtains the ID number of what is currently believed
     * to be the primary replica.
     *
     * @return the probable primary ID
     */
    int primaryId();

    /**
     * Obtains an {@link IntStream} populated with the known
     * replica ID numbers.
     *
     * @return the known replica IDs
     */
    IntStream knownReplicaIds();

    /**
     * Sends the given encoded {@link Request} message to
     * the replica that has the given ID number.
     *
     * @param replicaId the ID number of the replica
     * @param request   the encoded request message
     * @throws Exception if an error occurs sending the
     *                   message
     */
    void sendRequest(int replicaId, T request) throws Exception;

    /**
     * Sends the given encoded {@link Request} to all known
     * replicas.
     *
     * @param request the encoded request message
     * @throws Exception if an error occurs sending the
     *                   message
     */
    void multicastRequest(T request) throws Exception;

    /**
     * Called by a user-implemented message listener which
     * will then notify the {@link Client} of a received
     * reply from a replica.
     *
     * @param reply the encoded reply message
     * @throws Exception if an error occurs processing the
     *                   message and decoding it once it has been received
     */
    void recvReply(T reply) throws Exception;
}
