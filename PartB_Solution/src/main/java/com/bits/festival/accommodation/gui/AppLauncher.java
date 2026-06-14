package com.bits.festival.accommodation.gui;

import javax.swing.*;
import java.awt.GridLayout;

/**
 * GUI entry point. Opens a small role chooser; both roles share one {@link AllocationModel}
 * instance, so an allocation the admin runs is instantly visible in the participant view
 * (Observer pattern across windows). This satisfies the brief's requirement for distinct
 * interfaces per role (warden/admin vs. participant).
 *
 * <pre>
 *   java -cp out:lib/gson.jar com.bits.festival.accommodation.gui.AppLauncher
 * </pre>
 */
public final class AppLauncher {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppLauncher::showChooser);
    }

    private static void showChooser() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // fall back to default look and feel
        }

        // One shared model → both windows observe the same allocation state.
        final AllocationModel model = new AllocationModel();

        JFrame chooser = new JFrame("Festival Accommodation — Choose Role");
        chooser.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        chooser.setLayout(new GridLayout(0, 1, 8, 8));

        JLabel title = new JLabel("Open a view:", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        chooser.add(title);

        JButton admin = new JButton("Admin / Warden Dashboard");
        admin.addActionListener(e -> new AdminDashboard(model).setVisible(true));
        chooser.add(admin);

        JButton participant = new JButton("Participant View (My Room)");
        participant.addActionListener(e -> new ParticipantView(model).setVisible(true));
        chooser.add(participant);

        chooser.setSize(320, 160);
        chooser.setLocationRelativeTo(null);
        chooser.setVisible(true);
    }

    private AppLauncher() {
    }
}
