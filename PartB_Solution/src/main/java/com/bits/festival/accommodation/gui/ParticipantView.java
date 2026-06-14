package com.bits.festival.accommodation.gui;

import com.bits.festival.accommodation.model.Allocation;
import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Room;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Participant view — the read-only, single-user perspective a festival-goer sees in the mobile
 * app. The participant picks their ID from a dropdown and sees their assigned room, roommates,
 * nightly price and total wallet charge — or, if unlucky, their position on the waitlist.
 *
 * <p>Deliberately distinct from the {@link AdminDashboard}: no tables, no controls, no
 * operational actions — just "where am I staying?". Also an Observer so it updates live when
 * the admin re-runs an allocation or a late arrival is processed.
 */
public final class ParticipantView extends JFrame implements AllocationModelListener {

    private final AllocationModel model;
    private final JComboBox<String> participantPicker = new JComboBox<>();
    private final JLabel headline = new JLabel("Select your participant ID above.");
    private final JTextArea details = new JTextArea(10, 40);

    public ParticipantView(AllocationModel model) {
        super("Festival Accommodation — My Room (Participant View)");
        this.model = model;
        model.addListener(this);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new GridLayout(2, 1, 4, 4));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        JPanel pickerRow = new JPanel(new BorderLayout(8, 0));
        pickerRow.add(new JLabel("Your participant ID:"), BorderLayout.WEST);
        pickerRow.add(participantPicker, BorderLayout.CENTER);
        top.add(pickerRow);
        headline.setFont(headline.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        top.add(headline);
        add(top, BorderLayout.NORTH);

        details.setEditable(false);
        details.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(details), BorderLayout.CENTER);

        participantPicker.addActionListener(e -> refreshSelection());

        setSize(560, 380);
        setLocationRelativeTo(null);
        reloadPicker();
        refreshSelection();
    }

    @Override
    public void onModelChanged(AllocationModel m) {
        reloadPicker();
        refreshSelection();
    }

    private void reloadPicker() {
        Object selected = participantPicker.getSelectedItem();
        participantPicker.removeAllItems();
        for (Participant p : model.participants()) {
            participantPicker.addItem(p.id() + " — " + p.name());
        }
        if (selected != null) {
            participantPicker.setSelectedItem(selected);
        }
    }

    private void refreshSelection() {
        String item = (String) participantPicker.getSelectedItem();
        AllocationResult result = model.result();
        if (item == null) {
            headline.setText("No participants loaded yet.");
            details.setText("");
            return;
        }
        String id = item.split(" — ")[0];

        if (result == null) {
            headline.setText("Allocation not run yet.");
            details.setText("Ask the accommodation desk to run the allocation, "
                    + "then your room will appear here.");
            return;
        }

        Optional<Allocation> mine = result.allocations().stream()
                .filter(a -> a.participant().id().equals(id))
                .findFirst();

        if (mine.isPresent()) {
            showAssigned(mine.get(), result);
        } else {
            showWaitlisted(id, result);
        }
    }

    private void showAssigned(Allocation a, AllocationResult result) {
        Room room = a.room();
        Participant p = a.participant();
        headline.setText("✓ You're booked in Room " + room.id() + " (" + room.building() + ")");

        List<Participant> roommates = result.roommatesOf(p.id());
        String roommateText = roommates.isEmpty()
                ? "(none — you have the room to yourself)"
                : roommates.stream().map(r -> r.id() + " " + r.name())
                        .collect(Collectors.joining(", "));

        details.setText(""
                + "Participant : " + p.id() + " — " + p.name() + "\n"
                + "Room        : " + room.id() + "  (Building " + room.building()
                        + ", Floor " + room.floor() + ")\n"
                + "Room type   : " + room.roomType()
                        + (room.accessible() ? "  [accessible]" : "") + "\n"
                + "Amenities   : " + String.join(", ", room.amenities()) + "\n"
                + "Roommates   : " + roommateText + "\n"
                + "\n"
                + "Nights      : " + p.nights() + "\n"
                + "Price/night : ₹" + (long) room.pricePerNight() + "\n"
                + "Wallet debit: ₹" + (long) a.charge() + "   (billed to your festival wallet)\n");
    }

    private void showWaitlisted(String id, AllocationResult result) {
        int rank = 1;
        for (Participant p : result.waitlist()) {
            if (p.id().equals(id)) {
                headline.setText("⏳ You're on the waitlist (position " + rank + ")");
                details.setText(""
                        + "Participant : " + p.id() + " — " + p.name() + "\n"
                        + "Category    : " + p.category() + "\n"
                        + "Waitlist #  : " + rank + " of " + result.waitlist().size() + "\n\n"
                        + "All rooms matching your requirements are currently full.\n"
                        + "You'll be assigned automatically as soon as a bed frees up\n"
                        + "(higher-priority categories are seated first).");
                return;
            }
            rank++;
        }
        headline.setText("No record found for " + id);
        details.setText("This participant was not part of the latest allocation run.");
    }
}
