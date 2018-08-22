package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.*;

public interface Encoder<Op, R, T> {
    default T encode(Message msg) {
        switch (msg.type()) {
            case REQUEST:
                return this.encodeRequest((Request<Op>) msg);
            case PRE_PREPARE:
                return this.encodePrePrepare((PrePrepare) msg);
            case PREPARE:
                return this.encodePrepare((Prepare) msg);
            case COMMIT:
                return this.encodeCommit((Commit) msg);
            case REPLY:
                return this.encodeReply((Reply<R>) msg);
            case CHECKPOINT:
                return this.encodeCheckpoint((Checkpoint) msg);
            case VIEW_CHANGE:
                return this.encodeViewChange((ViewChange) msg);
            case NEW_VIEW:
                return this.encodeNewView((NewView) msg);
            default:
                throw new IllegalArgumentException("Passed a non-message");
        }
    }

    T encodeRequest(Request<Op> request);

    T encodePrePrepare(PrePrepare prePrepare);

    T encodePrepare(Prepare prepare);

    T encodeCommit(Commit commit);

    T encodeReply(Reply<R> reply);

    T encodeCheckpoint(Checkpoint checkpoint);

    T encodeViewChange(ViewChange viewChange);

    T encodeNewView(NewView newView);

    Request<Op> decodeRequest(T encoded);

    PrePrepare decodePrePrepare(T encoded);

    Prepare decodePrepare(T encoded);

    Commit decodeCommit(T encoded);

    Reply<R> decodeReply(T encoded);

    Checkpoint decodeCheckpoint(T encoded);

    ViewChange decodeViewChange(T encoded);

    NewView decodeNewView(T encoded);
}
