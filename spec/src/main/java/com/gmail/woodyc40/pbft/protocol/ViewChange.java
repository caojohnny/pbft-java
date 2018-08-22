package com.gmail.woodyc40.pbft.protocol;

import com.gmail.woodyc40.pbft.Message;
import com.gmail.woodyc40.pbft.MessageType;

import java.util.Collection;

public class ViewChange implements Message {
    private static final MessageType TYPE = MessageType.VIEW_CHANGE;

    private final int newView;
    private final int seqNumber;
    private final Collection<Message> correctness;
    private final Collection<Message> higherPrep;
    private final String id;

    public ViewChange(int newView, int seqNumber,
                      Collection<Message> correctness,
                      Collection<Message> higherPrep,
                      String id) {

        this.newView = newView;
        this.seqNumber = seqNumber;
        this.correctness = correctness;
        this.higherPrep = higherPrep;
        this.id = id;
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    public int newView() {
        return this.newView;
    }

    public int seqNumber() {
        return this.seqNumber;
    }

    public Collection<Message> correctnessMessages() {
        return this.correctness;
    }

    public Collection<Message> higherPrepMessages() {
        return this.higherPrep;
    }

    public String id() {
        return this.id;
    }
}
