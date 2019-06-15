package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.Commit;
import com.gmail.woodyc40.pbft.message.Prepare;
import com.gmail.woodyc40.pbft.message.Request;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface MessageLog {
    void add(Object message);

    boolean shouldBuffer();

    <O> void buffer(Request<O> request);

    boolean isBetweenWaterMarks(long seqNumber);

    boolean exists(int viewNumber, long seqNumber);

    boolean isPrepared(Prepare msg);

    boolean isCommittedLocal(Commit commit);

    @Nullable
    <O> Ticket<O> getTicket(long seqNumber);
}
