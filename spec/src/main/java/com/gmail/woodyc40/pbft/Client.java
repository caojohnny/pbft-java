package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Reply;

public interface Client<Op, R, T> extends Node<Op, R, T> {
    long timeout();

    Roster roster();

    void sendRequest(Op operation);

    void recvReply(Reply<R> reply);
}
