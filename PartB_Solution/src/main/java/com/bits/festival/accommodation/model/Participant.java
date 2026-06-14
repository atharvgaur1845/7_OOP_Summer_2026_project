package com.bits.festival.accommodation.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * An outstation participant who needs hostel accommodation for the festival.
 *
 * <p>Immutable value object built via {@link Builder}. Equality is by {@link #id} so that
 * participants can be used as map keys and de-duplicated by the loaders.
 */
public final class Participant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Waitlist priority: higher categories get a room first when demand exceeds supply. */
    public enum Category {
        PERFORMER(0),  // headline acts / competing artists — seated first
        VIP(1),
        DELEGATE(2),
        ATTENDEE(3);   // general attendee — lowest priority

        private final int rank;

        Category(int rank) {
            this.rank = rank;
        }

        /** Lower is more important. */
        public int rank() {
            return rank;
        }

        public static Category from(String raw) {
            if (raw == null || raw.isBlank()) {
                return ATTENDEE;
            }
            return Category.valueOf(raw.trim().toUpperCase());
        }
    }

    private final String id;
    private final String name;
    private final Gender gender;
    private final String homeCity;
    private final double budgetPerNight;     // max ₹/night the participant is comfortable paying
    private final int arrivalDay;            // festival day index of arrival (used for waitlist tie-break)
    private final int nights;                // number of nights required (used for wallet billing)
    private final boolean needsAccessibleRoom;
    private final Category category;
    private final Preference preference;

    private Participant(Builder b) {
        this.id = Objects.requireNonNull(b.id, "participant id");
        this.name = b.name == null ? b.id : b.name;
        this.gender = b.gender == null ? Gender.OTHER : b.gender;
        this.homeCity = b.homeCity == null ? "" : b.homeCity;
        this.budgetPerNight = b.budgetPerNight;
        this.arrivalDay = b.arrivalDay;
        this.nights = Math.max(1, b.nights);
        this.needsAccessibleRoom = b.needsAccessibleRoom;
        this.category = b.category == null ? Category.ATTENDEE : b.category;
        this.preference = b.preference == null ? Preference.none() : b.preference;
    }

    public String id() { return id; }
    public String name() { return name; }
    public Gender gender() { return gender; }
    public String homeCity() { return homeCity; }
    public double budgetPerNight() { return budgetPerNight; }
    public int arrivalDay() { return arrivalDay; }
    public int nights() { return nights; }
    public boolean needsAccessibleRoom() { return needsAccessibleRoom; }
    public Category category() { return category; }
    public Preference preference() { return preference; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Participant)) return false;
        return id.equals(((Participant) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Participant{" + id + " '" + name + "', " + gender + ", " + category
                + ", budget=" + budgetPerNight + ", nights=" + nights
                + (needsAccessibleRoom ? ", accessible" : "") + '}';
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    /** Fluent builder for {@link Participant}. */
    public static final class Builder {
        private final String id;
        private String name;
        private Gender gender;
        private String homeCity;
        private double budgetPerNight = Double.MAX_VALUE; // no budget cap by default
        private int arrivalDay = 0;
        private int nights = 1;
        private boolean needsAccessibleRoom;
        private Category category;
        private Preference preference;

        public Builder(String id) { this.id = id; }

        public Builder name(String name) { this.name = name; return this; }
        public Builder gender(Gender g) { this.gender = g; return this; }
        public Builder homeCity(String c) { this.homeCity = c; return this; }
        public Builder budgetPerNight(double b) { this.budgetPerNight = b; return this; }
        public Builder arrivalDay(int d) { this.arrivalDay = d; return this; }
        public Builder nights(int n) { this.nights = n; return this; }
        public Builder needsAccessibleRoom(boolean v) { this.needsAccessibleRoom = v; return this; }
        public Builder category(Category c) { this.category = c; return this; }
        public Builder preference(Preference p) { this.preference = p; return this; }

        public Participant build() {
            return new Participant(this);
        }
    }
}
