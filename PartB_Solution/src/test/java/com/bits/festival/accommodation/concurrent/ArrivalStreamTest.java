package com.bits.festival.accommodation.concurrent;

import com.bits.festival.accommodation.model.Participant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the producer–consumer arrival stream delivers every item and then terminates. */
class ArrivalStreamTest {

    @Test
    void producerDeliversAllArrivalsThenConsumerStops() throws Exception {
        ArrivalStream stream = new ArrivalStream();
        List<Participant> arrivals = List.of(
                Participant.builder("A1").build(),
                Participant.builder("A2").build(),
                Participant.builder("A3").build());

        CopyOnWriteArrayList<String> received = new CopyOnWriteArrayList<>();
        CountDownLatch done = new CountDownLatch(1);

        Thread consumer = new Thread(() -> {
            stream.consume(p -> received.add(p.id()));
            done.countDown(); // only reached after the poison pill
        });
        consumer.start();

        stream.startProducer(arrivals, 0);

        assertTrue(done.await(5, TimeUnit.SECONDS), "consumer should terminate after poison pill");
        assertEquals(3, received.size());
        assertTrue(received.containsAll(List.of("A1", "A2", "A3")));
    }

    @Test
    void manuallyOfferedArrivalsAreConsumed() throws Exception {
        ArrivalStream stream = new ArrivalStream();
        CountDownLatch got = new CountDownLatch(1);
        Thread consumer = new Thread(() -> stream.consume(p -> got.countDown()));
        consumer.setDaemon(true);
        consumer.start();

        stream.offer(Participant.builder("X").build());
        assertTrue(got.await(5, TimeUnit.SECONDS));
        consumer.interrupt();
    }
}
