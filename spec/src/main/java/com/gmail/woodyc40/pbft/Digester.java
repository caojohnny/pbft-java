package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Request;

public interface Digester<Op> {
    byte[] digest(Request<Op> request);
}
