package com.bits.festival.accommodation.cost;

import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Preference;
import com.bits.festival.accommodation.model.Room;

/**
 * Default cost model. Hard constraints return {@code +∞}; otherwise the cost is a weighted
 * sum of missed soft preferences, with weights read from the {@link AllocationConfig}
 * singleton so they can be tuned live from the GUI.
 *
 * <h3>Hard constraints (forbidden → +∞)</h3>
 * <ul>
 *   <li>Gender policy: the room must {@link com.bits.festival.accommodation.model.Gender#accepts accept}
 *       the participant's gender.</li>
 *   <li>Accessibility: a participant who needs an accessible room must get an accessible room.</li>
 * </ul>
 *
 * <h3>Soft penalties (added together)</h3>
 * <ul>
 *   <li>Budget overflow: {@code (price - budget) × budgetOverflowWeight} when over budget.</li>
 *   <li>Building mismatch / room-type mismatch: flat penalties when the preference is unmet.</li>
 * </ul>
 *
 * <p>Roommate-grouping is intentionally <em>not</em> scored here: it depends on where the
 * other participants land, which a per-cell cost cannot see. It is handled as a soft
 * post-processing nudge by the allocator; see the report's modelling notes.
 */
public final class DefaultCostStrategy implements CostStrategy {

    private final AllocationConfig config;

    public DefaultCostStrategy() {
        this(AllocationConfig.getInstance());
    }

    public DefaultCostStrategy(AllocationConfig config) {
        this.config = config;
    }

    @Override
    public double cost(Participant p, Room r) {
        // ---- Hard constraints ----
        if (!r.genderPolicy().accepts(p.gender())) {
            return Double.POSITIVE_INFINITY;
        }
        if (p.needsAccessibleRoom() && !r.accessible()) {
            return Double.POSITIVE_INFINITY;
        }

        // ---- Soft penalties ----
        double cost = 0.0;

        if (p.budgetPerNight() != Double.MAX_VALUE && r.pricePerNight() > p.budgetPerNight()) {
            cost += (r.pricePerNight() - p.budgetPerNight()) * config.budgetOverflowWeight();
        }

        Preference pref = p.preference();
        if (pref.hasBuildingPreference()
                && !pref.preferredBuilding().equalsIgnoreCase(r.building())) {
            cost += config.buildingMismatchPenalty();
        }
        if (pref.hasRoomTypePreference()
                && !pref.preferredRoomType().equalsIgnoreCase(r.roomType())) {
            cost += config.roomTypeMismatchPenalty();
        }

        return cost;
    }
}
