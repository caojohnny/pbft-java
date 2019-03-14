package com.gmail.woodyc40.pbft.example;

import com.gmail.woodyc40.pbft.Digester;
import com.gmail.woodyc40.pbft.example.type.AdditionOperation;
import com.gmail.woodyc40.pbft.example.type.AdditionResult;
import com.gmail.woodyc40.pbft.protocol.Request;

public class EmptyDigester implements Digester<AdditionOperation, AdditionResult, byte[]> {
    private static final EmptyDigester INSTANCE = new EmptyDigester();
    private static final byte[] EMPTY = new byte[0];

    public static Digester<AdditionOperation, AdditionResult, byte[]> instance() {
        return INSTANCE;
    }

    @Override
    public byte[] digest(Request<AdditionOperation, AdditionResult, byte[]> request) {
        return EMPTY;
    }
}
