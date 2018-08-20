package com.gmail.woodyc40.pbft.protocol;

import com.gmail.woodyc40.pbft.Message;
import com.gmail.woodyc40.pbft.MessageType;

public class Request<Op> implements Message {
    private static final MessageType TYPE = MessageType.REQUEST;

    private final Op operation;
    private final long timestamp;
    private final String clientId;

    public Request(Op operation, long timestamp, String clientId) {
        this.operation = operation;
        this.timestamp = timestamp;
        this.clientId = clientId;
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

    public String client() {
        return this.clientId;
    }
}
