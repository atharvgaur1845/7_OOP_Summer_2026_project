package com.bits.festival.accommodation.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The complete, immutable outcome of an allocation run: the list of {@link Allocation}s, the
 * ordered {@link #waitlist()} of participants who could not be seated, and the run
 * {@link Metrics}.
 *
 * <p>Implements {@link Serializable} so a run can be persisted with {@code ObjectOutputStream}
 * and reloaded later (offline / resume support) — see {@code io.AllocationStore}.
 */
public final class AllocationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Allocation> allocations;
    private final List<Participant> waitlist;
    private final Metrics metrics;

    public AllocationResult(List<Allocation> allocations, List<Participant> waitlist, Metrics metrics) {
        this.allocations = Collections.unmodifiableList(allocations);
        this.waitlist = Collections.unmodifiableList(waitlist);
        this.metrics = metrics;
    }

    public List<Allocation> allocations() {
        return allocations;
    }

    /** Participants who were not assigned, already ordered by waitlist priority. */
    public List<Participant> waitlist() {
        return waitlist;
    }

    public Metrics metrics() {
        return metrics;
    }

    /** Convenience lookup: participant id -> the room they were assigned (only assigned ones). */
    public Map<String, Room> assignmentsById() {
        Map<String, Room> map = new LinkedHashMap<>();
        for (Allocation a : allocations) {
            map.put(a.participant().id(), a.room());
        }
        return map;
    }

    /**
     * Roommates of a participant: everyone else allocated to the same room.
     *
     * @return list of co-occupant participants (possibly empty); empty if the id is unknown
     *         or waitlisted.
     */
    public List<Participant> roommatesOf(String participantId) {
        Room room = assignmentsById().get(participantId);
        if (room == null) {
            return Collections.emptyList();
        }
        return allocations.stream()
                .filter(a -> a.room().equals(room))
                .map(Allocation::participant)
                .filter(p -> !p.id().equals(participantId))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public String toString() {
        return "AllocationResult{" + metrics + ", allocations=" + allocations.size()
                + ", waitlist=" + waitlist.size() + '}';
    }
}
