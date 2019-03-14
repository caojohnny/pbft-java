package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Reply;
import com.gmail.woodyc40.pbft.protocol.Request;

public interface Client<Op, R, T> extends Node<Op, R, T> {
    Request<Op, R, T> sendRequest(Op operation);

    void recvReply(Reply<R> reply);
}
