package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Room;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.bits.festival.accommodation.io.ParticipantCsvRepository.get;
import static com.bits.festival.accommodation.io.ParticipantCsvRepository.indexHeader;
import static com.bits.festival.accommodation.io.ParticipantCsvRepository.parseBool;
import static com.bits.festival.accommodation.io.ParticipantCsvRepository.parseDouble;
import static com.bits.festival.accommodation.io.ParticipantCsvRepository.required;
import static com.bits.festival.accommodation.io.ParticipantCsvRepository.requireColumn;

/**
 * Loads {@link Room}s from a CSV file produced by the festival's Accommodation module.
 *
 * <p>Expected header (order-independent; extra columns ignored):
 * <pre>
 * id,building,floor,capacity,genderPolicy,pricePerNight,accessible,roomType,amenities
 * </pre>
 * {@code amenities} is a {@code ;}-separated list.
 */
public final class RoomCsvRepository implements Repository<Room> {

    private final Path file;

    public RoomCsvRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Room> loadAll() throws DataLoadException {
        final List<Room> rooms = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            while (headerLine != null && headerLine.isBlank()) {
                headerLine = reader.readLine();
            }
            if (headerLine == null) {
                return rooms;
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
                    rooms.add(toRoom(col, f));
                } catch (RuntimeException ex) {
                    throw new DataLoadException(
                            "Malformed room row at line " + lineNo + " in " + file
                                    + ": " + ex.getMessage(), ex);
                }
            }
        } catch (IOException e) {
            throw new DataLoadException("Failed to read rooms from " + file, e);
        }
        return rooms;
    }

    private Room toRoom(Map<String, Integer> col, List<String> f) {
        return Room.builder(required(get(col, f, "id"), "id"))
                .building(get(col, f, "building"))
                .floor((int) parseDouble(get(col, f, "floor"), 0))
                .capacity((int) parseDouble(get(col, f, "capacity"), 1))
                .genderPolicy(Gender.from(get(col, f, "genderPolicy")))
                .pricePerNight(parseDouble(get(col, f, "pricePerNight"), 0))
                .accessible(parseBool(get(col, f, "accessible")))
                .roomType(get(col, f, "roomType"))
                .amenities(CsvUtil.splitMulti(get(col, f, "amenities")))
                .build();
    }
}
