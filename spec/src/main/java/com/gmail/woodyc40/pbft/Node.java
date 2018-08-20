package com.gmail.woodyc40.pbft;

public interface Node<Op, R, T> {
    String id();

    int tolerance();

    long timeout();

    Digester<Op> digester();

    Encoder<Op, R, T> encoder();

    Transport<T> transport();
}
