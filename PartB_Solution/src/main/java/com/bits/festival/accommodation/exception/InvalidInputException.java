package com.bits.festival.accommodation.exception;

/**
 * Thrown when the supplied participants/rooms are structurally invalid — e.g. duplicate IDs,
 * negative capacity, or empty input where data is required.
 */
public class InvalidInputException extends AccommodationException {
    private static final long serialVersionUID = 1L;

    public InvalidInputException(String message) {
        super(message);
    }
}
