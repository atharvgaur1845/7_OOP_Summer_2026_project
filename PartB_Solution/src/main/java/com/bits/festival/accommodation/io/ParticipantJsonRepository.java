package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Preference;
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
 * Loads {@link Participant}s from a JSON array — the shape the Django REST API would emit. Each
 * element maps to a {@link Dto} via Gson; the DTO is then translated into the immutable domain
 * object. This keeps Gson's reflective, mutable-field requirements off the domain model.
 */
public final class ParticipantJsonRepository implements Repository<Participant> {

    private final Path file;
    private final Gson gson = new Gson();

    public ParticipantJsonRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Participant> loadAll() throws DataLoadException {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Dto[] dtos = gson.fromJson(reader, Dto[].class);
            if (dtos == null) {
                return new ArrayList<>();
            }
            List<Participant> out = new ArrayList<>(dtos.length);
            for (Dto d : dtos) {
                out.add(d.toDomain());
            }
            return out;
        } catch (JsonSyntaxException e) {
            throw new DataLoadException("Malformed participant JSON in " + file, e);
        } catch (IOException e) {
            throw new DataLoadException("Failed to read participants from " + file, e);
        }
    }

    /** Gson data-transfer object mirroring the JSON schema. Fields are intentionally mutable. */
    static final class Dto {
        String id;
        String name;
        String gender;
        String homeCity;
        Double budgetPerNight;
        Integer arrivalDay;
        Integer nights;
        Boolean needsAccessibleRoom;
        String category;
        String preferredBuilding;
        String preferredRoomType;
        List<String> preferredRoommates;

        Participant toDomain() {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("participant missing id");
            }
            Preference pref = new Preference.Builder()
                    .building(preferredBuilding)
                    .roomType(preferredRoomType)
                    .roommates(preferredRoommates)
                    .build();
            return Participant.builder(id)
                    .name(name)
                    .gender(Gender.from(gender))
                    .homeCity(homeCity)
                    .budgetPerNight(budgetPerNight == null ? Double.MAX_VALUE : budgetPerNight)
                    .arrivalDay(arrivalDay == null ? 0 : arrivalDay)
                    .nights(nights == null ? 1 : nights)
                    .needsAccessibleRoom(Boolean.TRUE.equals(needsAccessibleRoom))
                    .category(Participant.Category.from(category))
                    .preference(pref)
                    .build();
        }
    }
}
