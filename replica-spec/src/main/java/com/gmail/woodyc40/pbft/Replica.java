package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

/**
 * Represents a replicated state-machine in the PBFT
 * algorithm.
 *
 * <p>Users are required to call the following methods:
 *   - {@link #recvRequest(ReplicaRequest)}
 *   - {@link #recvPrePrepare(ReplicaPrePrepare)}
 *   - {@link #recvRequest(ReplicaRequest)}
 *   - {@link #recvCommit(ReplicaCommit)}</p>
 *
 * @param <O> the operation type
 * @param <R> the result type of the operation
 * @param <T> the transmissible type
 */
public interface Replica<O, R, T> {
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
    ReplicaMessageLog log();

    /**
     * Called by the replica {@link ReplicaTransport} to indicate
     * that a PBFT {@code REQUEST} has been received.
     *
     * @param request the received request message
     */
    void recvRequest(ReplicaRequest<O> request);

    /**
     * Used by a non-primary to redirect a PBFT
     * {@code REQUEST} to the actual primary in the case
     * that the client mistakenly sends.
     *
     * @param replicaId the primary replica ID
     * @param request the request to redirect
     */
    void sendRequest(int replicaId, ReplicaRequest<O> request);

    /**
     * Used by the primary replica to send a PBFT
     * {@code PRE-PREPARE} message to the other replicas.
     *
     * @param prePrepare the message to send
     */
    void sendPrePrepare(ReplicaPrePrepare<O> prePrepare);

    /**
     * Called by the replica {@link ReplicaTransport} to indicate
     * that a PBFT {@code PRE-PREPARE} message has been
     * multicasted by the primary to this replica.
     *
     * @param prePrepare the message received
     */
    void recvPrePrepare(ReplicaPrePrepare<O> prePrepare);

    /**
     * Used by each replica to indicate that a PBFT
     * {@code PREPARE} message has been received.
     *
     * @param prepare the message to send
     */
    void sendPrepare(ReplicaPrepare prepare);

    /**
     * Called by the replica {@link ReplicaTransport} to indicate
     * that a PBFT {@code PREPARE} message has been
     * received from another replica.
     *
     * @param prepare the message received
     */
    void recvPrepare(ReplicaPrepare prepare);

    /**
     * Used by each replica to indicate that the prepared
     * phase has been reached and to notify other replicas
     * with the PBFT {@code COMMIT} message.
     *
     * @param commit the message to send
     */
    void sendCommit(ReplicaCommit commit);

    /**
     * Called by the replica {@link ReplicaTransport} to indicate
     * that the source replica has entered the prepared
     * phase and that a PBFT {@code COMMIT} message was
     * received by this replica as a result.
     *
     * @param commit the message received
     */
    void recvCommit(ReplicaCommit commit);

    /**
     * Used by each replica to send a PBFT {@code REPLY}
     * message back to the client in order to notify it
     * of the requested operation result.
     *
     * @param clientId the target client ID String
     * @param reply the message to send
     */
    void sendReply(String clientId, ReplicaReply<R> reply);

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
     * Obtains the encoder component used to encode and
     * decode messages for use with the {@link ReplicaTransport}
     * layer.
     *
     * @return the encoder used by this replica
     */
    ReplicaEncoder<O, R, T> encoder();

    /**
     * Obtains the digester component used to verify
     * request integrity.
     *
     * @return the digester component
     */
    ReplicaDigester<O> digester();

    /**
     * The type of transport used for communication between
     * replicas as well as clients.
     *
     * @return the transport used by this replica
     */
    ReplicaTransport<T> transport();
}
