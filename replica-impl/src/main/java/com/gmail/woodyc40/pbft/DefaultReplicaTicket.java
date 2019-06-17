package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ReplicaCommit;
import com.gmail.woodyc40.pbft.message.ReplicaPrePrepare;
import com.gmail.woodyc40.pbft.message.ReplicaPrepare;
import com.gmail.woodyc40.pbft.message.ReplicaRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DefaultReplicaTicket<O> implements ReplicaTicket<O> {
    private final int viewNumber;
    private final long seqNumber;
    private final ReplicaRequest<O> request;
    private final List<Object> messages = new ArrayList<>();

    public DefaultReplicaTicket(int viewNumber, long seqNumber, ReplicaRequest<O> request) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
        this.request = request;
    }

    @Override
    public int viewNumber() {
        return this.viewNumber;
    }

    @Override
    public long seqNumber() {
        return this.seqNumber;
    }

    @Override
    public void append(Object message) {
        this.messages.add(message);
    }

    private boolean matchesPrePrepare(ReplicaPrePrepare<O> prePrepare, ReplicaPrepare prepare) {
        return Arrays.equals(prePrepare.digest(), prepare.digest());
    }

    @Override
    public boolean isPrepared(int tolerance) {
        ReplicaPrePrepare<O> prePrepare = null;
        for (Object message : this.messages) {
            if (message instanceof ReplicaPrePrepare) {
                prePrepare = (ReplicaPrePrepare<O>) message;
                break;
            }
        }

        if (prePrepare == null) {
            return false;
        }

        final int requiredMatches = 2 * tolerance;
        int matchingPrepares = 0;
        for (Object message : this.messages) {
            if (message instanceof ReplicaPrepare) {
                ReplicaPrepare prepare = (ReplicaPrepare) message;
                if (this.matchesPrePrepare(prePrepare, prepare)) {
                    matchingPrepares++;

                    if (matchingPrepares == requiredMatches) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean isCommittedLocal(int tolerance) {
        ReplicaPrePrepare<O> prePrepare = null;
        for (Object message : this.messages) {
            if (message instanceof ReplicaPrePrepare) {
                prePrepare = (ReplicaPrePrepare<O>) message;
                break;
            }
        }

        if (prePrepare == null) {
            return false;
        }

        final int requiredMatches = 2 * tolerance;
        final int requiredCommits = 2 * tolerance + 1;
        int matchingPrepares = 0;
        int commits = 0;
        for (Object message : this.messages) {
            if (message instanceof ReplicaPrepare) {
                ReplicaPrepare prepare = (ReplicaPrepare) message;
                if (this.matchesPrePrepare(prePrepare, prepare)) {
                    matchingPrepares++;
                }
            } else if (message instanceof ReplicaCommit) {
                commits++;
            }
        }

        return matchingPrepares >= requiredMatches && commits >= requiredCommits;
    }

    @Override
    public Collection<Object> messages() {
        return this.messages;
    }

    @Override
    public ReplicaRequest<O> request() {
        return this.request;
    }
}
