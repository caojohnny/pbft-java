package com.gmail.woodyc40.pbftjava.spec.protocol;

import com.gmail.woodyc40.pbftjava.spec.Message;
import com.gmail.woodyc40.pbftjava.spec.MessageType;

public class PrePrepare implements Message {
    private static final MessageType TYPE = MessageType.PRE_PREPARE;

    private final int viewNumber;
    private final int seqNumber;
    private final byte[] digest;

    public PrePrepare(int viewNumber, int seqNumber, byte[] digest) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.digest = digest;
    }

    @Override
    public MessageType type() {
        return TYPE;
    }

    public int getViewNumber() {
        return this.viewNumber;
    }

    public int getSeqNumber() {
        return this.seqNumber;
    }

    public byte[] getDigest() {
        return this.digest;
    }
}
