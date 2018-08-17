package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Reply;

public interface Client<Op, R> extends Node {
    long timeout();

    void sendRequest(Op operation);

    void recvReply(Reply<R> reply);
}
