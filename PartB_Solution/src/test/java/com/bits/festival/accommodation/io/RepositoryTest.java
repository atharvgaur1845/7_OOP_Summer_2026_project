package com.bits.festival.accommodation.io;

import com.bits.festival.accommodation.exception.DataLoadException;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests for the CSV / JSON DAO layer and the factory's extension routing. */
class RepositoryTest {

    @Test
    void loadsParticipantsFromCsv(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("participants.csv");
        Files.writeString(file,
                "id,name,gender,budgetPerNight,nights,needsAccessible,category,prefRoommates\n"
                        + "P1,Asha,FEMALE,800,2,false,PERFORMER,P2;P3\n"
                        + "P2,Ben,MALE,,1,yes,ATTENDEE,\n");

        List<Participant> ps = RepositoryFactory.participants(file).loadAll();
        assertEquals(2, ps.size());

        Participant p1 = ps.get(0);
        assertEquals("P1", p1.id());
        assertEquals(Gender.FEMALE, p1.gender());
        assertEquals(Participant.Category.PERFORMER, p1.category());
        assertEquals(2, p1.preference().preferredRoommates().size());

        Participant p2 = ps.get(1);
        assertTrue(p2.needsAccessibleRoom());
        assertEquals(Double.MAX_VALUE, p2.budgetPerNight(), 0.0); // blank budget -> no cap
    }

    @Test
    void loadsRoomsFromCsvWithQuotedAndMultiValueFields(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("rooms.csv");
        Files.writeString(file,
                "id,building,capacity,genderPolicy,pricePerNight,accessible,roomType,amenities\n"
                        + "R1,\"Hostel, A\",3,MALE,500,true,NON_AC,wifi;fan\n");

        List<Room> rooms = RepositoryFactory.rooms(file).loadAll();
        assertEquals(1, rooms.size());
        Room r = rooms.get(0);
        assertEquals("Hostel, A", r.building()); // embedded comma preserved by quoting
        assertEquals(3, r.capacity());
        assertEquals(2, r.amenities().size());
        assertTrue(r.accessible());
    }

    @Test
    void loadsFromJson(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("participants.json");
        Files.writeString(file,
                "[{\"id\":\"P1\",\"name\":\"Asha\",\"gender\":\"FEMALE\",\"nights\":2,"
                        + "\"category\":\"VIP\",\"preferredRoommates\":[\"P2\"]}]");
        List<Participant> ps = RepositoryFactory.participants(file).loadAll();
        assertEquals(1, ps.size());
        assertEquals(Participant.Category.VIP, ps.get(0).category());
        assertEquals(2, ps.get(0).nights());
    }

    @Test
    void malformedJsonRaisesDataLoadException(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("rooms.json");
        Files.writeString(file, "{ this is : not valid json ]");
        assertThrows(DataLoadException.class,
                () -> RepositoryFactory.rooms(file).loadAll());
    }

    @Test
    void missingIdColumnRaisesDataLoadException(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("participants.csv");
        Files.writeString(file, "name,gender\nAsha,FEMALE\n");
        assertThrows(DataLoadException.class,
                () -> RepositoryFactory.participants(file).loadAll());
    }

    @Test
    void unsupportedExtensionRejected(@TempDir Path dir) {
        Path file = dir.resolve("rooms.xml");
        assertThrows(DataLoadException.class, () -> RepositoryFactory.rooms(file));
    }
}
