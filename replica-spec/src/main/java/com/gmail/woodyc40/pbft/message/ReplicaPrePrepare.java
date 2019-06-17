package com.gmail.woodyc40.pbft.message;

/**
 * Represents a PBFT {@code PRE-PREPARE} message.
 *
 * @param <O> the request operation type
 */
public interface ReplicaPrePrepare<O> extends ReplicaPhaseMessage {
    /**
     * The request message that prompted the primary to
     * multicast this PrePrepare message.
     *
     * @return the request message
     */
    ReplicaRequest<O> request();
}
