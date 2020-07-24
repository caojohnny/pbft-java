package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ReplicaViewChange;

/**
 * Represents the result of accepting a
 * {@link ReplicaViewChange}. This allows the replica to
 * respond to the message based on the state of the message
 * log.
 */
public interface ReplicaViewChangeResult {
    /**
     * Determines whether the replica should bandwagon as a
     * result of receiving an additional view change vote.
     *
     * @return {@code true} if bandwagoning should occur
     */
    boolean shouldBandwagon();

    /**
     * Determines the smallest view number that has been
     * voted for this replica to bandwagon. Only returns a
     * valid result if {@link #shouldBandwagon()} returns
     * {@code true}.
     *
     * @return the view number to bandwagon
     */
    int bandwagonViewNumber();

    /**
     * Determines whether the timer to vote to the next
     * view should be started as a result of receiving
     * {@code 2f + 1} votes for the current view change.
     *
     * @return {@code true} if the timer to v+2 should be
     * started if the vote to v+1 is received
     */
    boolean beginNextVoteTimer();
}
