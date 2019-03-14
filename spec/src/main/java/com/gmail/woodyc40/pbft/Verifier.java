package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Request;

public interface Verifier<Op, R, T> {
    boolean verifyRequest(Request<Op, R, T> request);
}
