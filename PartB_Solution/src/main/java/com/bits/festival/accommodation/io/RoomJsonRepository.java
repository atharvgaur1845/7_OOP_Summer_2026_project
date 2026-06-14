package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Room;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads {@link Room}s from a JSON array (the Django REST shape). Mirrors
 * {@link ParticipantJsonRepository}: Gson → {@link Dto} → immutable {@link Room}.
 */
public final class RoomJsonRepository implements Repository<Room> {

    private final Path file;
    private final Gson gson = new Gson();

    public RoomJsonRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Room> loadAll() throws DataLoadException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Dto[] dtos = gson.fromJson(reader, Dto[].class);
            if (dtos == null) {
                return new ArrayList<>();
            }
            List<Room> out = new ArrayList<>(dtos.length);
            for (Dto d : dtos) {
                out.add(d.toDomain());
            }
            return out;
        } catch (JsonSyntaxException e) {
            throw new DataLoadException("Malformed room JSON in " + file, e);
        } catch (IOException e) {
            throw new DataLoadException("Failed to read rooms from " + file, e);
        }
    }

    /** Gson data-transfer object mirroring the JSON schema. */
    static final class Dto {
        String id;
        String building;
        Integer floor;
        Integer capacity;
        String genderPolicy;
        Double pricePerNight;
        Boolean accessible;
        String roomType;
        List<String> amenities;

        Room toDomain() {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("room missing id");
            }
            return Room.builder(id)
                    .building(building)
                    .floor(floor == null ? 0 : floor)
                    .capacity(capacity == null ? 1 : capacity)
                    .genderPolicy(Gender.from(genderPolicy))
                    .pricePerNight(pricePerNight == null ? 0 : pricePerNight)
                    .accessible(Boolean.TRUE.equals(accessible))
                    .roomType(roomType)
                    .amenities(amenities)
                    .build();
        }
    }
}
