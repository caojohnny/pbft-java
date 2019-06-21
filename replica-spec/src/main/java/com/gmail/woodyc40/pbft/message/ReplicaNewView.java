package com.gmail.woodyc40.pbft.message;

import java.util.Collection;

/**
 * Represents a PBFT {@code NEW-VIEW} message that is sent
 * by the "new" primary in order to confirm that the other
 * replicas should switch into the new view.
 */
public interface ReplicaNewView {
    /**
     * The view that all replicas should switch into.
     *
     * @return the new view number
     */
    int newViewNumber();

    /**
     * A collection of proofs that the primary has been
     * voted into a new view.
     *
     * @return the set of view change messages received
     */
    Collection<ReplicaViewChange> viewChangeProofs();

    /**
     * A collection of pre-prepare sequences between the
     * selected stable sequence numbers proving that the
     * min-s and max-s requests have been prepared.
     *
     * @return the collection pre-prepare messages known
     * to the new primary
     */
    Collection<ReplicaPrePrepare<?>> preparedProofs();
}
