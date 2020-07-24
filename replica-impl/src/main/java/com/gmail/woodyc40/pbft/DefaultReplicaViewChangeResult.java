package com.gmail.woodyc40.pbft;

public class DefaultReplicaViewChangeResult implements ReplicaViewChangeResult {
    private final boolean shouldBandwagon;
    private final int bandwagonViewNumber;
    private final boolean beginNextVote;

    public DefaultReplicaViewChangeResult(boolean shouldBandwagon,
                                          int bandwagonViewNumber,
                                          boolean beginNextVote) {
        this.shouldBandwagon = shouldBandwagon;
        this.bandwagonViewNumber = bandwagonViewNumber;
        this.beginNextVote = beginNextVote;
    }

    @Override
    public boolean shouldBandwagon() {
        return this.shouldBandwagon;
    }

    @Override
    public int bandwagonViewNumber() {
        return this.bandwagonViewNumber;
    }

    @Override
    public boolean beginNextVoteTimer() {
        return this.beginNextVote;
    }
}
