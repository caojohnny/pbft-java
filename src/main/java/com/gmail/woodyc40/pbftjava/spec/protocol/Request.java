package com.gmail.woodyc40.pbftjava.spec.protocol;

import com.gmail.woodyc40.pbftjava.spec.Client;
import com.gmail.woodyc40.pbftjava.spec.Message;
import com.gmail.woodyc40.pbftjava.spec.MessageType;

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

    public Op getOperation() {
        return this.operation;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public Client getClient() {
        return this.client;
    }
}
