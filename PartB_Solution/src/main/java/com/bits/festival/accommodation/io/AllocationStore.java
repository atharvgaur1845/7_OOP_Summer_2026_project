package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.AllocationResult;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persists an {@link AllocationResult} via Java object serialization so a run can be saved and
 * restored later — the "offline cache / resume" capability. The whole object graph
 * ({@code AllocationResult} → {@code Allocation} → {@code Participant}/{@code Room} → …) is
 * {@link java.io.Serializable}, so a single {@code writeObject} round-trips the entire result.
 */
public final class AllocationStore {

    /** Save a result to a binary {@code .ser} file. */
    public void save(AllocationResult result, Path file) throws DataLoadException {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(file)))) {
                out.writeObject(result);
            }
        } catch (IOException e) {
            throw new DataLoadException("Failed to save allocation snapshot to " + file, e);
        }
    }

    /** Load a previously-saved result. */
    public AllocationResult load(Path file) throws DataLoadException {
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(file)))) {
            return (AllocationResult) in.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new DataLoadException("Snapshot is not a valid AllocationResult: " + file, e);
        } catch (IOException e) {
            throw new DataLoadException("Failed to load allocation snapshot from " + file, e);
        }
    }
}
