package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.ReplicaCommit;
import com.gmail.woodyc40.pbft.message.ReplicaPrePrepare;
import com.gmail.woodyc40.pbft.message.ReplicaPrepare;
import com.gmail.woodyc40.pbft.message.ReplicaRequest;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultReplicaTicket<O, R> implements ReplicaTicket<O, R> {
    private final int viewNumber;
    private final long seqNumber;
    private final Collection<Object> messages = new ConcurrentLinkedQueue<>();

    private volatile ReplicaRequest<O> request;
    private final AtomicReference<ReplicaTicketPhase> phase = new AtomicReference<>(ReplicaTicketPhase.PRE_PREPARE);
    private final CompletableFuture<R> future = new CompletableFuture<>();

    public DefaultReplicaTicket(int viewNumber, long seqNumber) {
        this.viewNumber = viewNumber;
        this.seqNumber = seqNumber;
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

        if (this.request == null) {
            if (message instanceof ReplicaRequest) {
                this.request = (ReplicaRequest<O>) message;
            } else if (message instanceof ReplicaPrePrepare) {
                this.request = ((ReplicaPrePrepare) message).request();
            }
        }
    }

    private boolean matchesPrePrepare(ReplicaPrePrepare<O> prePrepare, ReplicaPrepare prepare) {
        return Arrays.equals(prePrepare.digest(), prepare.digest());
    }

    @Override
    public boolean isPrepared(int tolerance) {
        final int requiredMatches = 2 * tolerance;

        for (Object prePrepareObject : this.messages) {
            if (!(prePrepareObject instanceof ReplicaPrePrepare)) {
                continue;
            }

            ReplicaPrePrepare<O> prePrepare = (ReplicaPrePrepare<O>) prePrepareObject;

            int matchingPrepares = 0;
            for (Object prepareObject : this.messages) {
                if (!(prepareObject instanceof ReplicaPrepare)) {
                    continue;
                }

                ReplicaPrepare prepare = (ReplicaPrepare) prepareObject;
                if (!this.matchesPrePrepare(prePrepare, prepare)) {
                    continue;
                }

                matchingPrepares++;
                if (matchingPrepares == requiredMatches) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isCommittedLocal(int tolerance) {
        // this is checked after the PREPARE phase is
        // chcked in DefaultReplica so it is safe to
        // assume that {@code prepared} is true
        final int requiredCommits = 2 * tolerance + 1;
        int commits = 0;
        for (Object message : this.messages) {
            if (message instanceof ReplicaCommit) {
                commits++;
            }
        }

        return commits >= requiredCommits;
    }

    @Override
    public ReplicaTicketPhase phase() {
        return this.phase.get();
    }

    @Override
    public boolean casPhase(ReplicaTicketPhase old, ReplicaTicketPhase next) {
        return this.phase.compareAndSet(old, next);
    }

    @Override
    public Collection<Object> messages() {
        return this.messages;
    }

    @Override
    public @Nullable ReplicaRequest<O> request() {
        return this.request;
    }

    @Override
    public CompletableFuture<R> result() {
        return this.future;
    }
}
