package com.bits.festival.accommodation.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A single bed within a {@link Room}. The Hungarian algorithm solves a one-to-one
 * assignment, so a room of capacity <em>k</em> is expanded into <em>k</em> {@code RoomSlot}s
 * before the cost matrix is built. The cost of placing a participant in any slot of a room
 * is identical — slots simply model the room's capacity.
 */
public final class RoomSlot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Room room;
    private final int index; // 0-based bed index within the room

    public RoomSlot(Room room, int index) {
        this.room = Objects.requireNonNull(room, "room");
        this.index = index;
    }

    public Room room() {
        return room;
    }

    public int index() {
        return index;
    }

    /** Stable identifier such as {@code "H1-204#0"}. */
    public String slotId() {
        return room.id() + "#" + index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomSlot)) return false;
        RoomSlot that = (RoomSlot) o;
        return index == that.index && room.equals(that.room);
    }

    @Override
    public int hashCode() {
        return Objects.hash(room, index);
    }

    @Override
    public String toString() {
        return slotId();
    }
}
