package com.bits.festival.accommodation.service;

import com.bits.festival.accommodation.cost.AllocationConfig;
import com.bits.festival.accommodation.exception.InfeasibleAllocationException;
import com.bits.festival.accommodation.exception.InvalidInputException;
import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Preference;
import com.bits.festival.accommodation.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end tests for the allocation pipeline. */
class AccommodationAllocatorTest {

    private final AccommodationAllocator allocator = new AccommodationAllocator();

    @BeforeEach
    void resetConfig() {
        AllocationConfig.getInstance().resetDefaults();
    }

    @Test
    void everyoneSeatedWhenSupplyEqualsDemandAndAllFeasible() throws Exception {
        List<Participant> participants = List.of(
                Participant.builder("P1").gender(Gender.MALE).build(),
                Participant.builder("P2").gender(Gender.MALE).build());
        List<Room> rooms = List.of(
                Room.builder("R1").genderPolicy(Gender.MALE).capacity(2).pricePerNight(100).build());

        AllocationResult r = allocator.allocate(participants, rooms);
        assertEquals(2, r.metrics().assignedCount());
        assertEquals(0, r.metrics().waitlistedCount());
        assertEquals(0, r.metrics().hardConstraintViolations());
        assertEquals(1.0, r.metrics().placementRate(), 1e-9);
    }

    @Test
    void surplusParticipantsAreWaitlistedByPriority() throws Exception {
        List<Participant> participants = List.of(
                Participant.builder("ATT").gender(Gender.MALE)
                        .category(Participant.Category.ATTENDEE).arrivalDay(5).build(),
                Participant.builder("PERF").gender(Gender.MALE)
                        .category(Participant.Category.PERFORMER).arrivalDay(0).build());
        // Only one bed: the performer should win it, the attendee is waitlisted.
        List<Room> rooms = List.of(
                Room.builder("R1").genderPolicy(Gender.MALE).capacity(1).build());

        AllocationResult r = allocator.allocate(participants, rooms);
        assertEquals(1, r.metrics().assignedCount());
        assertEquals(1, r.waitlist().size());
        assertEquals("ATT", r.waitlist().get(0).id());
        assertEquals("PERF", r.allocations().get(0).participant().id());
    }

    @Test
    void surplusBedsRemainEmptyAndEveryoneIsSeated() throws Exception {
        List<Participant> participants = List.of(
                Participant.builder("P1").gender(Gender.FEMALE).build());
        List<Room> rooms = List.of(
                Room.builder("R1").genderPolicy(Gender.FEMALE).capacity(3).build());

        AllocationResult r = allocator.allocate(participants, rooms);
        assertEquals(1, r.metrics().assignedCount());
        assertEquals(0, r.waitlist().size());
        assertEquals(3, r.metrics().totalSlots());
    }

    @Test
    void respectsAccessibilityHardConstraint() throws Exception {
        Participant needsAccess = Participant.builder("ACC").gender(Gender.MALE)
                .needsAccessibleRoom(true).build();
        Participant other = Participant.builder("OTH").gender(Gender.MALE).build();
        Room accessible = Room.builder("ACCROOM").genderPolicy(Gender.MALE).capacity(1)
                .accessible(true).build();
        Room normal = Room.builder("NORMAL").genderPolicy(Gender.MALE).capacity(1)
                .accessible(false).build();

        AllocationResult r = allocator.allocate(List.of(needsAccess, other), List.of(normal, accessible));
        assertEquals(2, r.metrics().assignedCount());
        assertEquals(0, r.metrics().hardConstraintViolations());
        // The accessibility-needing participant must be in the accessible room.
        String accRoom = r.assignmentsById().get("ACC").id();
        assertEquals("ACCROOM", accRoom);
    }

    @Test
    void waitlistsParticipantWithNoFeasibleRoom() throws Exception {
        // A female with only a male room available cannot be placed.
        Participant female = Participant.builder("F").gender(Gender.FEMALE).build();
        Room maleRoom = Room.builder("M").genderPolicy(Gender.MALE).capacity(1).build();

        AllocationResult r = allocator.allocate(List.of(female), List.of(maleRoom));
        assertEquals(0, r.metrics().assignedCount());
        assertEquals(1, r.waitlist().size());
        assertEquals(0, r.metrics().hardConstraintViolations());
    }

    @Test
    void roommateRequestsAreColocatedWhenCostNeutral() throws Exception {
        // Two participants want to share; two identical 1-bed rooms would split them, but a
        // 2-bed room lets the nudge put them together. Use one 2-bed room.
        Participant a = Participant.builder("A").gender(Gender.MALE)
                .preference(new Preference.Builder().roommate("B").build()).build();
        Participant b = Participant.builder("B").gender(Gender.MALE)
                .preference(new Preference.Builder().roommate("A").build()).build();
        Room shared = Room.builder("SHARED").genderPolicy(Gender.MALE).capacity(2).build();
        Room single = Room.builder("SINGLE").genderPolicy(Gender.MALE).capacity(1).build();

        AllocationResult r = allocator.allocate(List.of(a, b), List.of(single, shared));
        // Both seated, and A's roommate list should include B (same room).
        assertEquals(2, r.metrics().assignedCount());
        assertTrue(r.roommatesOf("A").stream().anyMatch(p -> p.id().equals("B")),
                "A and B should share a room when capacity allows");
    }

    @Test
    void emptyParticipantsYieldEmptyResult() throws Exception {
        AllocationResult r = allocator.allocate(new ArrayList<>(),
                List.of(Room.builder("R").capacity(2).build()));
        assertEquals(0, r.metrics().assignedCount());
        assertEquals(0, r.waitlist().size());
    }

    @Test
    void duplicateParticipantIdRejected() {
        List<Participant> dupes = List.of(
                Participant.builder("X").build(),
                Participant.builder("X").build());
        List<Room> rooms = List.of(Room.builder("R").capacity(2).build());
        assertThrows(InvalidInputException.class, () -> allocator.allocate(dupes, rooms));
    }

    @Test
    void negativeCapacityRejected() {
        List<Participant> ps = List.of(Participant.builder("P").build());
        List<Room> rooms = List.of(Room.builder("R").capacity(-1).build());
        assertThrows(InvalidInputException.class, () -> allocator.allocate(ps, rooms));
    }

    @Test
    void zeroBedsForParticipantsIsInfeasible() {
        List<Participant> ps = List.of(Participant.builder("P").build());
        List<Room> rooms = List.of(Room.builder("R").capacity(0).build());
        assertThrows(InfeasibleAllocationException.class, () -> allocator.allocate(ps, rooms));
    }
}
