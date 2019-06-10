package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Transport;

/**
 * Represents a PBFT {@code PRE-PREPARE} message.
 *
 * @param <O> the request operation type
 */
public interface PrePrepare<O> {
    /**
     * The current view number when this message is
     * multicasted by the primary, obtainable through
     * {@link Transport#viewNumber()}.
     *
     * @return the view number
     */
    int viewNumber();

    /**
     * Represents sequence number assigned by the replica
     * at the time this message is multicasted.
     *
     * @return the sequence number
     */
    long seqNumber();

    /**
     * Obtains the {@link Request} message digest.
     *
     * @return the message digest
     */
    byte[] digest();

    /**
     * The request message that prompted the primary to
     * multicast this PrePrepare message.
     *
     * @return the request message
     */
    Request<O> request();
}
