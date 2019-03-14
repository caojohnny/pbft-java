package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Checkpoint;
import com.gmail.woodyc40.pbft.protocol.Commit;
import com.gmail.woodyc40.pbft.protocol.NewView;
import com.gmail.woodyc40.pbft.protocol.PrePrepare;
import com.gmail.woodyc40.pbft.protocol.Prepare;
import com.gmail.woodyc40.pbft.protocol.Reply;
import com.gmail.woodyc40.pbft.protocol.Request;
import com.gmail.woodyc40.pbft.protocol.ViewChange;

import java.util.concurrent.atomic.AtomicInteger;

// TODO: Implement
public class DefaultStateMachine<Op, R, T>
        extends AbstractStateMachine<Op, R, T> {
    private final AtomicInteger seqCounter = new AtomicInteger();

    protected DefaultStateMachine(int id,
                                  Verifier<Op> verifier,
                                  NodeOptions<Op, R, T> options) {
        super(id, verifier, options);
    }

    @Override
    public void primaryRecvReq(Request<Op, R, T> request) {
        if (this.id() != this.transport().primaryId()) {
            return;
        }

        // TODO: Check progress of other requests

        if (!this.verifier().verifyRequest(request)) {
            return;
        }

        PrePrepare prePrepare = new PrePrepare(this.id(), this.seqCounter.getAndIncrement(), this.digester().digest(request));
        T encodedPrePrepare = this.encoder().encode(prePrepare);
        T encodedMsg = this.encoder().encode(request);

        this.transport().multicast(this.roster(), encodedPrePrepare);
        this.transport().multicast(this.roster(), encodedMsg);
        this.state(State.PREPARE);
    }

    @Override
    public void sendPrePrepare(Request<Op, R, T> request) {
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
    public void sendReply(Reply<R> reply) {
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
