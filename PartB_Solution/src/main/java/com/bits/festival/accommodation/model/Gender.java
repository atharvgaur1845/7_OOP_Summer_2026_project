package com.bits.festival.accommodation.model;

/**
 * Gender of a participant and the gender policy a room enforces.
 *
 * <p>{@link #ANY} is only meaningful as a <em>room policy</em> (a co-ed / unrestricted
 * room). A participant is always one of {@link #MALE}, {@link #FEMALE} or {@link #OTHER}.
 */
public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    /** Room policy only: the room accepts participants of any gender. */
    ANY;

    /**
     * @param participant the participant's gender
     * @return {@code true} if a room with {@code this} policy may host the participant
     */
    public boolean accepts(Gender participant) {
        return this == ANY || this == participant;
    }

    /** Lenient parse used by the CSV/JSON loaders. */
    public static Gender from(String raw) {
        if (raw == null) {
            return ANY;
        }
        switch (raw.trim().toUpperCase()) {
            case "M":
            case "MALE":
                return MALE;
            case "F":
            case "FEMALE":
                return FEMALE;
            case "O":
            case "OTHER":
                return OTHER;
            case "":
            case "ANY":
            case "MIXED":
            case "COED":
            case "CO-ED":
                return ANY;
            default:
                return Gender.valueOf(raw.trim().toUpperCase());
        }
    }
}
