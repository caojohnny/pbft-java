package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Request;

public interface Digester<Op, R, T> {
    byte[] digest(Request<Op, R, T> request);
}
