package com.gmail.woodyc40.pbft;

import com.gmail.woodyc40.pbft.message.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class DefaultReplica<O, R, T> implements Replica<O, R, T> {
    private static final byte[] EMPTY_DIGEST = new byte[0];

    private final int replicaId;
    private final int tolerance;
    private final long timeout;
    private final ReplicaMessageLog log;
    private final ReplicaEncoder<O, R, T> encoder;
    private final ReplicaDigester<O> digester;
    private final ReplicaTransport<T> transport;

    private volatile int viewNumber;
    private volatile boolean disgruntled;
    private final AtomicLong seqCounter = new AtomicLong();
    private final Map<ReplicaRequestKey, LinearBackoff> timeouts = new ConcurrentHashMap<>();

    public DefaultReplica(int replicaId,
                          int tolerance,
                          long timeout,
                          ReplicaMessageLog log,
                          ReplicaEncoder<O, R, T> encoder,
                          ReplicaDigester<O> digester,
                          ReplicaTransport<T> transport) {
        this.replicaId = replicaId;
        this.tolerance = tolerance;
        this.timeout = timeout;
        this.log = log;
        this.encoder = encoder;
        this.digester = digester;
        this.transport = transport;
    }

    @Override
    public int replicaId() {
        return this.replicaId;
    }

    @Override
    public int tolerance() {
        return this.tolerance;
    }

    @Override
    public long timeoutMs() {
        return this.timeout;
    }

    @Override
    public ReplicaMessageLog log() {
        return this.log;
    }

    @Override
    public void setViewNumber(int newViewNumber) {
        this.viewNumber = newViewNumber;
    }

    @Override
    public int viewNumber() {
        return this.viewNumber;
    }

    @Override
    public void setDisgruntled(boolean disgruntled) {
        this.disgruntled = disgruntled;
    }

    @Override
    public boolean isDisgruntled() {
        return this.disgruntled;
    }

    @Override
    public Collection<ReplicaRequestKey> activeTimers() {
        return Collections.unmodifiableCollection(this.timeouts.keySet());
    }

    @Override
    public long checkTimeout(ReplicaRequestKey key) {
        LinearBackoff backoff = this.timeouts.get(key);
        if (backoff == null) {
            return 0L;
        }

        synchronized (backoff) {
            long elapsed = backoff.elapsed();

            /*
             * This method is called in a loop to check the timers on the requests
             * that are currently waiting to be fulfilled.
             *
             * Per PBFT 4.5.2, each time a timeout occurs, a VIEW-CHANGE vote will
             * be multicasted to the current view plus the number of timeouts that
             * have occurred and the replica waits a longer period of time until
             * the next vote is sent. The timer then waits for the sufficient number
             * of VIEW-CHANGE votes to be received before being allowed to expire
             * again after the next period of time and multicast the next
             * VIEW-CHANGE.
             */
            long remainingTime = backoff.timeout() - elapsed;
            if (remainingTime <= 0 && !backoff.isWaitingForVotes()) {
                this.disgruntled = true;

                int newViewNumber = backoff.newViewNumber();
                backoff.expire();

                ReplicaViewChange viewChange = this.log.produceViewChange(
                        newViewNumber,
                        this.replicaId,
                        this.tolerance);
                this.sendViewChange(viewChange);

                /*
                 * Timer expires, meaning that we will need to wait at least the
                 * next period of time before the timer is allowed to expire again
                 * due to having to als include the time for the votes to be
                 * received and processed
                 */
                return backoff.timeout();
            }

            // Timer has not expired yet, so wait out the remaining we computed
            return remainingTime;
        }
    }

    private void resendReply(String clientId, ReplicaTicket<O, R> ticket) {
        ticket.result().thenAccept(result -> {
            int viewNumber = ticket.viewNumber();
            ReplicaRequest<O> request = ticket.request();
            long timestamp = request.timestamp();
            ReplicaReply<R> reply = new DefaultReplicaReply<>(
                    viewNumber,
                    timestamp,
                    clientId,
                    this.replicaId,
                    result);
            this.sendReply(clientId, reply);
        }).exceptionally(t -> {
            throw new RuntimeException(t);
        });
    }

    private void recvRequest(ReplicaRequest<O> request, boolean wasRequestBuffered) {
        String clientId = request.clientId();
        long timestamp = request.timestamp();

        /*
         * At this stage, the request does not have a sequence number yet.
         * Since requests from individual clients are totally ordered by
         * timestamps, we attempt to identify the request processed based on
         * the origin client and the request's timestamp
         *
         * If the ticket is not null, then it indicates that the request has
         * already been fulfilled by this replica, so we resend the reply in
         * accordance with PBFT 4.1.
         */
        ReplicaRequestKey key = new DefaultReplicaRequestKey(clientId, timestamp);
        ReplicaTicket<O, R> cachedTicket = this.log.getTicketFromCache(key);
        if (cachedTicket != null) {
            this.resendReply(clientId, cachedTicket);
            return;
        }

        // Start the timer for this request per PBFT 4.4
        this.timeouts.computeIfAbsent(key, k -> new LinearBackoff(this.viewNumber, this.timeout));

        int primaryId = this.getPrimaryId();

        // PBFT 4.1 - If the request is received by a non-primary replica
        // send the request to the actual primary
        if (this.replicaId != primaryId) {
            this.sendRequest(primaryId, request);
            return;
        }

        /*
         * PBFT 4.2 states that buffered messages should be dispatched in a
         * group, and so when the buffer is flushed, then all requests are
         * fulfilled serially in an async manner because each reply to a
         * buffered request is guaranteed to dispatch the next buffered request.
         */
        if (!wasRequestBuffered) {
            if (this.log.shouldBuffer()) {
                this.log.buffer(request);
                return;
            }
        }

        int currentViewNumber = this.viewNumber;
        long seqNumber = this.seqCounter.getAndIncrement();

        /*
         * The message log is not flat-mapped, meaning that messages are
         * organized based on the request being fulfilled rather than simply
         * being appeneded to improve the performance of processing individual
         * messages.
         *
         * The ticketing system is ordered based on the sequence number as all
         * subsequent messages between the replicas reference the sequence
         * number that this (the primary) has determined.
         */
        ReplicaTicket<O, R> ticket = this.log.newTicket(currentViewNumber, seqNumber);
        // PBFT 4.2 - Append REQUEST
        ticket.append(request);

        /*
         * Non-standard behavior - PBFT 4.2 specifies that requests are not to
         * be sent with PRE-PREPARE, but I leave it up to the transport to
         * decide how messages are sent and encoded and use the PRE-PREPARE
         * message to hold the request as well.
         *
         * Replica has accepted the request, multicast a PRE-PREPARE per PBFT
         * 4.2 which contains the view, the sequence number, request digest,
         * and the request message that was received.
         */
        ReplicaPrePrepare<O> prePrepare = new DefaultReplicaPrePrepare<>(
                currentViewNumber,
                seqNumber,
                this.digester.digest(request),
                request);
        this.sendPrePrepare(prePrepare);

        // PBFT 4.2 - Append PRE-PREPARE
        ticket.append(prePrepare);
    }

    @Override
    public void recvRequest(ReplicaRequest<O> request) {
        // PBFT 4.4 - Do not accept REQUEST when disgruntled
        if (this.disgruntled) {
            return;
        }

        // PBFT 4.2 - Attempt to process non-bufferred request
        this.recvRequest(request, false);
    }

    @Override
    public void sendRequest(int replicaId, ReplicaRequest<O> request) {
        // See #recvRequest(ReplicaRequest, boolean)
        T encodedPrePrepare = this.encoder.encodeRequest(request);
        this.transport.sendMessage(replicaId, encodedPrePrepare);
    }

    @Override
    public void sendPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        // See #recvRequest(ReplicaRequest, boolean)
        T encodedPrePrepare = this.encoder.encodePrePrepare(prePrepare);
        this.transport.multicast(encodedPrePrepare, this.replicaId);
    }

    private boolean verifyPhaseMessage(ReplicaPhaseMessage message) {
        /*
         * The 3 phases specified by PBFT 4.2 have a common verification
         * procedure which is extracted to this method. If this procedure fails,
         * then the replica automatically halts processing because the message
         * has been sent by a faulty replica.
         */

        // PBFT 4.4 - Phase messages are not accepted when the replica is
        // disgruntled
        if (this.disgruntled) {
            return false;
        }

        int currentViewNumber = this.viewNumber;
        int viewNumber = message.viewNumber();
        if (currentViewNumber != viewNumber) {
            return false;
        }

        long seqNumber = message.seqNumber();
        return this.log.isBetweenWaterMarks(seqNumber);
    }

    @Override
    public void recvPrePrepare(ReplicaPrePrepare<O> prePrepare) {
        if (!this.verifyPhaseMessage(prePrepare)) {
            return;
        }

        int currentViewNumber = this.viewNumber;
        byte[] digest = prePrepare.digest();
        ReplicaRequest<O> request = prePrepare.request();
        long seqNumber = prePrepare.seqNumber();

        // PBFT 4.2 - Verify request digest
        byte[] computedDigest = this.digester.digest(request);
        if (!Arrays.equals(digest, computedDigest)) {
            return;
        }

        /*
         * PBFT 4.2 specifies that given a valid PRE-PREPARE (matching
         * signature, view and valid sequence number), the replica must only
         * accept a PRE-PREPARE given that no other PRE-PREPARE has been
         * received OR that the new PRE-PREPARE matches the digest of the one
         * that was already received.
         *
         * Upon accepting the PRE-PREPARE, the replica adds it to the log and
         * multicasts a PREPARE to all other replicas and adding the PREPARE to
         * its log.
         */
        ReplicaTicket<O, R> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket != null) {
            // PRE-PREPARE has previously been inserted into the log for this
            // sequence number - verify the digests match per PBFT 4.2
            for (Object message : ticket.messages()) {
                if (!(message instanceof ReplicaPrePrepare)) {
                    continue;
                }

                ReplicaPrePrepare<O> prevPrePrepare = (ReplicaPrePrepare<O>) message;
                byte[] prevDigest = prevPrePrepare.digest();
                if (!Arrays.equals(prevDigest, digest)) {
                    return;
                }
            }
        } else {
            // PRE-PREPARE is the first - create a new ticket for it in this
            // replica (see #recvRequest(ReplicaRequest, boolean) for why the
            // message log is structured this way)
            ticket = this.log.newTicket(currentViewNumber, seqNumber);
        }

        // PBFT 4.2 - Add PRE-PREPARE along with its REQUEST to the log
        ticket.append(prePrepare);

        // PBFT 4.2 - Multicast PREPARE to other replicas
        ReplicaPrepare prepare = new DefaultReplicaPrepare(
                currentViewNumber,
                seqNumber,
                digest,
                this.replicaId);
        this.sendPrepare(prepare);

        // PBFT 4.2 - Add PREPARE to the log
        ticket.append(prepare);

        /*
         * Per PBFT 4.2, this replica stasfies the prepared predicate IF it has
         * valid PRE-PREPARE, REQUEST and PREPARE messages. Since processing is
         * done asynchronously, the replica state is checked when PRE-PREPARE
         * is accepted in case it arrives later than the corresponding PREPARE
         * messages.
         */
        this.tryAdvanceState(ticket, prePrepare);
    }

    @Override
    public void sendPrepare(ReplicaPrepare prepare) {
        // PBFT 4.2 - Multicast PREPARE upon acceptance of PRE-PREPARE
        T encodedPrepare = this.encoder.encodePrepare(prepare);
        this.transport.multicast(encodedPrepare, this.replicaId);
    }

    private @Nullable ReplicaTicket<O, R> recvPhaseMessage(ReplicaPhaseMessage message) {
        /*
         * The PREPARE and COMMIT messages have a common validation procedure
         * per PBFT 4.2, and has been extracted to this method.
         *
         * Both must pass the #verifyPhaseMessage(...) test as with PRE-PREPARE.
         * If the ticket for these messages have not been added yet, then a new
         * ticket will be created in order for these messages to be added to the
         * log and organized for the sequence number (see
         * #recvRequest(ReplicaRequest, boolean)) because out-of-order phase
         * messages may arrive before the corresponding PRE-PREPARE has been
         * received to set up the ticket.
         */
        if (!this.verifyPhaseMessage(message)) {
            // Return null to indicate reception handlers the message is invalid
            return null;
        }

        int currentViewNumber = message.viewNumber();
        long seqNumber = message.seqNumber();

        ReplicaTicket<O, R> ticket = this.log.getTicket(currentViewNumber, seqNumber);
        if (ticket == null) {
            ticket = this.log.newTicket(currentViewNumber, seqNumber);
        }

        // PBFT 4.2 - PREPARE and COMMIT messages are appended to the log
        // provided that they pass validation
        ticket.append(message);
        return ticket;
    }

    @Override
    public void recvPrepare(ReplicaPrepare prepare) {
        ReplicaTicket<O, R> ticket = this.recvPhaseMessage(prepare);

        if (ticket != null) {
            // PBFT 4.2 - Attempt to advance the state upon reception of
            // PREPARE to check if enough messages have been received to COMMIT
            this.tryAdvanceState(ticket, prepare);
        }
    }

    @Override
    public void sendCommit(ReplicaCommit commit) {
        // PBFT 4.2 - Multicast COMMIT message when prepared becomes true
        T encodedCommit = this.encoder.encodeCommit(commit);
        this.transport.multicast(encodedCommit, this.replicaId);
    }

    @Override
    public void recvCommit(ReplicaCommit commit) {
        ReplicaTicket<O, R> ticket = this.recvPhaseMessage(commit);

        if (ticket != null) {
            // PBFT 4.2 - Attempt to advance the state upon reception of COMMIT
            // to perform the computation
            this.tryAdvanceState(ticket, commit);
        }
    }

    private void tryAdvanceState(ReplicaTicket<O, R> ticket, ReplicaPhaseMessage message) {
        int currentViewNumber = message.viewNumber();
        long seqNumber = message.seqNumber();
        byte[] digest = message.digest();

        ReplicaTicketPhase phase = ticket.phase();
        if (phase == ReplicaTicketPhase.PRE_PREPARE) {
            /*
             * PRE_PREPARE state, formally prior to the prepared predicate
             * becomes true for this replica.
             *
             * Upon receiving 2*f (where f is the maximum faulty nodes) PREPARE
             * messages and validating them, attempt to CAS the phase to
             * indicate that the prepared predicate has become true and COMMIT
             * in accordance with PBFT 4.2.
             */
            if (ticket.isPrepared(this.tolerance) && ticket.casPhase(phase, ReplicaTicketPhase.PREPARE)) {
                ReplicaCommit commit = new DefaultReplicaCommit(
                        currentViewNumber,
                        seqNumber,
                        digest,
                        this.replicaId);
                this.sendCommit(commit);

                // PBFT 4.2 - Add own commit to the log
                ticket.append(commit);
            }
        }

        // Re-check the phase again to ensure that out-of-order COMMIT and
        // PREPARE messages do not prevent the replica from making progress
        phase = ticket.phase();

        if (phase == ReplicaTicketPhase.PREPARE) {
            /*
             * Per PBFT 4.2, committed-local is true only when committed is true
             * so the committed predicate is ignored. Committed-local is
             * achieved when 2*f + 1 COMMIT messages have been logged. CAS the
             * phase to indicate committed-local and perform the computation
             * synchronously.
             *
             * The computation may be performed asynchronously if the
             * implementer so wishes, as long as the cleanup is performed in a
             * callback of some sort.
             */
            if (ticket.isCommittedLocal(this.tolerance) && ticket.casPhase(phase, ReplicaTicketPhase.COMMIT)) {
                ReplicaRequest<O> request = ticket.request();
                if (request != null) {
                    O operation = request.operation();
                    R result = this.compute(operation);

                    String clientId = request.clientId();
                    long timestamp = request.timestamp();
                    ReplicaReply<R> reply = new DefaultReplicaReply<>(
                            currentViewNumber,
                            timestamp,
                            clientId,
                            this.replicaId,
                            result);

                    ReplicaRequestKey key = new DefaultReplicaRequestKey(clientId, timestamp);
                    this.log.completeTicket(key, currentViewNumber, seqNumber);
                    this.sendReply(clientId, reply);

                    this.timeouts.remove(key);
                }

                /*
                 * Checkpointing is specified by PBFT 4.3. A checkpoint is
                 * reached every time the sequence number mod the interval
                 * reaches 0, in which case a CHECKPOINT message is sent
                 * containing the current sequence number, the digest of the
                 * current state and this replica's ID.
                 *
                 * This replica implementation is stateless with respect to the
                 * requests made by the client (i.e. clients do not change the
                 * state of the replica, only the state of the protocol), and so
                 * the digest method simply returns an empty array.
                 */
                if (seqNumber % this.log.checkpointInterval() == 0) {
                    ReplicaCheckpoint checkpoint = new DefaultReplicaCheckpoint(
                            seqNumber,
                            this.digestState(),
                            this.replicaId);
                    this.sendCheckpoint(checkpoint);

                    // Log own checkpoint in accordance to PBFT 4.3
                    this.log.appendCheckpoint(checkpoint, this.tolerance);
                }
            }
        }
    }

    private void handleNextBufferedRequest() {
        ReplicaRequest<O> bufferedRequest = this.log.popBuffer();

        if (bufferedRequest != null) {
            // Process the bufferred request
            this.recvRequest(bufferedRequest, true);
        }
    }

    @Override
    public void sendReply(String clientId, ReplicaReply<R> reply) {
        T encodedReply = this.encoder.encodeReply(reply);
        this.transport.sendReply(clientId, encodedReply);

        // When prior requests are fulfilled, attempt to process the buffer
        // so as to ensure they are dispatched in a timely manner
        this.handleNextBufferedRequest();
    }

    @Override
    public void recvCheckpoint(ReplicaCheckpoint checkpoint) {
        /*
         * Per PBFT 4.3, upon reception of a checkpoint message, check for
         * 2*f + 1 checkpoint messags and perform GC if the checkpoint is
         * stable.
         */
        this.log.appendCheckpoint(checkpoint, this.tolerance);
    }

    @Override
    public void sendCheckpoint(ReplicaCheckpoint checkpoint) {
        // PBFT 4.3 - Multicast checkpoint
        T encodedCheckpoint = this.encoder.encodeCheckpoint(checkpoint);
        this.transport.multicast(encodedCheckpoint, this.replicaId);
    }

    private void enterNewView(int newViewNumber) {
        /*
         * Enter new view by resetting the disgruntled state, updating the
         * view number and clearing any prior timeouts that the replicas have
         * been waiting on
         */
        this.disgruntled = false;
        this.viewNumber = newViewNumber;
        this.timeouts.clear();
    }

    @Override
    public void recvViewChange(ReplicaViewChange viewChange) {
        /*
         * A replica sends a view change to vote out the primary when it
         * becomes disgruntled due to a (or many) timeouts in accordance with
         * PBFT 4.4.
         *
         * When the the view change is recorded to the message log, it will
         * determine firstly whether it should bandwagon the next view change
         * and then secondly whether it should start the next view change
         * timer in accordance with PBFT 4.5.2.
         */
        int curViewNumber = this.viewNumber;
        int newViewNumber = viewChange.newViewNumber();
        int newPrimaryId = getPrimaryId(newViewNumber, this.transport.countKnownReplicas());

        // PBFT 4.5.2 - Determine whether to bandwagon on the lowest view change
        ReplicaViewChangeResult result = this.log.acceptViewChange(viewChange,
                this.replicaId,
                curViewNumber,
                this.tolerance);
        if (result.shouldBandwagon()) {
            ReplicaViewChange bandwagonViewChange = this.log.produceViewChange(
                    result.bandwagonViewNumber(),
                    this.replicaId,
                    this.tolerance);
            this.sendViewChange(bandwagonViewChange);
        }

        // PBFT 4.5.2 - Start the timers that will vote for newViewNumber + 1.
        if (result.beginNextVoteTimer()) {
            for (LinearBackoff backoff : this.timeouts.values()) {
                synchronized (backoff) {
                    int timerViewNumber = backoff.newViewNumber();
                    if (newViewNumber + 1 == timerViewNumber) {
                        backoff.beginNextTimer();
                    }
                }
            }
        }

        if (newPrimaryId == this.replicaId) {
            /*
             * Per PBFT 4.4, if this is the replica being voted as the new
             * primary, then when it receives 2*f votes, it will multicast a
             * NEW-VIEW message to notify the other replicas and perform the
             * necessary procedures to enter the new view.
             */
            ReplicaNewView newView = this.log.produceNewView(newViewNumber, this.replicaId, this.tolerance);
            if (newView != null) {
                this.sendNewView(newView);

                /*
                 * Possibly non-standard behavior - there is actually no
                 * mention of sequence number synchronization, but clearly this
                 * must be done because the NEW-VIEW may possibly update the
                 * checkpoint past the current sequence number; therefore, the
                 * new primary has to ensure that any subsequent requests being
                 * dispatched after the view change occurs must have a
                 * sufficiently high sequence number to pass the watermark test.
                 */
                for (ReplicaPrePrepare<?> prePrepare : newView.preparedProofs()) {
                    long seqNumber = prePrepare.seqNumber();
                    if (this.seqCounter.get() < seqNumber) {
                        this.seqCounter.set(seqNumber + 1);
                    }
                }

                this.enterNewView(newViewNumber);
            }
        }
    }

    @Override
    public void sendViewChange(ReplicaViewChange viewChange) {
        // PBFT 4.4 - Multicast VIEW-CHANGE vote
        T encodedViewChange = this.encoder.encodeViewChange(viewChange);
        this.transport.multicast(encodedViewChange, this.replicaId);
    }

    @Override
    public void recvNewView(ReplicaNewView newView) {
        /*
         * Per PBFT 4.4, upon reception of a NEW-VIEW message, a replica
         * verifies that the VIEW-CHANGE votes all match the new view number.
         * If this is not the case, then the message is disregarded as it is
         * from a faulty replica.
         *
         * The set of PRE-PREPARE messages is then verified using their digests
         * and then dispatches the pre-prepares to itself by multicasting a
         * PREPARE message for each PRE-PREPARE.
         */
        if (!this.log.acceptNewView(newView)) {
            return;
        }

        int newViewNumber = newView.newViewNumber();
        for (ReplicaPrePrepare<?> prePrepare : newView.preparedProofs()) {
            ReplicaRequest<O> request = (ReplicaRequest<O>) prePrepare.request();
            O operation = request.operation();

            // PBFT 4.4 - No-op request used to fulfill the NEW-VIEW constraints
            if (operation == null) {
                continue;
            }

            // PBFT 4.4 - Verify digests of each PRE-PREPARE
            byte[] digest = prePrepare.digest();
            if (!Arrays.equals(digest, this.digester.digest(request))) {
                continue;
            }

            // PBFT 4.2 - Append the PRE-PREPARE to the log
            long seqNumber = prePrepare.seqNumber();
            ReplicaTicket<O, R> ticket = this.log.newTicket(newViewNumber, seqNumber);
            ticket.append(prePrepare);

            ReplicaPrepare prepare = new DefaultReplicaPrepare(
                    newViewNumber,
                    seqNumber,
                    digest,
                    this.replicaId);
            this.sendPrepare(prepare);

            // PBFT 4.4 - Append the PREPARE message to the log
            ticket.append(prepare);
        }

        this.enterNewView(newViewNumber);
    }

    @Override
    public void sendNewView(ReplicaNewView newView) {
        // PBFT 4.4 - Multicast NEW-VIEW
        T encodedNewView = this.encoder.encodeNewView(newView);
        this.transport.multicast(encodedNewView, this.replicaId);
    }

    @Override
    public byte[] digestState() {
        return EMPTY_DIGEST;
    }

    @Override
    public ReplicaEncoder<O, R, T> encoder() {
        return this.encoder;
    }

    @Override
    public ReplicaDigester<O> digester() {
        return this.digester;
    }

    @Override
    public ReplicaTransport<T> transport() {
        return this.transport;
    }

    private static int getPrimaryId(int viewNumber, int knownReplicas) {
        return viewNumber % knownReplicas;
    }

    private int getPrimaryId() {
        return getPrimaryId(this.viewNumber, this.transport.countKnownReplicas());
    }

    private static class LinearBackoff {
        private final long initialTimeout;

        private int newViewNumber;
        private long timeout;
        private long startTime;

        private boolean waitingForVotes;

        public LinearBackoff(int curViewNumber, long timeout) {
            this.initialTimeout = timeout;

            this.newViewNumber = curViewNumber + 1;
            this.timeout = this.initialTimeout;
            this.startTime = System.currentTimeMillis();
        }

        public void expire() {
            this.newViewNumber++;
            this.timeout += this.initialTimeout;
            this.waitingForVotes = true;
        }

        public int newViewNumber() {
            return this.newViewNumber;
        }

        public long timeout() {
            return this.timeout;
        }

        public long elapsed() {
            return System.currentTimeMillis() - this.startTime;
        }

        public boolean isWaitingForVotes() {
            return this.waitingForVotes;
        }

        public void beginNextTimer() {
            if (this.waitingForVotes) {
                this.waitingForVotes = false;
                this.startTime = System.currentTimeMillis();
            }
        }
    }
}
