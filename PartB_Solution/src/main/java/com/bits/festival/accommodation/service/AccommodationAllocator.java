package com.bits.festival.accommodation.service;

import com.bits.festival.accommodation.algorithm.BipartiteMatcher;
import com.bits.festival.accommodation.algorithm.CostMatrix;
import com.bits.festival.accommodation.algorithm.CostMatrixBuilder;
import com.bits.festival.accommodation.algorithm.HungarianAlgorithm;
import com.bits.festival.accommodation.cost.CostStrategy;
import com.bits.festival.accommodation.cost.CostStrategyFactory;
import com.bits.festival.accommodation.exception.InfeasibleAllocationException;
import com.bits.festival.accommodation.exception.InvalidInputException;
import com.bits.festival.accommodation.model.Allocation;
import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Metrics;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;
import com.bits.festival.accommodation.model.RoomSlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Orchestrates the full accommodation pipeline:
 * <ol>
 *   <li><b>Validate</b> the participants and rooms (unique IDs, sane capacities).</li>
 *   <li><b>Build</b> a square cost matrix (rooms expanded to beds, soft costs + hard-constraint
 *       sentinels) — see {@link CostMatrixBuilder}.</li>
 *   <li><b>Solve</b> for the minimum-cost assignment with the {@link HungarianAlgorithm}.</li>
 *   <li><b>Recover</b> feasible placements the cost-optimum stranded, via a priority-ordered
 *       {@link BipartiteMatcher} fallback over the beds that remained empty.</li>
 *   <li><b>Nudge</b> roommate requests by safe cost-neutral swaps (best-effort).</li>
 *   <li><b>Waitlist</b> whoever is still unplaced, ordered by category then arrival day.</li>
 * </ol>
 *
 * <p>The waitlist comparator (the {@link #WAITLIST_ORDER} {@code Comparator}) prioritises
 * performers, then earlier arrivals, then ID — and the same order drives the fallback fill so
 * higher-priority participants claim scarce leftover beds first.
 */
public final class AccommodationAllocator {

    /** Performers first, then earlier arrival day, then stable by ID. */
    public static final Comparator<Participant> WAITLIST_ORDER =
            Comparator.comparingInt((Participant p) -> p.category().rank())
                    .thenComparingInt(Participant::arrivalDay)
                    .thenComparing(Participant::id);

    private final CostStrategy strategy;
    private final boolean roommateNudgeEnabled;

    /** Uses the default cost strategy and enables the roommate co-location nudge. */
    public AccommodationAllocator() {
        this(CostStrategyFactory.createDefault(), true);
    }

    public AccommodationAllocator(CostStrategy strategy) {
        this(strategy, true);
    }

    public AccommodationAllocator(CostStrategy strategy, boolean roommateNudgeEnabled) {
        this.strategy = strategy;
        this.roommateNudgeEnabled = roommateNudgeEnabled;
    }

    /**
     * Run an allocation.
     *
     * @throws InvalidInputException        if IDs duplicate or a capacity is negative
     * @throws InfeasibleAllocationException if there are participants but zero beds
     */
    public AllocationResult allocate(List<Participant> participants, List<Room> rooms)
            throws InvalidInputException, InfeasibleAllocationException {
        validate(participants, rooms);

        final long start = System.nanoTime();
        final int totalSlots = rooms.stream().mapToInt(Room::capacity).sum();

        if (participants.isEmpty()) {
            Metrics m = new Metrics(0, 0, 0, totalSlots, 0.0, 0, elapsedMillis(start));
            return new AllocationResult(new ArrayList<>(), new ArrayList<>(), m);
        }
        if (totalSlots == 0) {
            throw new InfeasibleAllocationException(
                    "No beds available for " + participants.size() + " participant(s)");
        }

        // ---- Build matrix + solve optimal assignment ----
        final CostMatrix cm = new CostMatrixBuilder(strategy).build(participants, rooms);
        final int[] assignment = HungarianAlgorithm.solve(cm.matrix());

        // ---- Interpret the optimal assignment ----
        final Map<String, Allocation> assigned = new LinkedHashMap<>(); // participantId -> Allocation
        final Set<RoomSlot> usedSlots = new HashSet<>();
        final List<Participant> unassigned = new ArrayList<>();

        for (int row = 0; row < cm.participantCount(); row++) {
            final Participant p = cm.participants().get(row);
            final int col = assignment[row];
            if (cm.isRealSlotColumn(col) && !cm.isForbidden(row, col)) {
                RoomSlot slot = cm.slots().get(col);
                // Store the TRUE soft cost (the matrix value carries a priority bias used only
                // for seat-vs-waitlist ordering, which must not pollute reported dissatisfaction).
                double trueCost = strategy.cost(p, slot.room());
                assigned.put(p.id(), new Allocation(p, slot.room(), trueCost));
                usedSlots.add(slot);
            } else {
                unassigned.add(p);
            }
        }

        // ---- Feasibility fallback: fill empty beds with stranded participants ----
        fillFromFreeBeds(cm, assigned, usedSlots, unassigned);

        // ---- Best-effort roommate co-location ----
        if (roommateNudgeEnabled) {
            improveRoommateColocation(assigned);
        }

        // ---- Waitlist whoever remains, in priority order ----
        final PriorityQueue<Participant> waitlistPq = new PriorityQueue<>(WAITLIST_ORDER);
        waitlistPq.addAll(unassigned);
        final List<Participant> waitlist = new ArrayList<>(waitlistPq.size());
        while (!waitlistPq.isEmpty()) {
            waitlist.add(waitlistPq.poll());
        }

        // ---- Assemble result + metrics ----
        final List<Allocation> allocations = new ArrayList<>(assigned.values());
        double totalCost = allocations.stream().mapToDouble(Allocation::cost).sum();
        int violations = (int) allocations.stream()
                .filter(a -> strategy.isForbidden(strategy.cost(a.participant(), a.room())))
                .count();

        Metrics metrics = new Metrics(participants.size(), allocations.size(), waitlist.size(),
                totalSlots, totalCost, violations, elapsedMillis(start));
        return new AllocationResult(allocations, waitlist, metrics);
    }

    /**
     * Re-match stranded participants against beds that ended up empty, considering only
     * feasible pairings, processing higher-priority participants first.
     */
    private void fillFromFreeBeds(CostMatrix cm, Map<String, Allocation> assigned,
                                  Set<RoomSlot> usedSlots, List<Participant> unassigned) {
        if (unassigned.isEmpty()) {
            return;
        }
        // Free real beds.
        final List<RoomSlot> freeSlots = new ArrayList<>();
        for (RoomSlot slot : cm.slots()) {
            if (!usedSlots.contains(slot)) {
                freeSlots.add(slot);
            }
        }
        if (freeSlots.isEmpty()) {
            return;
        }

        // Process unassigned in waitlist (priority) order so scarce beds go to top categories.
        unassigned.sort(WAITLIST_ORDER);

        // Build feasible bipartite edges: participant -> free bed (non-forbidden cost).
        final List<List<Integer>> adjacency = BipartiteMatcher.newAdjacency(unassigned.size());
        for (int l = 0; l < unassigned.size(); l++) {
            Participant p = unassigned.get(l);
            for (int r = 0; r < freeSlots.size(); r++) {
                double c = strategy.cost(p, freeSlots.get(r).room());
                if (!strategy.isForbidden(c)) {
                    adjacency.get(l).add(r);
                }
            }
        }

        final int[] matchLeft = BipartiteMatcher.match(adjacency, freeSlots.size());

        // Apply matches and remove newly-seated participants from the unassigned list.
        final List<Participant> stillUnassigned = new ArrayList<>();
        for (int l = 0; l < unassigned.size(); l++) {
            Participant p = unassigned.get(l);
            int r = matchLeft[l];
            if (r >= 0) {
                RoomSlot slot = freeSlots.get(r);
                double c = strategy.cost(p, slot.room());
                assigned.put(p.id(), new Allocation(p, slot.room(), c));
                usedSlots.add(slot);
            } else {
                stillUnassigned.add(p);
            }
        }
        unassigned.clear();
        unassigned.addAll(stillUnassigned);
    }

    /**
     * Best-effort local search to honour roommate requests, using two complementary moves, each
     * applied only when it strictly increases roommate co-location, keeps every hard constraint
     * satisfied, and never increases total dissatisfaction:
     * <ul>
     *   <li><b>Move</b> a participant into a requested roommate's room when that room has a free
     *       bed (the common case).</li>
     *   <li><b>Swap</b> two participants' rooms when both rooms are full (no free bed to move
     *       into) but exchanging them co-locates more roommates.</li>
     * </ul>
     * Bounded passes keep it cheap; the solution can only get better or stay the same.
     */
    private void improveRoommateColocation(Map<String, Allocation> assigned) {
        if (assigned.size() < 2) {
            return;
        }
        final int maxPasses = 4;
        for (int pass = 0; pass < maxPasses; pass++) {
            boolean improved = tryMoves(assigned);
            improved |= trySwaps(assigned);
            if (!improved) {
                break;
            }
        }
    }

    /** Move participants into a requested roommate's room when a bed is free there. */
    private boolean tryMoves(Map<String, Allocation> assigned) {
        final Map<String, Integer> occupancy = occupancy(assigned);
        boolean improved = false;
        for (Allocation a : new ArrayList<>(assigned.values())) {
            // 'a' may be stale if this participant already moved this pass; re-read.
            Allocation current = assigned.get(a.participant().id());
            Participant p = current.participant();
            Room from = current.room();
            for (String wantedId : p.preference().preferredRoommates()) {
                Allocation other = assigned.get(wantedId);
                if (other == null || other.room().equals(from)) {
                    continue; // roommate not seated, or already together
                }
                Room to = other.room();
                if (occupancy.getOrDefault(to.id(), 0) >= to.capacity()) {
                    continue; // no free bed in the roommate's room
                }
                double newCost = strategy.cost(p, to);
                if (strategy.isForbidden(newCost) || newCost > current.cost() + 1e-9) {
                    continue; // infeasible or would worsen dissatisfaction
                }
                int before = roommateSatisfaction(assigned, from, to);
                assigned.put(p.id(), new Allocation(p, to, newCost));
                int after = roommateSatisfaction(assigned, from, to);
                if (after > before) {
                    occupancy.merge(from.id(), -1, Integer::sum);
                    occupancy.merge(to.id(), 1, Integer::sum);
                    improved = true;
                    break; // p moved; continue with the next participant
                }
                assigned.put(p.id(), current); // revert
            }
        }
        return improved;
    }

    /** Swap two participants' rooms when that co-locates more roommates (handles full rooms). */
    private boolean trySwaps(Map<String, Allocation> assigned) {
        final List<Allocation> list = new ArrayList<>(assigned.values());
        boolean improved = false;
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Allocation a1 = assigned.get(list.get(i).participant().id());
                Allocation a2 = assigned.get(list.get(j).participant().id());
                if (trySwap(assigned, a1, a2)) {
                    improved = true;
                }
            }
        }
        return improved;
    }

    private Map<String, Integer> occupancy(Map<String, Allocation> assigned) {
        Map<String, Integer> occ = new java.util.HashMap<>();
        for (Allocation a : assigned.values()) {
            occ.merge(a.room().id(), 1, Integer::sum);
        }
        return occ;
    }

    private boolean trySwap(Map<String, Allocation> assigned, Allocation a1, Allocation a2) {
        Participant p1 = a1.participant();
        Participant p2 = a2.participant();
        Room r1 = a1.room();
        Room r2 = a2.room();
        if (r1.equals(r2)) {
            return false;
        }
        double newC1 = strategy.cost(p1, r2);
        double newC2 = strategy.cost(p2, r1);
        if (strategy.isForbidden(newC1) || strategy.isForbidden(newC2)) {
            return false; // would violate a hard constraint
        }
        // Do not worsen total dissatisfaction.
        final double eps = 1e-9;
        if (newC1 + newC2 > a1.cost() + a2.cost() + eps) {
            return false;
        }
        // Only swap if it strictly improves roommate co-location across the two rooms.
        int before = roommateSatisfaction(assigned, r1, r2);
        // Hypothetically apply, measure, then keep or revert.
        assigned.put(p1.id(), new Allocation(p1, r2, newC1));
        assigned.put(p2.id(), new Allocation(p2, r1, newC2));
        int after = roommateSatisfaction(assigned, r1, r2);
        if (after > before) {
            return true;
        }
        // revert
        assigned.put(p1.id(), a1);
        assigned.put(p2.id(), a2);
        return false;
    }

    /** Count satisfied roommate requests among occupants of the two given rooms. */
    private int roommateSatisfaction(Map<String, Allocation> assigned, Room r1, Room r2) {
        int score = 0;
        for (Allocation a : assigned.values()) {
            Room room = a.room();
            if (!room.equals(r1) && !room.equals(r2)) {
                continue;
            }
            for (String wanted : a.participant().preference().preferredRoommates()) {
                Allocation other = assigned.get(wanted);
                if (other != null && other.room().equals(room)) {
                    score++;
                }
            }
        }
        return score;
    }

    private void validate(List<Participant> participants, List<Room> rooms)
            throws InvalidInputException {
        if (participants == null || rooms == null) {
            throw new InvalidInputException("participants and rooms must not be null");
        }
        Set<String> pids = new HashSet<>();
        for (Participant p : participants) {
            if (p == null) {
                throw new InvalidInputException("participant list contains null");
            }
            if (!pids.add(p.id())) {
                throw new InvalidInputException("duplicate participant id: " + p.id());
            }
        }
        Set<String> rids = new HashSet<>();
        for (Room r : rooms) {
            if (r == null) {
                throw new InvalidInputException("room list contains null");
            }
            if (r.capacity() < 0) {
                throw new InvalidInputException("room " + r.id() + " has negative capacity");
            }
            if (!rids.add(r.id())) {
                throw new InvalidInputException("duplicate room id: " + r.id());
            }
        }
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
