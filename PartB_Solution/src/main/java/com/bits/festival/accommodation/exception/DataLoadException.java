package com.bits.festival.accommodation.exception;

/**
 * Thrown when input data cannot be read or parsed — missing file, malformed CSV/JSON,
 * unsupported file extension, or an I/O failure while writing results.
 */
public class DataLoadException extends AccommodationException {
    private static final long serialVersionUID = 1L;

    public DataLoadException(String message) {
        super(message);
    }

    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
