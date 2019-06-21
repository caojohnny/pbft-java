package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;

import java.util.Collection;

/**
 * Represents a replicated state-machine in the PBFT
 * algorithm.
 *
 * <p>Users are required to call the following methods:
 * - {@link #recvRequest(ReplicaRequest)}
 * - {@link #recvPrePrepare(ReplicaPrePrepare)}
 * - {@link #recvPrepare(ReplicaPrepare)}
 * - {@link #recvCommit(ReplicaCommit)}
 * - {@link #recvCheckpoint(ReplicaCheckpoint)}
 * - {@link #recvViewChange(ReplicaViewChange)}
 * - {@link #recvNewView(ReplicaNewView)}
 *
 * In addition, users are also expected to call
 * {@link #checkTimeout(ReplicaRequestKey)} in a loop in
 * order to maintain liveness.</p>
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
     * The number of milliseconds that elapses from when
     * a request is received by this client and a timeout
     * occurs to send a view change vote.
     *
     * @return the number of milliseconds to acheive
     * a timeout
     */
    long timeoutMs();

    /**
     * Obtains this {@link Replica}'s message log as
     * specified in the PBFT algorithm;
     *
     * @return the message log
     */
    ReplicaMessageLog log();

    /**
     * Sets the new view number upon the acceptance of a
     * {@link ReplicaNewView} message.
     *
     * @param newViewNumber the new view to enter into
     */
    void setViewNumber(int newViewNumber);

    /**
     * Obtains the current view number, used to determine
     * the primary ID number.
     *
     * @return the current view number
     */
    int viewNumber();

    /**
     * Sets whether this replica should pause receiving any
     * message except for:
     *   - {@link ReplicaViewChange}
     *   - {@link ReplicaNewView}
     *   - {@link ReplicaCheckpoint}
     * in preparation for a view change.
     *
     * @param disgruntled {@code true} if this replica
     *                    should pause to wait for a view
     *                    change
     */
    void setDisgruntled(boolean disgruntled);

    /**
     * Checks to see if this replica is currently
     * "disgurntled" and has sent a vote to change view.
     *
     * @return {@code true} if this replica has paused
     * waiting for a view change to occur
     */
    boolean isDisgruntled();

    /**
     * Obtains a collection of requests that have active
     * timers started.
     *
     * @return a collection of requests that have started
     * timeout timers
     */
    Collection<ReplicaRequestKey> activeTimers();

    /**
     * Checks to see whether the given active timer
     * represented by the request key has timed out,
     * appropriately sending a view change message if so.
     *
     * <p>A timeout occurs when {@link #isDisgruntled()}
     * changes from {@code false} to {@code true} after
     * calling this method.</p>
     *
     * @param key the request to check
     * @return the time to wait
     */
    long checkTimeout(ReplicaRequestKey key);

    /**
     * Called by the replica user to indicate
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
     * @param request   the request to redirect
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
     * Called by the replica user to indicate
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
     * Called by the replica user to indicate
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
     * Called by the user to indicate
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
     * @param reply    the message to send
     */
    void sendReply(String clientId, ReplicaReply<R> reply);

    /**
     * Called by the replica user to indicate that a PBFT
     * {@code CHECKPOINT} message has been received.
     *
     * @param checkpoint the message
     */
    void recvCheckpoint(ReplicaCheckpoint checkpoint);

    /**
     * Sends a PBFT {@code CHECKPONT} message to other
     * replicas to notify them that this replica has
     * reached a stable checkpoint.
     *
     * @param checkpoint the message to send
     */
    void sendCheckpoint(ReplicaCheckpoint checkpoint);

    /**
     * Called by users to indicate that a PBFT
     * {@code VIEW-CHANGE} message has been received.
     *
     * @param viewChange the message
     */
    void recvViewChange(ReplicaViewChange viewChange);

    /**
     * Sends a PBFT {@code VIEW-CHANGE} to all replicas
     * upon operation timeout.
     *
     * @param viewChange the message to send
     */
    void sendViewChange(ReplicaViewChange viewChange);

    /**
     * Called by users to indicate that a PBFT
     * {@code NEW-VIEW} message has been received and
     * the replica should switch to a new view.
     *
     * @param newView
     */
    void recvNewView(ReplicaNewView newView);

    /**
     * Sends a PBFT {@code NEW-VIEW} message to indicate
     * that a consensus has been reached to switch to a
     * new view.
     *
     * @param newView the message
     */
    void sendNewView(ReplicaNewView newView);

    /**
     * Produces a digest of the current replica state in
     * order for other replicas to verify its status.
     *
     * @return the digest of the currentstate
     */
    byte[] digestState();

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
