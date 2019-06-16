package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Transport;

/**
 * Represents a message abstraction for the 3 messages
 * controlling the replica phases, {@code PRE-PREPARE},
 * {@code PREPARE}, and {@code COMMIT}.
 */
public interface PhaseMessage {
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
     * The digest for the {@link Request} which prompted
     * the computation to occur.
     *
     * @return the message digest
     */
    byte[] digest();
}
