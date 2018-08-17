package com.gmail.woodyc40.pbft.protocol;

import com.gmail.woodyc40.pbft.Client;
import com.gmail.woodyc40.pbft.Message;
import com.gmail.woodyc40.pbft.MessageType;

public class Reply<R> implements Message {
    private static final MessageType TYPE = MessageType.REPLY;

    private final int viewNumber;
    private final long timestamp;
    private final Client client;
    private final int replicaNumber;
    private final R response;

    public Reply(int viewNumber, long timestamp, Client client, int replicaNumber, R response) {
        this.viewNumber = viewNumber;
        this.timestamp = timestamp;
        this.client = client;
        this.replicaNumber = replicaNumber;
        this.response = response;
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    public int viewNumber() {
        return this.viewNumber;
    }

    public long timestamp() {
        return this.timestamp;
    }

    public Client client() {
        return this.client;
    }

    public int replicaNumber() {
        return this.replicaNumber;
    }

    public R response() {
        return this.response;
    }
}
