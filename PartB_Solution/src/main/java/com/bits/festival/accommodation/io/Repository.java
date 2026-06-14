package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;

import java.util.List;

/**
 * Generic Data Access Object (DAO). A {@code Repository<T>} loads a list of domain objects of
 * type {@code T} from some backing store (CSV file, JSON file, …) without exposing how. This
 * keeps the allocator decoupled from file formats — see {@link RepositoryFactory}, which picks
 * the concrete implementation by file extension.
 *
 * @param <T> the domain type produced (e.g. {@code Participant}, {@code Room})
 */
public interface Repository<T> {

    /**
     * Load every record from the backing store.
     *
     * @throws DataLoadException if the source is missing, malformed, or unreadable
     */
    List<T> loadAll() throws DataLoadException;
}
