package com.gmail.woodyc40.pbft.example;

import com.gmail.woodyc40.pbft.Digester;
import com.gmail.woodyc40.pbft.example.type.AdditionOperation;
import com.gmail.woodyc40.pbft.protocol.Request;

public class EmptyDigester implements Digester<AdditionOperation> {
    private static final EmptyDigester INSTANCE = new EmptyDigester();
    public static Digester<AdditionOperation> instance() {
        return INSTANCE;
    }

    private static final byte[] EMPTY = new byte[0];

    @Override
    public byte[] digest(Request<AdditionOperation> request) {
        return EMPTY;
    }
}
