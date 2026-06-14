package com.bits.festival.accommodation.gui;

import com.bits.festival.accommodation.exception.AccommodationException;
import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;
import com.bits.festival.accommodation.service.AccommodationAllocator;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The <strong>Model</strong> of the GUI's MVC architecture and the <strong>Subject</strong> of
 * the Observer pattern. It owns the loaded participants/rooms and the most recent
 * {@link AllocationResult}, runs the allocation through {@link AccommodationAllocator}, and
 * notifies registered {@link AllocationModelListener}s whenever its state changes.
 *
 * <p>Listener notifications are always marshalled onto the Swing Event Dispatch Thread, so the
 * model can safely be driven from background threads (the {@code SwingWorker} that runs the
 * allocation, and the arrival-stream consumer thread).
 */
public final class AllocationModel {

    private final List<AllocationModelListener> listeners = new CopyOnWriteArrayList<>();
    private final AccommodationAllocator allocator = new AccommodationAllocator();

    private final List<Participant> participants = new ArrayList<>();
    private final List<Room> rooms = new ArrayList<>();
    private volatile AllocationResult result;

    public void addListener(AllocationModelListener listener) {
        listeners.add(listener);
    }

    public synchronized void setData(List<Participant> participants, List<Room> rooms) {
        this.participants.clear();
        this.participants.addAll(participants);
        this.rooms.clear();
        this.rooms.addAll(rooms);
        this.result = null;
        notifyChanged();
    }

    /**
     * Run the full allocation over the current data. Safe to call from a background thread.
     *
     * @return the fresh result (also stored and broadcast to listeners)
     */
    public AllocationResult runAllocation() throws AccommodationException {
        final List<Participant> ps;
        final List<Room> rs;
        synchronized (this) {
            ps = new ArrayList<>(participants);
            rs = new ArrayList<>(rooms);
        }
        AllocationResult r = allocator.allocate(ps, rs);
        synchronized (this) {
            this.result = r;
        }
        notifyChanged();
        return r;
    }

    /**
     * Register a late arrival and re-run the allocation so the dashboard reflects the new
     * participant in real time. Used by the {@code ArrivalStream} consumer.
     */
    public void onArrival(Participant arrival) throws AccommodationException {
        synchronized (this) {
            // Ignore duplicates (idempotent against repeated stream events).
            boolean exists = participants.stream().anyMatch(p -> p.id().equals(arrival.id()));
            if (!exists) {
                participants.add(arrival);
            }
        }
        runAllocation();
    }

    public synchronized List<Participant> participants() {
        return new ArrayList<>(participants);
    }

    public synchronized List<Room> rooms() {
        return new ArrayList<>(rooms);
    }

    public AllocationResult result() {
        return result;
    }

    public synchronized boolean hasData() {
        return !participants.isEmpty() && !rooms.isEmpty();
    }

    private void notifyChanged() {
        Runnable fire = () -> {
            for (AllocationModelListener l : listeners) {
                l.onModelChanged(this);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            fire.run();
        } else {
            SwingUtilities.invokeLater(fire);
        }
    }
}
