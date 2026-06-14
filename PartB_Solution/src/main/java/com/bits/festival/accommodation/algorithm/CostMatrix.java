package com.bits.festival.accommodation.algorithm;

import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.RoomSlot;

import java.util.List;

/**
 * A square cost matrix ready for {@link HungarianAlgorithm}, together with the mapping from
 * matrix rows/columns back to domain objects.
 *
 * <p>Layout (size {@code K = max(participants, slots)}):
 * <ul>
 *   <li>Rows {@code 0..P-1} are real participants ({@link #participants}); rows {@code P..K-1}
 *       are dummy participants (zero cost everywhere) that let surplus beds stay empty.</li>
 *   <li>Columns {@code 0..S-1} are real {@link #slots}; columns {@code S..K-1} are dummy beds
 *       (zero cost) — a real participant matched to one of these is unassigned → waitlisted.</li>
 * </ul>
 * Forbidden (hard-constraint) pairings are stored as the large finite sentinel
 * {@code forbiddenValue} rather than {@code +∞} so the solver stays numerically stable.
 */
public final class CostMatrix {

    private final double[][] matrix;
    private final List<Participant> participants;
    private final List<RoomSlot> slots;
    private final double forbiddenValue;

    public CostMatrix(double[][] matrix, List<Participant> participants,
                      List<RoomSlot> slots, double forbiddenValue) {
        this.matrix = matrix;
        this.participants = participants;
        this.slots = slots;
        this.forbiddenValue = forbiddenValue;
    }

    public double[][] matrix() {
        return matrix;
    }

    public int size() {
        return matrix.length;
    }

    public List<Participant> participants() {
        return participants;
    }

    public List<RoomSlot> slots() {
        return slots;
    }

    public int participantCount() {
        return participants.size();
    }

    public int slotCount() {
        return slots.size();
    }

    public boolean isRealParticipantRow(int row) {
        return row < participants.size();
    }

    public boolean isRealSlotColumn(int col) {
        return col < slots.size();
    }

    /** Whether the value at {@code (row,col)} denotes a forbidden pairing. */
    public boolean isForbidden(int row, int col) {
        return matrix[row][col] >= forbiddenValue;
    }
}
