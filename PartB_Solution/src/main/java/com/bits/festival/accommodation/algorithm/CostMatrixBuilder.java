package com.bits.festival.accommodation.algorithm;

import com.bits.festival.accommodation.cost.AllocationConfig;
import com.bits.festival.accommodation.cost.CostStrategy;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;
import com.bits.festival.accommodation.model.RoomSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Builds a square {@link CostMatrix} from participants and rooms, ready for the
 * {@link HungarianAlgorithm}.
 *
 * <p>Rooms are expanded into one {@link RoomSlot} per bed. The matrix is then padded to
 * {@code K = max(participants, slots)} with zero-cost dummy rows/columns. Each real
 * participant↔slot cell is scored by the supplied {@link CostStrategy}; a forbidden
 * ({@code +∞}) score is stored as the large finite sentinel from {@link AllocationConfig}.
 *
 * <p><strong>Concurrency:</strong> filling the matrix is the most expensive step (O(P·S)
 * strategy evaluations), so each participant's row is computed as an independent task on an
 * {@link ExecutorService}. The strategy contract requires thread-safety, making this a safe,
 * embarrassingly-parallel workload. The pool is created and shut down per build.
 */
public final class CostMatrixBuilder {

    private final CostStrategy strategy;
    private final int threads;

    public CostMatrixBuilder(CostStrategy strategy) {
        this(strategy, Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    public CostMatrixBuilder(CostStrategy strategy, int threads) {
        this.strategy = strategy;
        this.threads = Math.max(1, threads);
    }

    /**
     * @param participants real participants (rows)
     * @param rooms        rooms, expanded into capacity-many slots (columns)
     * @return a padded square cost matrix with row/column ↔ domain mappings
     */
    public CostMatrix build(List<Participant> participants, List<Room> rooms) {
        final List<RoomSlot> slots = expandToSlots(rooms);
        final int p = participants.size();
        final int s = slots.size();
        final int k = Math.max(p, s);
        final double forbidden = AllocationConfig.getInstance().hardConstraintPenalty();

        final double[][] matrix = new double[k][k]; // zero-initialised → dummy cells are 0

        // Compute one row per participant in parallel.
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            final List<Future<?>> futures = new ArrayList<>(p);
            for (int i = 0; i < p; i++) {
                final int row = i;
                final Participant participant = participants.get(i);
                futures.add(pool.submit(() -> fillRow(matrix[row], participant, slots, forbidden)));
            }
            for (Future<?> f : futures) {
                f.get(); // propagate any computation failure
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Cost matrix build interrupted", ie);
        } catch (java.util.concurrent.ExecutionException ee) {
            throw new RuntimeException("Cost matrix build failed", ee.getCause());
        } finally {
            pool.shutdownNow();
        }

        return new CostMatrix(matrix, participants, slots, forbidden);
    }

    private void fillRow(double[] rowCosts, Participant participant,
                         List<RoomSlot> slots, double forbidden) {
        // Constant per-row seating bias: when beds are scarce the solver prefers to seat
        // higher-priority participants (lower category rank) and push lower-priority ones to a
        // dummy column (the waitlist). Because it is identical across all of this row's real
        // beds, it never changes WHICH room a seated participant gets — only seat-vs-waitlist.
        double bias = participant.category().rank()
                * AllocationConfig.getInstance().priorityBiasPerRank();
        for (int j = 0; j < slots.size(); j++) {
            double c = strategy.cost(participant, slots.get(j).room());
            // Keep feasible cells strictly below the forbidden sentinel so they stay seatable.
            rowCosts[j] = Double.isInfinite(c) ? forbidden : Math.min(c + bias, forbidden - 1.0);
        }
        // remaining cells (dummy slot columns) stay 0.0
    }

    /** Expand each room of capacity k into k {@link RoomSlot}s. */
    public static List<RoomSlot> expandToSlots(List<Room> rooms) {
        final List<RoomSlot> slots = new ArrayList<>();
        for (Room room : rooms) {
            for (int i = 0; i < room.capacity(); i++) {
                slots.add(new RoomSlot(room, i));
            }
        }
        return slots;
    }
}
