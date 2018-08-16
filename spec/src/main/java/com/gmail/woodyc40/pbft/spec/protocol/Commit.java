package com.gmail.woodyc40.pbft.spec.protocol;

import com.gmail.woodyc40.pbft.spec.Message;
import com.gmail.woodyc40.pbft.spec.MessageType;

public class Commit implements Message {
    private static final MessageType TYPE = MessageType.COMMIT;

    private final int viewNumber;
    private final int seqNumber;
    private final byte[] digest;
    private final int id;

    public Commit(int viewNumber, int seqNumber, byte[] digest, int id) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.digest = digest;
        this.id = id;
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    public int viewNumber() {
        return this.viewNumber;
    }

    public int seqNumber() {
        return this.seqNumber;
    }

    public byte[] digest() {
        return this.digest;
    }

    public int id() {
        return this.id;
    }
}
