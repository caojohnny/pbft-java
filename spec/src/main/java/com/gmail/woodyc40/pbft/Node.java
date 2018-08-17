package com.gmail.woodyc40.pbft;

public interface Node<Op, R, T> {
    int id();

    int tolerance();

    Digester<Op> digester();

    Encoder<Op, R, T> encoder();

    Transport<T> transport();
}
