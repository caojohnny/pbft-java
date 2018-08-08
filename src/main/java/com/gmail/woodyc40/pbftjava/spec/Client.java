package com.gmail.woodyc40.pbftjava.spec;

import com.gmail.woodyc40.pbftjava.spec.protocol.Reply;

public interface Client<Op, R> {
    long timeout();

    int tolerance();

    void sendRequest(Op operation);

    void recvReply(Reply<R> reply);
}
