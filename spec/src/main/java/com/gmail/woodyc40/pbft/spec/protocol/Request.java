package com.gmail.woodyc40.pbft.spec.protocol;

import com.gmail.woodyc40.pbft.spec.Client;
import com.gmail.woodyc40.pbft.spec.Message;
import com.gmail.woodyc40.pbft.spec.MessageType;

public class Request<Op> implements Message {
    private static final MessageType TYPE = MessageType.REQUEST;

    private final Op operation;
    private final long timestamp;
    private final Client client;

    public Request(Op operation, long timestamp, Client client) {
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

    public Client client() {
        return this.client;
    }
}
