package com.gmail.woodyc40.pbft;

public interface Transport<T> {
    void multicast(Roster roster, T encodedMsg);

    void send(String targetId, Roster roster, T encodedMsg);

    T recv();
}
