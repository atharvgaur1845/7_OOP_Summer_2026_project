package com.bits.festival.accommodation.model;

import java.io.Serializable;

/**
 * Success metrics describing the quality of an allocation run. These map directly to the
 * "success metrics" promised in the Part A formulation: placement rate, total/average
 * dissatisfaction, hard-constraint violations (must be zero), bed utilisation and runtime.
 */
public final class Metrics implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int totalParticipants;
    private final int assignedCount;
    private final int waitlistedCount;
    private final int totalSlots;
    private final double totalCost;
    private final int hardConstraintViolations;
    private final long runtimeMillis;

    public Metrics(int totalParticipants, int assignedCount, int waitlistedCount,
                   int totalSlots, double totalCost, int hardConstraintViolations,
                   long runtimeMillis) {
        this.totalParticipants = totalParticipants;
        this.assignedCount = assignedCount;
        this.waitlistedCount = waitlistedCount;
        this.totalSlots = totalSlots;
        this.totalCost = totalCost;
        this.hardConstraintViolations = hardConstraintViolations;
        this.runtimeMillis = runtimeMillis;
    }

    public int totalParticipants() { return totalParticipants; }
    public int assignedCount() { return assignedCount; }
    public int waitlistedCount() { return waitlistedCount; }
    public int totalSlots() { return totalSlots; }
    public double totalCost() { return totalCost; }
    public int hardConstraintViolations() { return hardConstraintViolations; }
    public long runtimeMillis() { return runtimeMillis; }

    /** Fraction of participants who received a room (0..1). */
    public double placementRate() {
        return totalParticipants == 0 ? 1.0 : (double) assignedCount / totalParticipants;
    }

    /** Mean dissatisfaction over assigned participants. */
    public double averageCost() {
        return assignedCount == 0 ? 0.0 : totalCost / assignedCount;
    }

    /** Fraction of beds that ended up occupied (0..1). */
    public double bedUtilisation() {
        return totalSlots == 0 ? 0.0 : (double) assignedCount / totalSlots;
    }

    @Override
    public String toString() {
        return String.format(
                "Metrics{placed=%d/%d (%.1f%%), waitlisted=%d, beds=%d (util %.1f%%), "
                        + "totalCost=%.2f, avgCost=%.2f, hardViolations=%d, runtime=%dms}",
                assignedCount, totalParticipants, placementRate() * 100,
                waitlistedCount, totalSlots, bedUtilisation() * 100,
                totalCost, averageCost(), hardConstraintViolations, runtimeMillis);
    }
}
