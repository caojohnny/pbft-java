package com.gmail.woodyc40.pbft;

public interface Transport<T> {
    void send(String targetId, T encodedMsg);

    T recv();
}
