package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Preference;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@link Participant}s from a CSV file produced by the festival's Accommodation module.
 *
 * <p>Expected header (order-independent; extra columns ignored):
 * <pre>
 * id,name,gender,homeCity,budgetPerNight,arrivalDay,nights,needsAccessible,category,
 * prefBuilding,prefRoomType,prefRoommates
 * </pre>
 * {@code prefRoommates} is a {@code ;}-separated list of participant IDs. Uses
 * try-with-resources around a {@link BufferedReader} for safe, buffered streaming I/O.
 */
public final class ParticipantCsvRepository implements Repository<Participant> {

    private final Path file;

    public ParticipantCsvRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Participant> loadAll() throws DataLoadException {
        final List<Participant> participants = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String headerLine = nextNonBlank(reader);
            if (headerLine == null) {
                return participants; // empty file → no participants
            }
            Map<String, Integer> col = indexHeader(CsvUtil.parseLine(headerLine));
            requireColumn(col, "id");

            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                List<String> f = CsvUtil.parseLine(line);
                try {
                    participants.add(toParticipant(col, f));
                } catch (RuntimeException ex) {
                    throw new DataLoadException(
                            "Malformed participant row at line " + lineNo + " in " + file
                                    + ": " + ex.getMessage(), ex);
                }
            }
        } catch (IOException e) {
            throw new DataLoadException("Failed to read participants from " + file, e);
        }
        return participants;
    }

    private Participant toParticipant(Map<String, Integer> col, List<String> f) {
        Preference pref = new Preference.Builder()
                .building(get(col, f, "prefBuilding"))
                .roomType(get(col, f, "prefRoomType"))
                .roommates(CsvUtil.splitMulti(get(col, f, "prefRoommates")))
                .build();

        return Participant.builder(required(get(col, f, "id"), "id"))
                .name(get(col, f, "name"))
                .gender(Gender.from(get(col, f, "gender")))
                .homeCity(get(col, f, "homeCity"))
                .budgetPerNight(parseDouble(get(col, f, "budgetPerNight"), Double.MAX_VALUE))
                .arrivalDay((int) parseDouble(get(col, f, "arrivalDay"), 0))
                .nights((int) parseDouble(get(col, f, "nights"), 1))
                .needsAccessibleRoom(parseBool(get(col, f, "needsAccessible")))
                .category(Participant.Category.from(get(col, f, "category")))
                .preference(pref)
                .build();
    }

    // ---- small parsing helpers shared in spirit with RoomCsvRepository ----

    static Map<String, Integer> indexHeader(List<String> header) {
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            col.put(header.get(i).trim().toLowerCase(), i);
        }
        return col;
    }

    static void requireColumn(Map<String, Integer> col, String name) throws DataLoadException {
        if (!col.containsKey(name.toLowerCase())) {
            throw new DataLoadException("CSV is missing required column: " + name);
        }
    }

    static String get(Map<String, Integer> col, List<String> fields, String name) {
        Integer idx = col.get(name.toLowerCase());
        if (idx == null || idx >= fields.size()) {
            return "";
        }
        return fields.get(idx);
    }

    static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required value: " + name);
        }
        return value;
    }

    static double parseDouble(String s, double fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        return Double.parseDouble(s.trim());
    }

    static boolean parseBool(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim().toLowerCase();
        return t.equals("true") || t.equals("yes") || t.equals("y") || t.equals("1");
    }

    private static String nextNonBlank(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                return line;
            }
        }
        return null;
    }
}
