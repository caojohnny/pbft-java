package com.gmail.woodyc40.pbft.spec;

import com.gmail.woodyc40.pbft.spec.protocol.*;
import com.gmail.woodyc40.pbftjava.spec.protocol.*;

public interface StateMachine<Op, R> {
    int id();

    int tolerance();

    Digester digester();

    MessageEncoder encoder();

    void primaryRecvReq(Request<Op> request);

    void relayRequest(Request<Op> request);

    void sendPrePrepare(byte[] requestDigest);

    void recvPrePrepare(PrePrepare prePrepare);

    void sendPrepare(Prepare prepare);

    void recvPrepare(Prepare prepare);

    boolean prepared(int replica);

    void sendCommit(Commit commit);

    void recvCommit(Commit commit);

    boolean commitedLocal(int replica);

    void sendReply(Reply<R> reply);
}
