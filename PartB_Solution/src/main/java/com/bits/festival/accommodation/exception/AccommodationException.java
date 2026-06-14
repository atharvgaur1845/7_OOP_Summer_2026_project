package com.bits.festival.accommodation.exception;

/**
 * Base type for every checked failure raised by the accommodation engine. Callers can catch
 * this single type to handle any domain error, or catch the specific subclasses
 * ({@link InvalidInputException}, {@link DataLoadException}, {@link InfeasibleAllocationException})
 * for finer-grained handling.
 */
public class AccommodationException extends Exception {
    private static final long serialVersionUID = 1L;

    public AccommodationException(String message) {
        super(message);
    }

    public AccommodationException(String message, Throwable cause) {
        super(message, cause);
    }
}
