package com.gmail.woodyc40.pbft.message;

import com.gmail.woodyc40.pbft.Replica;
import com.gmail.woodyc40.pbft.Transport;

/**
 * Represents a PBFT {@code PREPARE} message sent in
 * response to a {@link PrePrepare} message being
 * received.
 */
public interface Prepare {
    /**
     * Obtains the view number read from the replica
     * multicasting this message obtained from
     * {@link Transport#viewNumber()}.
     *
     * @return the view number
     */
    int viewNumber();

    /**
     * Obtains the sequence number assigned by the primary
     * to the {@link PrePrepare} message that prompted this
     * message.
     *
     * @return the sequence number
     */
    long seqNumber();

    /**
     * The digest for the {@link Request} which prompted
     * the preceding {@link PrePrepare} message.
     *
     * @return the message digest
     */
    byte[] digest();

    /**
     * The ID number for the replica that multicasts this
     * message, obtainable through
     * {@link Replica#replicaId()}.
     *
     * @return the replica ID
     */
    int replicaId();
}
