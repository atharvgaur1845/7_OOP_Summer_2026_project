package com.bits.festival.accommodation.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A hostel room offered by the festival's Accommodation module. A room provides
 * {@link #capacity} beds ("slots"), enforces a {@link Gender} policy, and may be
 * accessibility-equipped. Immutable; equality by {@link #id}.
 */
public final class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String building;
    private final int floor;
    private final int capacity;            // number of beds/slots
    private final Gender genderPolicy;     // MALE / FEMALE / ANY
    private final double pricePerNight;
    private final boolean accessible;      // ground-floor / ramp / accessible washroom
    private final String roomType;         // e.g. "AC", "NON_AC", "DORM"
    private final Set<String> amenities;

    private Room(Builder b) {
        this.id = Objects.requireNonNull(b.id, "room id");
        this.building = b.building == null ? "" : b.building;
        this.floor = b.floor;
        this.capacity = b.capacity;
        this.genderPolicy = b.genderPolicy == null ? Gender.ANY : b.genderPolicy;
        this.pricePerNight = b.pricePerNight;
        this.accessible = b.accessible;
        this.roomType = b.roomType == null ? "" : b.roomType;
        this.amenities = Collections.unmodifiableSet(new LinkedHashSet<>(b.amenities));
    }

    public String id() { return id; }
    public String building() { return building; }
    public int floor() { return floor; }
    public int capacity() { return capacity; }
    public Gender genderPolicy() { return genderPolicy; }
    public double pricePerNight() { return pricePerNight; }
    public boolean accessible() { return accessible; }
    public String roomType() { return roomType; }
    public Set<String> amenities() { return amenities; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        return id.equals(((Room) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Room{" + id + " @" + building + " f" + floor + ", cap=" + capacity
                + ", " + genderPolicy + ", ₹" + pricePerNight + "/night, " + roomType
                + (accessible ? ", accessible" : "") + '}';
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Fluent builder for {@link Room}. */
    public static final class Builder {
        private final String id;
        private String building;
        private int floor;
        private int capacity = 1;
        private Gender genderPolicy;
        private double pricePerNight;
        private boolean accessible;
        private String roomType;
        private final Set<String> amenities = new LinkedHashSet<>();

        public Builder(String id) { this.id = id; }

        public Builder building(String b) { this.building = b; return this; }
        public Builder floor(int f) { this.floor = f; return this; }
        public Builder capacity(int c) { this.capacity = c; return this; }
        public Builder genderPolicy(Gender g) { this.genderPolicy = g; return this; }
        public Builder pricePerNight(double p) { this.pricePerNight = p; return this; }
        public Builder accessible(boolean a) { this.accessible = a; return this; }
        public Builder roomType(String t) { this.roomType = t; return this; }

        public Builder amenity(String a) {
            if (a != null && !a.isBlank()) {
                this.amenities.add(a.trim());
            }
            return this;
        }

        public Builder amenities(Iterable<String> as) {
            if (as != null) {
                as.forEach(this::amenity);
            }
            return this;
        }

        public Room build() {
            return new Room(this);
        }
    }
}
