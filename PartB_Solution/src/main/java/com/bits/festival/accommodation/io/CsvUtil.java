package com.bits.festival.accommodation.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal RFC-4180-ish CSV helpers shared by the CSV repositories and writer. Supports
 * double-quoted fields (so a value may contain commas) and {@code ""} as an escaped quote.
 * Multi-value cells (roommate lists, amenities) use {@code ';'} as an inner separator so they
 * never clash with the field delimiter.
 */
public final class CsvUtil {

    public static final char DELIMITER = ',';
    public static final String MULTI_VALUE_SEPARATOR = ";";

    private CsvUtil() {
    }

    /** Parse one CSV line into trimmed fields, honouring double-quoted segments. */
    public static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"'); // escaped quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == DELIMITER) {
                out.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        out.add(field.toString().trim());
        return out;
    }

    /** Quote a field for output if it contains the delimiter, a quote, or a newline. */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.indexOf(DELIMITER) >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0;
        if (!needsQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Join multiple values into a single multi-value cell. */
    public static String joinMulti(Iterable<String> values) {
        return String.join(MULTI_VALUE_SEPARATOR, toList(values));
    }

    /** Split a multi-value cell into its parts (empty cell → empty list). */
    public static List<String> splitMulti(String cell) {
        List<String> out = new ArrayList<>();
        if (cell == null || cell.isBlank()) {
            return out;
        }
        for (String part : cell.split(MULTI_VALUE_SEPARATOR)) {
            String t = part.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static List<String> toList(Iterable<String> values) {
        List<String> list = new ArrayList<>();
        for (String v : values) {
            list.add(v);
        }
        return list;
    }
}
