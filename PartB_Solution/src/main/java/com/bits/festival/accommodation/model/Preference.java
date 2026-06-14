package com.bits.festival.accommodation.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Soft preferences a participant expresses about their accommodation. None of these are
 * hard constraints — they only influence the <em>cost</em> (dissatisfaction) of an
 * assignment. Built with a small fluent {@link Builder} (Builder pattern) because most
 * fields are optional and supplied piecemeal by the file loaders.
 */
public final class Preference implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String preferredBuilding; // null = no preference
    private final String preferredRoomType; // e.g. "AC", "NON_AC", "DORM"; null = no preference
    /** IDs of participants this person would like to share a room with (roommate group). */
    private final Set<String> preferredRoommates;

    private Preference(Builder b) {
        this.preferredBuilding = b.preferredBuilding;
        this.preferredRoomType = b.preferredRoomType;
        this.preferredRoommates = Collections.unmodifiableSet(new LinkedHashSet<>(b.preferredRoommates));
    }

    /** A preference object that expresses nothing (neutral cost). */
    public static Preference none() {
        return new Builder().build();
    }

    public String preferredBuilding() {
        return preferredBuilding;
    }

    public String preferredRoomType() {
        return preferredRoomType;
    }

    public Set<String> preferredRoommates() {
        return preferredRoommates;
    }

    public boolean hasBuildingPreference() {
        return preferredBuilding != null && !preferredBuilding.isBlank();
    }

    public boolean hasRoomTypePreference() {
        return preferredRoomType != null && !preferredRoomType.isBlank();
    }

    @Override
    public String toString() {
        return "Preference{building=" + preferredBuilding
                + ", roomType=" + preferredRoomType
                + ", roommates=" + preferredRoommates + '}';
    }

    /** Fluent builder for {@link Preference}. */
    public static final class Builder {
        private String preferredBuilding;
        private String preferredRoomType;
        private final Set<String> preferredRoommates = new LinkedHashSet<>();

        public Builder building(String building) {
            this.preferredBuilding = building;
            return this;
        }

        public Builder roomType(String roomType) {
            this.preferredRoomType = roomType;
            return this;
        }

        public Builder roommate(String participantId) {
            if (participantId != null && !participantId.isBlank()) {
                this.preferredRoommates.add(participantId.trim());
            }
            return this;
        }

        public Builder roommates(Iterable<String> ids) {
            if (ids != null) {
                ids.forEach(this::roommate);
            }
            return this;
        }

        public Preference build() {
            return new Preference(this);
        }
    }
}
