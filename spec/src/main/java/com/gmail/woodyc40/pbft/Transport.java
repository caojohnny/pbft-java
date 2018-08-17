package com.gmail.woodyc40.pbft;

public interface Transport<T> {
    void send(int targetId, T encodedMsg);

    T recv();
}
