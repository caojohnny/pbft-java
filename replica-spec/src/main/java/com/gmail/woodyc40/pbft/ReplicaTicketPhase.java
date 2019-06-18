package com.gmail.woodyc40.pbft;

/**
 * Represents the current phase that a ticket currently
 * has.
 */
public enum ReplicaTicketPhase {
    /**
     * Represents the initial phase of the ticket.
     */
    PRE_PREPARE,
    /**
     * Represents the phase of the ticket after it the
     * {@code prepared} condition becomes {@code true}
     */
    PREPARE,
    /**
     * Represents the phase of the ticket after it the
     * {@code committed} condition becomes {@code true}
     */
    COMMIT
}
