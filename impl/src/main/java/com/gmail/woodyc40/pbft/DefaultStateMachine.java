package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.*;

// TODO: Implement
public class DefaultStateMachine<Op, R, T>
        extends AbstractStateMachine<Op, T, R> {
    protected DefaultStateMachine(int id,
                                  Verifier<Op> verifier,
                                  NodeOptions<Op, T, R> options) {
        super(id, verifier, options);
    }

    @Override
    public void primaryRecvReq(Request<Op> request) {
        if (this.id() != this.transport().primaryId()) {
            return;
        }

        // TODO: Check progress of other requests

        if (!this.verifier().verifyRequest(request)) {
            return;
        }

        R encodedMsg = this.encoder().encode(request);
        this.transport().multicast(this.roster(), encodedMsg);
        this.state(State.PREPARE);
    }

    @Override
    public void sendPrePrepare(Request<Op> request) {
    }

    @Override
    public void recvPrePrepare(PrePrepare prePrepare) {
    }

    @Override
    public void sendPrepare(Prepare prepare) {
    }

    @Override
    public void recvPrepare(Prepare prepare) {
    }

    @Override
    public boolean prepared(int replica) {
        return false;
    }

    @Override
    public void sendCommit(Commit commit) {
    }

    @Override
    public void recvCommit(Commit commit) {
    }

    @Override
    public boolean commitedLocal(int replica) {
        return false;
    }

    @Override
    public void sendReply(Reply<T> reply) {
    }

    @Override
    public void sendCheckpoint(Checkpoint checkpoint) {
    }

    @Override
    public void recvCheckpoint(Checkpoint checkpoint) {
    }

    @Override
    public void sendViewChange(ViewChange viewChange) {
    }

    @Override
    public void recvViewChange(ViewChange viewChange) {
    }

    @Override
    public void sendNewView(NewView newView) {
    }

    @Override
    public void recvNewView(NewView newView) {
    }
}
