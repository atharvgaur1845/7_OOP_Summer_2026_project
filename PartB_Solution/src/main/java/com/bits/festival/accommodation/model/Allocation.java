package com.bits.festival.accommodation.model;

import java.io.Serializable;

/**
 * The result of seating one {@link Participant} into one {@link Room}: an immutable
 * (participant, room, cost) triple. {@link #cost} is the dissatisfaction score the cost
 * strategy assigned to this pairing (lower is better). {@link #charge()} derives the wallet
 * billing amount the festival's Wallet module should debit.
 */
public final class Allocation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Participant participant;
    private final Room room;
    private final double cost;

    public Allocation(Participant participant, Room room, double cost) {
        this.participant = participant;
        this.room = room;
        this.cost = cost;
    }

    public Participant participant() {
        return participant;
    }

    public Room room() {
        return room;
    }

    public double cost() {
        return cost;
    }

    /** Total amount to bill = price/night × nights stayed. */
    public double charge() {
        return room.pricePerNight() * participant.nights();
    }

    @Override
    public String toString() {
        return participant.id() + " -> " + room.id() + " (cost=" + cost + ")";
    }
}
