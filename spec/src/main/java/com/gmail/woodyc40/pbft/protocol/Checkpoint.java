package com.gmail.woodyc40.pbft.protocol;

import com.gmail.woodyc40.pbft.Message;
import com.gmail.woodyc40.pbft.MessageType;
import com.gmail.woodyc40.pbft.State;

public class Checkpoint implements Message {
    private static final MessageType TYPE = MessageType.CHECKPOINT;

    private final int seqNumber;
    private final State state;
    private final String id;

    public Checkpoint(int seqNumber, State state, String id) {
        this.seqNumber = seqNumber;
        this.state = state;
        this.id = id;
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    public int seqNumber() {
        return this.seqNumber;
    }

    public State state() {
        return this.state;
    }

    public String id() {
        return this.id;
    }
}
