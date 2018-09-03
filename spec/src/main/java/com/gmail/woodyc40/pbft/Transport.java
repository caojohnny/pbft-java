package com.gmail.woodyc40.pbft;

public interface Transport<T> {
    String primaryId();

    void multicast(Roster roster, T encodedMsg);

    void send(String targetId, T encodedMsg);

    T recv();
}
