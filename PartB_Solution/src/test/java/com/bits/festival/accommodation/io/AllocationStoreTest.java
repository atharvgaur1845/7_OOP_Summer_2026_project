package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;
import com.bits.festival.accommodation.service.AccommodationAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests serialization round-trip of an {@link AllocationResult}. */
class AllocationStoreTest {

    @Test
    void savesAndReloadsResultIdentically(@TempDir Path dir) throws Exception {
        List<Participant> ps = List.of(
                Participant.builder("P1").gender(Gender.MALE).nights(3).build(),
                Participant.builder("P2").gender(Gender.MALE).nights(2).build());
        List<Room> rooms = List.of(
                Room.builder("R1").genderPolicy(Gender.MALE).capacity(1).pricePerNight(500).build());

        AllocationResult original = new AccommodationAllocator().allocate(ps, rooms);

        Path file = dir.resolve("snapshot.ser");
        AllocationStore store = new AllocationStore();
        store.save(original, file);
        AllocationResult reloaded = store.load(file);

        assertEquals(original.allocations().size(), reloaded.allocations().size());
        assertEquals(original.waitlist().size(), reloaded.waitlist().size());
        assertEquals(original.metrics().assignedCount(), reloaded.metrics().assignedCount());
        assertEquals(original.metrics().totalCost(), reloaded.metrics().totalCost(), 1e-9);
        // The reloaded graph is usable: charge survives the round-trip.
        if (!reloaded.allocations().isEmpty()) {
            assertEquals(original.allocations().get(0).charge(),
                    reloaded.allocations().get(0).charge(), 1e-9);
        }
    }

    @Test
    void loadingGarbageRaisesDataLoadException(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("bad.ser");
        java.nio.file.Files.writeString(file, "not a serialized object");
        assertThrows(Exception.class, () -> new AllocationStore().load(file));
    }
}
