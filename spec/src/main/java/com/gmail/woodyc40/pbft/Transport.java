package com.gmail.woodyc40.pbft;

public interface Transport<T> {
    void multicast(Roster roster, T encodedMsg);

    void send(String targetId, T encodedMsg);

    T recv();
}
