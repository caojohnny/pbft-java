package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Request;

public interface Verifier<Op> {
    boolean verifyRequest(Request<Op> request);
}
