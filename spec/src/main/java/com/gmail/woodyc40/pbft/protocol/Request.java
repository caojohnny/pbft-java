package com.gmail.woodyc40.pbft.protocol;

import com.gmail.woodyc40.pbft.Client;
import com.gmail.woodyc40.pbft.Message;
import com.gmail.woodyc40.pbft.MessageType;

public class Request<Op, R, T> implements Message {
    private static final MessageType TYPE = MessageType.REQUEST;

    private final Op operation;
    private final long timestamp;
    private final Client<Op, R, T> client;

    public Request(Op operation, long timestamp, Client<Op, R, T> client) {
        this.operation = operation;
        this.timestamp = timestamp;
        this.client = client;
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    public Op operation() {
        return this.operation;
    }

    public long timestamp() {
        return this.timestamp;
    }

    public Client<Op, R, T> client() {
        return this.client;
    }
}
