package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.Allocation;
import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Participant;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes an {@link AllocationResult} back out as the festival's exchange files:
 * {@code allocations.{csv,json}} (consumed by the Mobile module to notify participants and by
 * the Wallet module to bill room charges) and {@code waitlist.{csv,json}}. Output format is
 * chosen by the target file's extension, mirroring {@link RepositoryFactory}.
 */
public final class AllocationWriter {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Write allocations to {@code allocationsFile} and the waitlist to {@code waitlistFile}. */
    public void write(AllocationResult result, Path allocationsFile, Path waitlistFile)
            throws DataLoadException {
        writeAllocations(result, allocationsFile);
        writeWaitlist(result, waitlistFile);
    }

    public void writeAllocations(AllocationResult result, Path file) throws DataLoadException {
        if (isJson(file)) {
            List<AllocationDto> dtos = new ArrayList<>();
            for (Allocation a : result.allocations()) {
                dtos.add(AllocationDto.of(a));
            }
            writeString(file, gson.toJson(dtos));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("participantId,participantName,roomId,building,roomType,nights,"
                    + "pricePerNight,charge,dissatisfaction\n");
            for (Allocation a : result.allocations()) {
                Participant p = a.participant();
                sb.append(CsvUtil.escape(p.id())).append(',')
                        .append(CsvUtil.escape(p.name())).append(',')
                        .append(CsvUtil.escape(a.room().id())).append(',')
                        .append(CsvUtil.escape(a.room().building())).append(',')
                        .append(CsvUtil.escape(a.room().roomType())).append(',')
                        .append(p.nights()).append(',')
                        .append(fmt(a.room().pricePerNight())).append(',')
                        .append(fmt(a.charge())).append(',')
                        .append(fmt(a.cost())).append('\n');
            }
            writeString(file, sb.toString());
        }
    }

    public void writeWaitlist(AllocationResult result, Path file) throws DataLoadException {
        if (isJson(file)) {
            List<WaitlistDto> dtos = new ArrayList<>();
            int rank = 1;
            for (Participant p : result.waitlist()) {
                dtos.add(WaitlistDto.of(p, rank++));
            }
            writeString(file, gson.toJson(dtos));
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("waitlistRank,participantId,participantName,category,arrivalDay\n");
            int rank = 1;
            for (Participant p : result.waitlist()) {
                sb.append(rank++).append(',')
                        .append(CsvUtil.escape(p.id())).append(',')
                        .append(CsvUtil.escape(p.name())).append(',')
                        .append(p.category()).append(',')
                        .append(p.arrivalDay()).append('\n');
            }
            writeString(file, sb.toString());
        }
    }

    private void writeString(Path file, String content) throws DataLoadException {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                w.write(content);
            }
        } catch (IOException e) {
            throw new DataLoadException("Failed to write " + file, e);
        }
    }

    private static boolean isJson(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    private static String fmt(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    /** JSON output schema for an allocation (what the Wallet/Mobile modules re-import). */
    static final class AllocationDto {
        String participantId;
        String participantName;
        String roomId;
        String building;
        String roomType;
        int nights;
        double pricePerNight;
        double charge;
        double dissatisfaction;

        static AllocationDto of(Allocation a) {
            AllocationDto d = new AllocationDto();
            d.participantId = a.participant().id();
            d.participantName = a.participant().name();
            d.roomId = a.room().id();
            d.building = a.room().building();
            d.roomType = a.room().roomType();
            d.nights = a.participant().nights();
            d.pricePerNight = a.room().pricePerNight();
            d.charge = a.charge();
            d.dissatisfaction = a.cost();
            return d;
        }
    }

    /** JSON output schema for a waitlisted participant. */
    static final class WaitlistDto {
        int waitlistRank;
        String participantId;
        String participantName;
        String category;
        int arrivalDay;

        static WaitlistDto of(Participant p, int rank) {
            WaitlistDto d = new WaitlistDto();
            d.waitlistRank = rank;
            d.participantId = p.id();
            d.participantName = p.name();
            d.category = p.category().name();
            d.arrivalDay = p.arrivalDay();
            return d;
        }
    }
}
