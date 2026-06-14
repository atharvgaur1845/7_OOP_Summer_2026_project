package com.bits.festival.accommodation.cost;

import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Preference;
import com.bits.festival.accommodation.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the default cost model: hard constraints and soft penalties. */
class DefaultCostStrategyTest {

    private final CostStrategy strategy = new DefaultCostStrategy();

    @BeforeEach
    void resetConfig() {
        AllocationConfig.getInstance().resetDefaults();
    }

    @Test
    void genderMismatchIsForbidden() {
        Participant male = Participant.builder("P").gender(Gender.MALE).build();
        Room female = Room.builder("R").genderPolicy(Gender.FEMALE).build();
        assertTrue(Double.isInfinite(strategy.cost(male, female)));
    }

    @Test
    void anyRoomAcceptsEveryGender() {
        Participant other = Participant.builder("P").gender(Gender.OTHER).build();
        Room any = Room.builder("R").genderPolicy(Gender.ANY).build();
        assertEquals(0.0, strategy.cost(other, any), 1e-9);
    }

    @Test
    void accessibilityNeedIsForbiddenInNonAccessibleRoom() {
        Participant needsAccess = Participant.builder("P").gender(Gender.MALE)
                .needsAccessibleRoom(true).build();
        Room notAccessible = Room.builder("R").genderPolicy(Gender.ANY).accessible(false).build();
        assertTrue(Double.isInfinite(strategy.cost(needsAccess, notAccessible)));
    }

    @Test
    void perfectMatchCostsZero() {
        Participant p = Participant.builder("P").gender(Gender.MALE).budgetPerNight(1000)
                .preference(new Preference.Builder().building("H1").roomType("AC").build()).build();
        Room r = Room.builder("R").genderPolicy(Gender.MALE).pricePerNight(800)
                .building("H1").roomType("AC").build();
        assertEquals(0.0, strategy.cost(p, r), 1e-9);
    }

    @Test
    void budgetOverflowAddsScaledPenalty() {
        AllocationConfig.getInstance().setBudgetOverflowWeight(0.5);
        Participant p = Participant.builder("P").gender(Gender.MALE)
                .budgetPerNight(500).build();
        Room r = Room.builder("R").genderPolicy(Gender.MALE).pricePerNight(700).build();
        // (700 - 500) * 0.5 = 100
        assertEquals(100.0, strategy.cost(p, r), 1e-9);
    }

    @Test
    void buildingAndRoomTypeMismatchPenaltiesAdd() {
        AllocationConfig cfg = AllocationConfig.getInstance();
        Participant p = Participant.builder("P").gender(Gender.MALE).budgetPerNight(10_000)
                .preference(new Preference.Builder().building("H1").roomType("AC").build()).build();
        Room r = Room.builder("R").genderPolicy(Gender.MALE).pricePerNight(100)
                .building("H2").roomType("NON_AC").build();
        assertEquals(cfg.buildingMismatchPenalty() + cfg.roomTypeMismatchPenalty(),
                strategy.cost(p, r), 1e-9);
    }
}
