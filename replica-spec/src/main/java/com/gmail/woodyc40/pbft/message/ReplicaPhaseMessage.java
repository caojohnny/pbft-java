package com.gmail.woodyc40.pbft.message;

/**
 * Represents a message abstraction for the 3 messages
 * controlling the replica phases, {@code PRE-PREPARE},
 * {@code PREPARE}, and {@code COMMIT}.
 */
public interface ReplicaPhaseMessage {
    /**
     * Obtains the view number as seen by the primary
     * when the request message arrived.
     *
     * @return the view number
     */
    int viewNumber();

    /**
     * Obtains the sequence number assigned by the primary
     * to the request computation.
     *
     * @return the sequence number
     */
    long seqNumber();

    /**
     * The digest for the {@link ReplicaRequest} which prompted
     * the computation to occur.
     *
     * @return the message digest
     */
    byte[] digest();
}
