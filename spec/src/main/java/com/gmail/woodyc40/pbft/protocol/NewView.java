package com.gmail.woodyc40.pbft.protocol;

import com.gmail.woodyc40.pbft.Message;
import com.gmail.woodyc40.pbft.MessageType;

import java.util.Collection;

public class NewView implements Message {
    private static final MessageType TYPE = MessageType.NEW_VIEW;

    private final int newView;
    private final Collection<Message> viewChange;
    private final Collection<Message> prePrep;

    public NewView(int newView, Collection<Message> viewChange,
                   Collection<Message> prePrep) {
        this.newView = newView;
        this.viewChange = viewChange;
        this.prePrep = prePrep;
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    public int newView() {
        return newView;
    }

    public Collection<Message> viewChangeMessages() {
        return viewChange;
    }

    public Collection<Message> prePrepMessages() {
        return prePrep;
    }
}
