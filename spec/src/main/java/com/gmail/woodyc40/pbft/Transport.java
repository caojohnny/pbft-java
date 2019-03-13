package com.gmail.woodyc40.pbft;

public interface Transport<T> {
    int primaryId();

    void multicast(Roster roster, T encodedMsg);

    void send(int targetId, T encodedMsg);

    T recv();
}
