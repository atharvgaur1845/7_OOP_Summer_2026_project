package com.bits.festival.accommodation.gui;

/**
 * Observer (GoF) callback for {@link AllocationModel} changes. Views (the admin dashboard and
 * the participant view) register themselves and refresh whenever the underlying data or the
 * computed allocation changes.
 */
@FunctionalInterface
public interface AllocationModelListener {

    /** Invoked on the Swing Event Dispatch Thread after the model changes. */
    void onModelChanged(AllocationModel model);
}
