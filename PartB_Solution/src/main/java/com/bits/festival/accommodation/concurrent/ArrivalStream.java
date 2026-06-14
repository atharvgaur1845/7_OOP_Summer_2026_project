package com.bits.festival.accommodation.concurrent;

import com.bits.festival.accommodation.model.Participant;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Simulates the festival's real-time arrival feed (a WebSocket / Firebase stream of late
 * registrations) using a {@link BlockingQueue} — the integration option the brief calls
 * "simulated real-time streams". This is a classic <strong>Producer–Consumer</strong>:
 *
 * <ul>
 *   <li>The <em>producer</em> ({@link #startProducer}) pushes late-arriving participants onto
 *       the queue on a background daemon thread, then a {@link #POISON} sentinel to signal end.</li>
 *   <li>The <em>consumer</em> ({@link #consume}) blocks on {@code take()} and dispatches each
 *       arrival to a handler (e.g. "seat this person in a free bed and refresh the dashboard")
 *       until it sees the poison pill.</li>
 * </ul>
 *
 * The queue itself provides the thread-safe hand-off, so no explicit locking is needed.
 */
public final class ArrivalStream {

    /** Poison pill that tells the consumer the producer is finished. */
    public static final Participant POISON = Participant.builder("__POISON__").build();

    private final BlockingQueue<Participant> queue = new LinkedBlockingQueue<>();

    /**
     * Launch a daemon thread that emits each arrival, spaced {@code delayMillis} apart, then a
     * poison pill.
     *
     * @return the started producer thread (callers may {@link Thread#interrupt()} to stop early)
     */
    public Thread startProducer(List<Participant> arrivals, long delayMillis) {
        Thread producer = new Thread(() -> {
            try {
                for (Participant p : arrivals) {
                    Thread.sleep(Math.max(0, delayMillis));
                    queue.put(p);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Always signal completion so the consumer can exit cleanly.
                try {
                    queue.put(POISON);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "arrival-producer");
        producer.setDaemon(true);
        producer.start();
        return producer;
    }

    /**
     * Block-consume arrivals, passing each to {@code handler}, until the poison pill arrives or
     * the thread is interrupted. Intended to run on its own (e.g. {@code SwingWorker}) thread.
     */
    public void consume(Consumer<Participant> handler) {
        try {
            while (true) {
                Participant p = queue.take();
                if (p == POISON) {
                    return;
                }
                handler.accept(p);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Manually offer an arrival (used by tests and the GUI's "add arrival" button). */
    public void offer(Participant participant) {
        queue.add(participant);
    }

    public int pending() {
        return queue.size();
    }
}
