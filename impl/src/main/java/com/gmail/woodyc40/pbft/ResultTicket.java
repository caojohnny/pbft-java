package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.protocol.Reply;
import com.gmail.woodyc40.pbft.protocol.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultTicket<Op, R, T> {
    private final Request<Op, R, T> request;
    private final int reqConsensusCount;

    private final List<Reply<R>> replies = new ArrayList<>();
    private boolean ready;
    private R result;

    public ResultTicket(Request<Op, R, T> request, int tolerance) {
        this.request = request;
        this.reqConsensusCount = tolerance + 1;
    }

    public Request<Op, R, T> request() {
        return this.request;
    }

    public boolean isReady() {
        synchronized (this) {
            return this.ready;
        }
    }

    public void reset() {
        synchronized (this) {
            this.replies.clear();
            this.ready = false;
            this.result = null;
        }
    }

    public void recvReply(Reply<R> reply) {
        synchronized (this) {
            this.replies.add(reply);

            int size = this.replies.size();

            // Size filter to prevent prematurely calculating consensus
            if (size >= this.reqConsensusCount) {
                // Find highest frequency result
                int highestFreq = Integer.MIN_VALUE;
                R highestResult = null;

                Map<R, Integer> freq = new HashMap<>();
                for (Reply<R> r : this.replies) {
                    int newFreq = freq.compute(r.response(), (k, v) -> v == null ? 1 : v + 1);

                    if (newFreq > highestFreq) {
                        highestFreq = newFreq;
                        highestResult = r.response();
                    }
                }

                // Consensus has been reached
                if (highestFreq >= this.reqConsensusCount) {
                    this.agree(highestResult);
                }
            }
        }
    }

    private void agree(R result) {
        synchronized (this) {
            this.ready = true;
            this.result = result;
            this.notifyAll();
        }
    }

    public R result() {
        synchronized (this) {
            return this.result;
        }
    }
}
