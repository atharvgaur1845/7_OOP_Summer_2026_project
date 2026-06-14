package com.bits.festival.accommodation.cost;

import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;

/**
 * Strategy (GoF) for scoring how undesirable it is to place a participant in a room. Lower
 * is better; {@code 0} is a perfect match. A return value of {@link Double#POSITIVE_INFINITY}
 * means the pairing is <em>forbidden</em> by a hard constraint (gender policy, accessibility)
 * and must never appear in a valid allocation.
 *
 * <p>Implementations must be stateless / thread-safe: {@code CostMatrixBuilder} invokes
 * {@link #cost} concurrently from many threads while filling the matrix.
 */
@FunctionalInterface
public interface CostStrategy {

    /**
     * @return the dissatisfaction penalty for seating {@code participant} in {@code room},
     *         or {@link Double#POSITIVE_INFINITY} if the pairing violates a hard constraint.
     */
    double cost(Participant participant, Room room);

    /** Whether a computed cost denotes a forbidden (hard-constraint-violating) pairing. */
    default boolean isForbidden(double cost) {
        return Double.isInfinite(cost) || cost >= AllocationConfig.getInstance().hardConstraintPenalty();
    }
}
