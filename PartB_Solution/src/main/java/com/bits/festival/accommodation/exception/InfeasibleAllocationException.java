package com.bits.festival.accommodation.exception;

/**
 * Thrown when no valid assignment can be produced at all — e.g. there are zero beds for a
 * non-empty participant list, so even the relaxed model has nothing to return. Note that a
 * <em>partial</em> allocation with a waitlist is a normal successful outcome and does
 * <strong>not</strong> raise this exception.
 */
public class InfeasibleAllocationException extends AccommodationException {
    private static final long serialVersionUID = 1L;

    public InfeasibleAllocationException(String message) {
        super(message);
    }
}
