package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

/**
 * Represents a replicated state-machine in the PBFT
 * algorithm.
 *
 * @param <O> the operation type
 * @param <R> the result type of the operation
 */
public interface Replica<O, R> {
    /**
     * The ID number of this {@link Replica}.
     *
     * @return the replica ID
     */
    int replicaId();

    /**
     * The number of faulty replicas that should be
     * accounted for by the protocol.
     *
     * @return the number of faulty nodes, {@code f}
     */
    int tolerance();

    /**
     * Obtains this {@link Replica}'s message log as
     * specified in the PBFT algorithm;
     *
     * @return the message log
     */
    MessageLog log();

    /**
     * Called by the replica {@link Transport} to indicate
     * that a PBFT {@code REQUEST} has been received.
     *
     * @param request the received request message
     */
    void recvRequest(Request<O> request);

    /**
     * Used by a non-primary to redirect a PBFT
     * {@code REQUEST} to the actual primary in the case
     * that the client mistakenly sends.
     *
     * @param replicaId the primary replica ID
     * @param request the request to redirect
     */
    void sendRequest(int replicaId, Request<O> request);

    /**
     * Used by the primary replica to send a PBFT
     * {@code PRE-PREPARE} message to the other replicas.
     *
     * @param prePrepare the message to send
     */
    void sendPrePrepare(PrePrepare<O> prePrepare);

    /**
     * Called by the replica {@link Transport} to indicate
     * that a PBFT {@code PRE-PREPARE} message has been
     * multicasted by the primary to this replica.
     *
     * @param prePrepare the message received
     */
    void recvPrePrepare(PrePrepare<O> prePrepare);

    /**
     * Used by each replica to indicate that a PBFT
     * {@code PREPARE} message has been received.
     *
     * @param prepare the message to send
     */
    void sendPrepare(Prepare prepare);

    /**
     * Called by the replica {@link Transport} to indicate
     * that a PBFT {@code PREPARE} message has been
     * received from another replica.
     *
     * @param prepare the message received
     */
    void recvPrepare(Prepare prepare);

    /**
     * Used by each replica to indicate that the prepared
     * phase has been reached and to notify other replicas
     * with the PBFT {@code COMMIT} message.
     *
     * @param commit the message to send
     */
    void sendCommit(Commit commit);

    /**
     * Called by the replica {@link Transport} to indicate
     * that the source replica has entered the prepared
     * phase and that a PBFT {@code COMMIT} message was
     * received by this replica as a result.
     *
     * @param commit the message received
     */
    void recvCommit(Commit commit);

    /**
     * Used by each replica to send a PBFT {@code REPLY}
     * message back to the client in order to notify it
     * of the requested operation result.
     *
     * @param clientId the target client ID String
     * @param reply the message to send
     */
    void sendReply(String clientId, Reply<R> reply);

    /**
     * Performs the computation signified by the object
     * which represents the operation to perform on this
     * replica.
     *
     * @param operation the operation to perform
     * @return the result of the operation
     */
    R compute(O operation);

    /**
     * Obtains the codec component used to encode and
     * decode messages for use with the {@link Transport}
     * layer.
     *
     * @param <T> the common transmissible type
     * @return the codec used by this replica
     */
    <T> Codec<T> codec();

    /**
     * Obtains the digester component used to verify
     * request integrity.
     *
     * @param <T> the operation type to be digested
     * @return the digester component
     */
    <T> Digester<T> digester();

    /**
     * The type of transport used for communication between
     * replicas as well as clients.
     *
     * @param <T> the encoded type used by the
     *            {@link Transport}
     * @return the transport used by this replica
     */
    <T> Transport<T> transport();
}
