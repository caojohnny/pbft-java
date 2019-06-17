package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Commit;
import com.gmail.woodyc40.pbft.message.PrePrepare;
import com.gmail.woodyc40.pbft.message.Prepare;
import com.gmail.woodyc40.pbft.message.Request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class DefaultTicket<O> implements Ticket<O> {
    private final Request<O> request;
    private final List<Object> messages = new ArrayList<>();

    public DefaultTicket(Request<O> request) {
        this.request = request;
    }

    @Override
    public void append(Object message) {
        this.messages.add(message);
    }

    private boolean matchesPrePrepare(PrePrepare<O> prePrepare, Prepare prepare) {
        return Arrays.equals(prePrepare.digest(), prepare.digest());
    }

    @Override
    public boolean isPrepared(int tolerance) {
        PrePrepare<O> prePrepare = null;
        for (Object message : this.messages) {
            if (message instanceof PrePrepare) {
                prePrepare = (PrePrepare<O>) message;
                break;
            }
        }

        if (prePrepare == null) {
            return false;
        }

        final int requiredMatches = 2 * tolerance;
        int matchingPrepares = 0;
        for (Object message : this.messages) {
            if (message instanceof Prepare) {
                Prepare prepare = (Prepare) message;
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
        PrePrepare<O> prePrepare = null;
        for (Object message : this.messages) {
            if (message instanceof PrePrepare) {
                prePrepare = (PrePrepare<O>) message;
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
            if (message instanceof Prepare) {
                Prepare prepare = (Prepare) message;
                if (this.matchesPrePrepare(prePrepare, prepare)) {
                    matchingPrepares++;
                }
            } else if (message instanceof Commit) {
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
    public Request<O> request() {
        return this.request;
    }
}
