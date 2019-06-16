package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Transport;

/**
 * Represents a PBFT {@code PRE-PREPARE} message.
 *
 * @param <O> the request operation type
 */
public interface PrePrepare<O> extends PhaseMessage {
    /**
     * The request message that prompted the primary to
     * multicast this PrePrepare message.
     *
     * @return the request message
     */
    Request<O> request();
}
