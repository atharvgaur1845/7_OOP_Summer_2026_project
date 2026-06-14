package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Factory (GoF) that selects the concrete {@link Repository} implementation by file extension:
 * {@code .csv} → CSV repository, {@code .json} → JSON repository. Callers get a
 * format-agnostic {@code Repository<T>} and never name a concrete class.
 */
public final class RepositoryFactory {

    private RepositoryFactory() {
    }

    public static Repository<Participant> participants(Path file) throws DataLoadException {
        switch (extension(file)) {
            case "csv":
                return new ParticipantCsvRepository(file);
            case "json":
                return new ParticipantJsonRepository(file);
            default:
                throw new DataLoadException("Unsupported participant file type: " + file);
        }
    }

    public static Repository<Room> rooms(Path file) throws DataLoadException {
        switch (extension(file)) {
            case "csv":
                return new RoomCsvRepository(file);
            case "json":
                return new RoomJsonRepository(file);
            default:
                throw new DataLoadException("Unsupported room file type: " + file);
        }
    }

    private static String extension(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }
}
