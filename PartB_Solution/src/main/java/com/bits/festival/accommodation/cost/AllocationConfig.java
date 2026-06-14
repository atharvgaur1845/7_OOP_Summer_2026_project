package com.bits.festival.accommodation.cost;

/**
 * Tunable weights and sentinels for the cost model, exposed as a process-wide
 * <strong>Singleton</strong> (lazy, thread-safe via the initialization-on-demand holder
 * idiom). The GUI/CLI mutate these weights and every {@link CostStrategy} reads them, so a
 * single shared instance keeps the whole engine consistent.
 *
 * <p>All weights are "penalty points" added to a participant↔room pairing when a soft
 * preference is missed. {@link #hardConstraintPenalty()} is the large finite sentinel used
 * in place of {@code +∞} so the Hungarian solver stays numerically well-behaved; any real
 * pairing whose cost is {@code >= } this value is treated as forbidden.
 */
public final class AllocationConfig {

    /** Initialization-on-demand holder — thread-safe, lazy, no synchronization cost. */
    private static final class Holder {
        private static final AllocationConfig INSTANCE = new AllocationConfig();
    }

    public static AllocationConfig getInstance() {
        return Holder.INSTANCE;
    }

    // Soft-preference penalties (per missed preference).
    private volatile double buildingMismatchPenalty = 20.0;
    private volatile double roomTypeMismatchPenalty = 15.0;
    private volatile double roommateSeparationPenalty = 25.0; // per requested roommate not co-located
    // Budget overflow is scaled by how far over budget the room is (per ₹/night over).
    private volatile double budgetOverflowWeight = 0.5;
    // The large finite stand-in for +infinity (forbidden assignment).
    private volatile double hardConstraintPenalty = 1_000_000.0;
    // Per-rank seating bias added (only inside the solver's matrix) so that, when beds are
    // scarce, higher-priority categories (lower rank) are seated and lower-priority ones are
    // waitlisted. Large enough to dominate soft penalties; far below the hard sentinel. Being
    // constant per participant, it never affects which room a seated participant receives.
    private volatile double priorityBiasPerRank = 1_000.0;

    private AllocationConfig() {
    }

    public double buildingMismatchPenalty() { return buildingMismatchPenalty; }
    public double roomTypeMismatchPenalty() { return roomTypeMismatchPenalty; }
    public double roommateSeparationPenalty() { return roommateSeparationPenalty; }
    public double budgetOverflowWeight() { return budgetOverflowWeight; }
    public double hardConstraintPenalty() { return hardConstraintPenalty; }
    public double priorityBiasPerRank() { return priorityBiasPerRank; }

    public void setBuildingMismatchPenalty(double v) { this.buildingMismatchPenalty = v; }
    public void setRoomTypeMismatchPenalty(double v) { this.roomTypeMismatchPenalty = v; }
    public void setRoommateSeparationPenalty(double v) { this.roommateSeparationPenalty = v; }
    public void setBudgetOverflowWeight(double v) { this.budgetOverflowWeight = v; }
    public void setHardConstraintPenalty(double v) { this.hardConstraintPenalty = v; }
    public void setPriorityBiasPerRank(double v) { this.priorityBiasPerRank = v; }

    /** Restore documented defaults (used by tests to avoid cross-test contamination). */
    public void resetDefaults() {
        buildingMismatchPenalty = 20.0;
        roomTypeMismatchPenalty = 15.0;
        roommateSeparationPenalty = 25.0;
        budgetOverflowWeight = 0.5;
        hardConstraintPenalty = 1_000_000.0;
        priorityBiasPerRank = 1_000.0;
    }
}
