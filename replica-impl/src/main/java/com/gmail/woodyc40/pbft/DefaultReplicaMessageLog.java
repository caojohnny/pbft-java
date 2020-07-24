package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DefaultReplicaMessageLog implements ReplicaMessageLog {
    private static final byte[] NULL_DIGEST = new byte[0];
    private static final DefaultReplicaRequest<Object> NULL_REQ = new DefaultReplicaRequest<>(null, 0, "");

    private final int bufferThreshold;
    private final int checkpointInterval;
    private final int watermarkInterval;

    private final Deque<ReplicaRequest<?>> buffer = new ConcurrentLinkedDeque<>();

    private final Map<ReplicaRequestKey, ReplicaTicket<?, ?>> ticketCache = new ConcurrentHashMap<>();
    private final Map<TicketKey, ReplicaTicket<?, ?>> tickets = new ConcurrentHashMap<>();

    private final Map<Long, Collection<ReplicaCheckpoint>> checkpoints = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Integer, ReplicaViewChange>> viewChanges = new ConcurrentHashMap<>();

    private volatile long lowWaterMark;
    private volatile long highWaterMark;

    public DefaultReplicaMessageLog(int bufferThreshold, int checkpointInterval, int watermarkInterval) {
        this.bufferThreshold = bufferThreshold;
        this.checkpointInterval = checkpointInterval;
        this.watermarkInterval = watermarkInterval;

        this.lowWaterMark = 0;
        this.highWaterMark = this.lowWaterMark + watermarkInterval;
    }

    @Override
    public int checkpointInterval() {
        return this.checkpointInterval;
    }

    @Override
    public int watermarkInterval() {
        return this.watermarkInterval;
    }

    @Override
    public <O, R> @Nullable ReplicaTicket<O, R> getTicketFromCache(ReplicaRequestKey key) {
        return (ReplicaTicket<O, R>) this.ticketCache.get(key);
    }

    @Override
    public @Nullable <O, R> ReplicaTicket<O, R> getTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return (ReplicaTicket<O, R>) this.tickets.get(key);
    }

    @Override
    public @NonNull <O, R> ReplicaTicket<O, R> newTicket(int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        return (ReplicaTicket<O, R>) this.tickets.computeIfAbsent(key, k -> new DefaultReplicaTicket<>(viewNumber, seqNumber));
    }

    @Override
    public boolean completeTicket(ReplicaRequestKey rrk, int viewNumber, long seqNumber) {
        TicketKey key = new TicketKey(viewNumber, seqNumber);
        ReplicaTicket<?, ?> ticket = this.tickets.remove(key);

        this.ticketCache.put(rrk, ticket);

        return ticket != null;
    }

    private void gcCheckpoint(long checkpoint) {
        /*
         * Procedure used to discard all PRE-PREPARE, PREPARE and COMMIT
         * messages with sequence number less than or equal the in addition to
         * any prior checkpoint proof per PBFT 4.3.
         *
         * A stable checkpoint then allows the water marks to slide over to
         * the checkpoint < x <= checkpoint + watermarkInterval per PBFT 4.3.
         */
        for (Entry<ReplicaRequestKey, ReplicaTicket<?, ?>> entry : this.ticketCache.entrySet()) {
            ReplicaTicket<?, ?> ticket = entry.getValue();
            if (ticket.seqNumber() <= checkpoint) {
                this.ticketCache.remove(entry.getKey());
            }
        }

        for (Long seqNumber : this.checkpoints.keySet()) {
            if (seqNumber < checkpoint) {
                this.checkpoints.remove(seqNumber);
            }
        }

        this.highWaterMark = checkpoint + this.watermarkInterval;
        this.lowWaterMark = checkpoint;
    }

    @Override
    public void appendCheckpoint(ReplicaCheckpoint checkpoint, int tolerance) {
        /*
         * Per PBFT 4.3, each time a checkpoint is generated or received, it
         * gets stored in the log until 2f + 1 are accumulated that have
         * matching digests to the checkpoint that was added to the log, in
         * which case the garbage collection occurs (see #gcCheckpoint(long)).
         */
        long seqNumber = checkpoint.lastSeqNumber();
        Collection<ReplicaCheckpoint> checkpointProofs = this.checkpoints.computeIfAbsent(seqNumber, k -> new ConcurrentLinkedQueue<>());
        checkpointProofs.add(checkpoint);

        final int stableCount = 2 * tolerance + 1;
        int matching = 0;

        // Use a loop here to avoid the linked list being traversed in its
        // entirety
        for (ReplicaCheckpoint proof : checkpointProofs) {
            if (Arrays.equals(proof.digest(), checkpoint.digest())) {
                matching++;

                if (matching == stableCount) {
                    this.gcCheckpoint(seqNumber);
                    return;
                }
            }
        }
    }

    private @Nullable Collection<ReplicaPhaseMessage> selectPreparedProofs(ReplicaTicket<?, ?> ticket, int requiredMatches) {
        /*
         * Selecting the proofs of PRE-PREPARE and PREPARE messages for the
         * VIEW-CHANGE vote per PBFT 4.4.
         *
         * This procedure is designed to be run over each ReplicaTicket and
         * collects the PRE-PREPARE for the ticket and the required PREPARE
         * messages, otherwise returning null if there were not enough
         * PREPARE messages or PRE-PREPARE has not been received yet.
         */
        Collection<ReplicaPhaseMessage> proof = new ArrayList<>();
        for (Object prePrepareObject : ticket.messages()) {
            if (!(prePrepareObject instanceof ReplicaPrePrepare)) {
                continue;
            }

            ReplicaPrePrepare<?> prePrepare = (ReplicaPrePrepare<?>) prePrepareObject;
            proof.add(prePrepare);

            int matchingPrepares = 0;
            for (Object prepareObject : ticket.messages()) {
                if (!(prepareObject instanceof ReplicaPrepare)) {
                    continue;
                }

                ReplicaPrepare prepare = (ReplicaPrepare) prepareObject;
                if (!Arrays.equals(prePrepare.digest(), prepare.digest())) {
                    continue;
                }

                matchingPrepares++;
                proof.add(prepare);

                if (matchingPrepares == requiredMatches) {
                    return proof;
                }
            }
        }

        return null;
    }

    @Override
    public ReplicaViewChange produceViewChange(int newViewNumber, int replicaId, int tolerance) {
        /*
         * Produces a VIEW-CHANGE vote message in accordance with PBFT 4.4.
         *
         * The last stable checkpoint is defined as the low water mark for the
         * message log. The checkpoint proofs are provided each time the
         * checkpoint advances, or could possibly be empty if the checkpoint
         * is still at 0 (i.e. starting state).
         *
         * Proofs are gathered through #selectPreparedProofs(...) with 2f
         * required PREPARE messages.
         */
        long checkpoint = this.lowWaterMark;

        Collection<ReplicaCheckpoint> checkpointProofs = checkpoint == 0 ?
                Collections.emptyList() : this.checkpoints.get(checkpoint);
        if (checkpointProofs == null) {
            throw new IllegalStateException("Checkpoint has diverged without any proof");
        }

        final int requiredMatches = 2 * tolerance;
        Map<Long, Collection<ReplicaPhaseMessage>> preparedProofs = new HashMap<>();

        // Scan through the ticket cache (i.e. the completed tickets)
        for (ReplicaTicket<?, ?> ticket : this.ticketCache.values()) {
            long seqNumber = ticket.seqNumber();
            if (seqNumber > checkpoint) {
                Collection<ReplicaPhaseMessage> proofs = this.selectPreparedProofs(ticket, requiredMatches);
                if (proofs == null) {
                    continue;
                }

                preparedProofs.put(seqNumber, proofs);
            }
        }

        // Scan through the currently active tickets
        for (ReplicaTicket<?, ?> ticket : this.tickets.values()) {
            ReplicaTicketPhase phase = ticket.phase();
            if (phase == ReplicaTicketPhase.PRE_PREPARE) {
                continue;
            }

            long seqNumber = ticket.seqNumber();
            if (seqNumber > checkpoint) {
                Collection<ReplicaPhaseMessage> proofs = this.selectPreparedProofs(ticket, requiredMatches);
                if (proofs == null) {
                    continue;
                }

                preparedProofs.put(seqNumber, proofs);
            }
        }

        DefaultReplicaViewChange viewChange = new DefaultReplicaViewChange(
                newViewNumber,
                checkpoint,
                checkpointProofs,
                preparedProofs,
                replicaId);

        /*
         * Potentially non-standard behavior - PBFT 4.5.2 does not specify
         * whether replicas include their own view change messages. For 3f + 1
         * replicas in the system, then given the max f faulty nodes, 3f + 1 - f
         * or 2f + 1 replicas are expected to vote, meaning that excluding the
         * initiating replica reduces the total number of votes to 2f. Since
         * PBFT 4.5.2 states that the next view change may only be initiated by
         * a quorum of 2f + 1 replicas, then electing a faulty primary that does
         * not multicast a NEW-VIEW message will cause the entire system to
         * stall; therefore I do include the initiating replica here.
         */
        Map<Integer, ReplicaViewChange> newViewSet = this.viewChanges.computeIfAbsent(newViewNumber, k -> new ConcurrentHashMap<>());
        newViewSet.put(replicaId, viewChange);

        return viewChange;
    }

    @Override
    public ReplicaViewChangeResult acceptViewChange(ReplicaViewChange viewChange, int curReplicaId, int curViewNumber, int tolerance) {
        /*
         * Per PBFT 4.4, a received VIEW-CHANGE vote is stored into the message
         * log and the state is returned to the replica as
         * ReplicaViewChangeResult.
         *
         * The procedure first computes the total number of votes from other
         * replicas that try to move the view a higher view number. If this
         * number of other relicas is equal to the bandwagon size, then this
         * replica contributes its vote once to avoid creating an infinite
         * response loop and taking up the network capacity.
         *
         * Secondly, this procedure finds the smallest view the system is
         * attempting to elect and selects that to bandwagon.
         *
         * Finally, this procedure determines if the number of votes is enough
         * to restart the timer to move to the view after the one now being
         * elected in the case that the candidate view has a faulty primary.
         */
        int newViewNumber = viewChange.newViewNumber();
        int replicaId = viewChange.replicaId();

        Map<Integer, ReplicaViewChange> newViewSet = this.viewChanges.computeIfAbsent(newViewNumber, k -> new ConcurrentHashMap<>());
        newViewSet.put(replicaId, viewChange);

        final int bandwagonSize = tolerance + 1;

        int totalVotes = 0;
        int smallestView = Integer.MAX_VALUE;
        for (Entry<Integer, Map<Integer, ReplicaViewChange>> entry : this.viewChanges.entrySet()) {
            int entryView = entry.getKey();
            if (entryView <= curViewNumber) {
                continue;
            }

            Map<Integer, ReplicaViewChange> votes = entry.getValue();
            int entryVotes = votes.size();

            /*
             * See #produceViewChange(...)
             * Subtract the current replica's vote to obtain the votes from the
             * other replicas
             */
            if (votes.containsKey(curReplicaId)) {
                entryVotes--;
            }

            totalVotes += entryVotes;

            if (smallestView > entryView) {
                smallestView = entryView;
            }
        }

        boolean shouldBandwagon = totalVotes == bandwagonSize;

        final int timerThreshold = 2 * tolerance + 1;
        boolean beginNextVote = newViewSet.size() >= timerThreshold;

        return new DefaultReplicaViewChangeResult(shouldBandwagon, smallestView, beginNextVote);
    }

    private Collection<ReplicaPrePrepare<?>> selectPreparedProofs(int newViewNumber, long minS, long maxS, Map<Long, ReplicaPrePrepare<?>> prePrepareMap) {
        /*
         * This procedure computes the prepared proofs for the NEW-VIEW message
         * that is sent by the primary when it is elected in accordance with
         * PBFT 4.4. It adds messages in between the min-s and max-s sequences,
         * including any missing messages by using a no-op PRE-PREPARE message.
         *
         * Non-standard behavior - PBFT 4.4 specifies that PRE-PREPARE messages
         * are to be sent without their requests, but again, this is up to the
         * transport to decide how to work. For simplicity, the default
         * implementation sends the request along with the PRE-PREPARE as
         * explained in DefaultReplica.
         */
        Collection<ReplicaPrePrepare<?>> sequenceProofs = new ArrayList<>();
        for (long i = minS; minS != maxS && i <= maxS; i++) {
            ReplicaPrePrepare<?> prePrepareProofMessage = prePrepareMap.get(i);
            if (prePrepareProofMessage == null) {
                prePrepareProofMessage = new DefaultReplicaPrePrepare<>(
                        newViewNumber,
                        i,
                        NULL_DIGEST,
                        NULL_REQ);
            }

            sequenceProofs.add(prePrepareProofMessage);

            ReplicaTicket<Object, Object> ticket = this.newTicket(newViewNumber, i);
            ticket.append(prePrepareProofMessage);
        }

        return sequenceProofs;
    }

    @Override
    public @Nullable ReplicaNewView produceNewView(int newViewNumber, int replicaId, int tolerance) {
        /*
         * Produces the NEW-VIEW message to notify the other replicas of the
         * elected primary in accordance with PBFT 4.4.
         *
         * If there is not a quorum of votes for this replica to become the
         * primary excluding this replica's own vote, then do not proceed.
         *
         * This scans through all VIEW-CHANGE votes for their checkpoint proofs
         * and their prepared proofs to look for the min and max sequence
         * numbers to generate the final PREPARE proofs
         * (see #selectPrepareProofs(...)). These values are also used to
         * update the water marks and passed through the proof map.
         *
         * The VIEW-CHANGE votes are then added in addition to the PREPARE
         * proofs to the NEW-VIEW message.
         */

        Map<Integer, ReplicaViewChange> newViewSet = this.viewChanges.get(newViewNumber);
        int votes = newViewSet.size();
        boolean hasOwnViewChange = newViewSet.containsKey(replicaId);
        if (hasOwnViewChange) {
            votes--;
        }

        final int quorum = 2 * tolerance;
        if (votes < quorum) {
            return null;
        }

        long minS = Long.MAX_VALUE;
        long maxS = Long.MIN_VALUE;
        Collection<ReplicaCheckpoint> minSProof = null;
        Map<Long, ReplicaPrePrepare<?>> prePrepareMap = new HashMap<>();
        for (ReplicaViewChange viewChange : newViewSet.values()) {
            long seqNumber = viewChange.lastSeqNumber();
            Collection<ReplicaCheckpoint> proofs = viewChange.checkpointProofs();
            if (seqNumber < minS) {
                minS = seqNumber;
                minSProof = proofs;
            }

            if (seqNumber > maxS) {
                maxS = seqNumber;
            }

            for (Entry<Long, Collection<ReplicaPhaseMessage>> entry : viewChange.preparedProofs().entrySet()) {
                long prePrepareSeqNumber = entry.getKey();
                if (prePrepareSeqNumber > maxS) {
                    maxS = prePrepareSeqNumber;
                }

                for (ReplicaPhaseMessage phaseMessage : entry.getValue()) {
                    if (!(phaseMessage instanceof ReplicaPrePrepare)) {
                        continue;
                    }

                    prePrepareMap.put(prePrepareSeqNumber, (ReplicaPrePrepare<?>) phaseMessage);
                    break;
                }
            }
        }

        this.gcNewView(newViewNumber);
        if (minS > this.lowWaterMark) {
            this.checkpoints.put(minS, minSProof);
            this.gcCheckpoint(minS);
        }

        Collection<ReplicaViewChange> viewChangeProofs = new ArrayList<>(newViewSet.values());
        viewChangeProofs.addAll(newViewSet.values());
        if (!hasOwnViewChange) {
            viewChangeProofs.add(this.produceViewChange(newViewNumber, replicaId, tolerance));
        }

        Collection<ReplicaPrePrepare<?>> preparedProofs = this.selectPreparedProofs(newViewNumber, minS, maxS, prePrepareMap);

        return new DefaultReplicaNewView(
                newViewNumber,
                viewChangeProofs,
                preparedProofs);
    }

    private void gcNewView(int newViewNumber) {
        /*
         * Performs clean-up for entering a new view in accordance with PBFT
         * 4.4. This means that any view change votes and pending tickets that
         * are not in the new view are removed.
         */
        this.viewChanges.remove(newViewNumber);

        for (TicketKey key : this.tickets.keySet()) {
            if (key.viewNumber() != newViewNumber) {
                this.tickets.remove(key);
            }
        }
    }

    @Override
    public boolean acceptNewView(ReplicaNewView newView) {
        /*
         * Verify the change to a new view in accordance with PBFT 4.4 and then
         * find the min-s value and update the low water mark if it is lagging
         * behind the new view.
         */
        int newViewNumber = newView.newViewNumber();
        this.gcNewView(newViewNumber);

        long minS = Integer.MAX_VALUE;
        Collection<ReplicaCheckpoint> checkpointProofs = null;
        for (ReplicaViewChange viewChange : newView.viewChangeProofs()) {
            if (newViewNumber != viewChange.newViewNumber()) {
                return false;
            }

            long seqNumber = viewChange.lastSeqNumber();
            if (seqNumber < minS) {
                minS = seqNumber;
                checkpointProofs = viewChange.checkpointProofs();
            }
        }

        if (this.lowWaterMark < minS) {
            this.checkpoints.put(minS, checkpointProofs);
            this.gcCheckpoint(minS);
        }

        return true;
    }

    @Override
    public boolean shouldBuffer() {
        return this.tickets.size() >= this.bufferThreshold;
    }

    @Override
    public <O> void buffer(ReplicaRequest<O> request) {
        this.buffer.addLast(request);
    }

    @Override
    public @Nullable <O> ReplicaRequest<O> popBuffer() {
        return (ReplicaRequest<O>) this.buffer.pollFirst();
    }

    @Override
    public boolean isBetweenWaterMarks(long seqNumber) {
        return seqNumber >= this.lowWaterMark && seqNumber <= this.highWaterMark;
    }

    private static class TicketKey {
        private final int viewNumber;
        private final long seqNumber;

        public TicketKey(int viewNumber, long seqNumber) {
            this.viewNumber = viewNumber;
            this.seqNumber = seqNumber;
        }

        public int viewNumber() {
            return this.viewNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TicketKey)) return false;

            TicketKey ticketKey = (TicketKey) o;

            if (this.viewNumber != ticketKey.viewNumber) return false;
            return this.seqNumber == ticketKey.seqNumber;
        }

        @Override
        public int hashCode() {
            int result = this.viewNumber;
            result = 31 * result + (int) (this.seqNumber ^ (this.seqNumber >>> 32));
            return result;
        }
    }
}
