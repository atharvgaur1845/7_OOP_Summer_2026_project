package com.bits.festival.accommodation.cli;

import com.bits.festival.accommodation.exception.AccommodationException;
import com.bits.festival.accommodation.io.AllocationStore;
import com.bits.festival.accommodation.io.AllocationWriter;
import com.bits.festival.accommodation.io.RepositoryFactory;
import com.bits.festival.accommodation.model.Allocation;
import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;
import com.bits.festival.accommodation.service.AccommodationAllocator;

import java.nio.file.Path;
import java.util.List;

/**
 * Headless command-line entry point — the integration-friendly runner used in scripts, the
 * demo video, and CI.
 *
 * <pre>
 *   java -cp out:lib/gson.jar com.bits.festival.accommodation.cli.Main \
 *        &lt;participants.(csv|json)&gt; &lt;rooms.(csv|json)&gt; [outputDir] [--format csv|json]
 * </pre>
 *
 * Reads the input files (format auto-detected by extension), runs the Hungarian allocation,
 * prints the success metrics, writes {@code allocations.*} + {@code waitlist.*} to the output
 * directory, and snapshots the result with serialization to {@code allocation.ser}.
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: Main <participants.(csv|json)> <rooms.(csv|json)> "
                    + "[outputDir] [--format csv|json]");
            System.exit(2);
            return;
        }

        Path participantsFile = Path.of(args[0]);
        Path roomsFile = Path.of(args[1]);
        Path outDir = Path.of(args.length >= 3 && !args[2].startsWith("--") ? args[2] : "out_data");
        String format = parseFormat(args);

        try {
            List<Participant> participants = RepositoryFactory.participants(participantsFile).loadAll();
            List<Room> rooms = RepositoryFactory.rooms(roomsFile).loadAll();

            System.out.printf("Loaded %d participants and %d rooms (%d beds).%n",
                    participants.size(), rooms.size(),
                    rooms.stream().mapToInt(Room::capacity).sum());

            AllocationResult result = new AccommodationAllocator().allocate(participants, rooms);

            printReport(result);

            AllocationWriter writer = new AllocationWriter();
            Path allocationsOut = outDir.resolve("allocations." + format);
            Path waitlistOut = outDir.resolve("waitlist." + format);
            writer.write(result, allocationsOut, waitlistOut);
            new AllocationStore().save(result, outDir.resolve("allocation.ser"));

            System.out.println();
            System.out.println("Wrote: " + allocationsOut);
            System.out.println("Wrote: " + waitlistOut);
            System.out.println("Snapshot: " + outDir.resolve("allocation.ser"));
        } catch (AccommodationException e) {
            System.err.println("Allocation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printReport(AllocationResult result) {
        System.out.println();
        System.out.println("==== Allocation metrics ====");
        System.out.println(result.metrics());
        System.out.println();
        System.out.println("==== Sample assignments (first 10) ====");
        result.allocations().stream().limit(10).forEach(Main::printAllocation);
        if (!result.waitlist().isEmpty()) {
            System.out.println();
            System.out.println("==== Waitlist (priority order, first 10) ====");
            result.waitlist().stream().limit(10).forEach(p ->
                    System.out.printf("  %-8s %-20s %-10s arrivalDay=%d%n",
                            p.id(), p.name(), p.category(), p.arrivalDay()));
        }
    }

    private static void printAllocation(Allocation a) {
        System.out.printf("  %-8s %-20s -> %-8s @ %-6s (%s, ₹%.0f/night, cost=%.1f)%n",
                a.participant().id(), a.participant().name(),
                a.room().id(), a.room().building(), a.room().roomType(),
                a.room().pricePerNight(), a.cost());
    }

    private static String parseFormat(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("--format")) {
                String f = args[i + 1].toLowerCase();
                if (f.equals("csv") || f.equals("json")) {
                    return f;
                }
            }
        }
        return "csv";
    }

    private Main() {
    }
}
