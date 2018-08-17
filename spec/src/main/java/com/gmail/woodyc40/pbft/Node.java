package com.gmail.woodyc40.pbft;

public interface Node {
    int tolerance();

    Digester digester();

    Encoder encoder();

    Transport transport();
}
