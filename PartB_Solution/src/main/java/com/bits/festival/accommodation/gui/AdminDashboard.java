package com.bits.festival.accommodation.gui;

import com.bits.festival.accommodation.exception.AccommodationException;
import com.bits.festival.accommodation.concurrent.ArrivalStream;
import com.bits.festival.accommodation.io.AllocationStore;
import com.bits.festival.accommodation.io.AllocationWriter;
import com.bits.festival.accommodation.io.RepositoryFactory;
import com.bits.festival.accommodation.model.Allocation;
import com.bits.festival.accommodation.model.AllocationResult;
import com.bits.festival.accommodation.model.Gender;
import com.bits.festival.accommodation.model.Participant;
import com.bits.festival.accommodation.model.Preference;
import com.bits.festival.accommodation.model.Room;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Admin / Warden view — the operational dashboard. Loads participant &amp; room files, runs the
 * Hungarian allocation on a background {@link SwingWorker} (so the UI never freezes), and shows
 * the resulting assignments, waitlist and success metrics in live {@link JTable}s. It also
 * drives the simulated real-time arrival stream and exports results / snapshots.
 *
 * <p>Registers as an {@link AllocationModelListener} (Observer) so every model change repaints
 * the tables automatically.
 */
public final class AdminDashboard extends JFrame implements AllocationModelListener {

    private final AllocationModel model;
    private final AllocationsTableModel allocationsTable = new AllocationsTableModel();
    private final WaitlistTableModel waitlistTable = new WaitlistTableModel();
    private final JLabel metricsLabel = new JLabel("No allocation yet. Load data and click Run.");
    private final JLabel statusLabel = new JLabel("Ready.");

    private final JButton runButton = new JButton("Run Allocation");
    private final JButton arrivalsButton = new JButton("Simulate Arrivals");
    private final JButton exportButton = new JButton("Export…");
    private final JButton snapshotButton = new JButton("Save Snapshot…");

    public AdminDashboard(AllocationModel model) {
        super("Festival Accommodation — Admin / Warden Dashboard");
        this.model = model;
        model.addListener(this);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        refreshButtons();
    }

    private JToolBar buildToolbar() {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);

        JButton loadSample = new JButton("Load Sample");
        loadSample.addActionListener(e -> loadSample());

        JButton loadFiles = new JButton("Load Files…");
        loadFiles.addActionListener(e -> loadFiles());

        runButton.addActionListener(e -> runAllocation());
        arrivalsButton.addActionListener(e -> simulateArrivals());
        exportButton.addActionListener(e -> exportResults());
        snapshotButton.addActionListener(e -> saveSnapshot());

        bar.add(loadSample);
        bar.add(loadFiles);
        bar.addSeparator();
        bar.add(runButton);
        bar.add(arrivalsButton);
        bar.addSeparator();
        bar.add(exportButton);
        bar.add(snapshotButton);
        return bar;
    }

    private JComponent buildCenter() {
        JTable left = new JTable(allocationsTable);
        left.setAutoCreateRowSorter(true);
        JTable right = new JTable(waitlistTable);
        right.setAutoCreateRowSorter(true);

        JScrollPane leftScroll = new JScrollPane(left);
        leftScroll.setBorder(BorderFactory.createTitledBorder("Assignments"));
        JScrollPane rightScroll = new JScrollPane(right);
        rightScroll.setBorder(BorderFactory.createTitledBorder("Waitlist (priority order)"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightScroll);
        split.setResizeWeight(0.65);
        return split;
    }

    private JComponent buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        metricsLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 0, 8));
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusRow.add(statusLabel);
        panel.add(metricsLabel, BorderLayout.NORTH);
        panel.add(statusRow, BorderLayout.SOUTH);
        return panel;
    }

    // ---- Actions ----

    private void loadSample() {
        try {
            List<Participant> ps = RepositoryFactory.participants(Path.of("data/participants.csv")).loadAll();
            List<Room> rs = RepositoryFactory.rooms(Path.of("data/rooms.csv")).loadAll();
            model.setData(ps, rs);
            status("Loaded sample: " + ps.size() + " participants, " + rs.size() + " rooms.");
        } catch (AccommodationException ex) {
            error("Could not load sample data (run from the PartB_Solution directory).", ex);
        }
    }

    private void loadFiles() {
        JFileChooser chooser = new JFileChooser(new File("data"));
        chooser.setDialogTitle("Select participants file (.csv or .json)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path participantsFile = chooser.getSelectedFile().toPath();

        chooser.setDialogTitle("Select rooms file (.csv or .json)");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path roomsFile = chooser.getSelectedFile().toPath();

        try {
            List<Participant> ps = RepositoryFactory.participants(participantsFile).loadAll();
            List<Room> rs = RepositoryFactory.rooms(roomsFile).loadAll();
            model.setData(ps, rs);
            status("Loaded " + ps.size() + " participants, " + rs.size() + " rooms.");
        } catch (AccommodationException ex) {
            error("Failed to load files.", ex);
        }
    }

    private void runAllocation() {
        if (!model.hasData()) {
            error("Load participants and rooms first.", null);
            return;
        }
        setBusy(true, "Running Hungarian allocation…");
        // SwingWorker keeps the (potentially heavy) solve off the Event Dispatch Thread.
        new SwingWorker<AllocationResult, Void>() {
            @Override
            protected AllocationResult doInBackground() throws Exception {
                return model.runAllocation();
            }

            @Override
            protected void done() {
                try {
                    get();
                    status("Allocation complete.");
                } catch (Exception ex) {
                    error("Allocation failed.", ex.getCause() != null ? ex.getCause() : ex);
                } finally {
                    setBusy(false, null);
                }
            }
        }.execute();
    }

    private void simulateArrivals() {
        if (!model.hasData()) {
            error("Load data and run an allocation first.", null);
            return;
        }
        setBusy(true, "Streaming late arrivals…");
        ArrivalStream stream = new ArrivalStream();
        stream.startProducer(syntheticArrivals(), 700);

        // Consumer runs on its own thread; each arrival re-runs allocation and the model
        // notifies the UI on the EDT. This is the Producer–Consumer pattern end to end.
        Thread consumer = new Thread(() -> {
            stream.consume(p -> {
                try {
                    model.onArrival(p);
                    status("Seated/queued late arrival " + p.id() + " (" + p.name() + ").");
                } catch (AccommodationException ex) {
                    error("Failed to process arrival " + p.id(), ex);
                }
            });
            SwingUtilities.invokeLater(() -> setBusy(false, "Arrival stream finished."));
        }, "arrival-consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    private void exportResults() {
        AllocationResult result = model.result();
        if (result == null) {
            error("Nothing to export — run an allocation first.", null);
            return;
        }
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setDialogTitle("Choose output directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path dir = chooser.getSelectedFile().toPath();
        try {
            new AllocationWriter().write(result,
                    dir.resolve("allocations.csv"), dir.resolve("waitlist.csv"));
            status("Exported allocations.csv and waitlist.csv to " + dir);
        } catch (AccommodationException ex) {
            error("Export failed.", ex);
        }
    }

    private void saveSnapshot() {
        AllocationResult result = model.result();
        if (result == null) {
            error("Nothing to save — run an allocation first.", null);
            return;
        }
        JFileChooser chooser = new JFileChooser(new File("."));
        chooser.setSelectedFile(new File("allocation.ser"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            new AllocationStore().save(result, chooser.getSelectedFile().toPath());
            status("Snapshot saved (serialized) to " + chooser.getSelectedFile());
        } catch (AccommodationException ex) {
            error("Snapshot failed.", ex);
        }
    }

    /** A few synthetic late registrations to demonstrate the live stream. */
    private List<Participant> syntheticArrivals() {
        return Arrays.asList(
                Participant.builder("P901").name("Late Arrival – Neha").gender(Gender.FEMALE)
                        .budgetPerNight(700).arrivalDay(4).nights(1)
                        .category(Participant.Category.PERFORMER)
                        .preference(new Preference.Builder().roomType("NON_AC").build()).build(),
                Participant.builder("P902").name("Late Arrival – Rohan").gender(Gender.MALE)
                        .budgetPerNight(700).arrivalDay(4).nights(1)
                        .category(Participant.Category.DELEGATE).build(),
                Participant.builder("P903").name("Late Arrival – Aman").gender(Gender.MALE)
                        .budgetPerNight(700).arrivalDay(5).nights(1)
                        .category(Participant.Category.ATTENDEE).build());
    }

    // ---- Observer callback ----

    @Override
    public void onModelChanged(AllocationModel m) {
        AllocationResult result = m.result();
        allocationsTable.setData(result == null ? List.of() : result.allocations());
        waitlistTable.setData(result == null ? List.of() : result.waitlist());
        metricsLabel.setText(result == null
                ? "Data loaded: " + m.participants().size() + " participants, "
                        + m.rooms().size() + " rooms. Click Run Allocation."
                : "<html><b>Metrics:</b> " + result.metrics() + "</html>");
        refreshButtons();
    }

    private void refreshButtons() {
        boolean hasData = model.hasData();
        boolean hasResult = model.result() != null;
        runButton.setEnabled(hasData);
        arrivalsButton.setEnabled(hasResult);
        exportButton.setEnabled(hasResult);
        snapshotButton.setEnabled(hasResult);
    }

    private void setBusy(boolean busy, String msg) {
        setCursor(busy ? java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR)
                : java.awt.Cursor.getDefaultCursor());
        runButton.setEnabled(!busy && model.hasData());
        if (msg != null) {
            status(msg);
        }
    }

    private void status(String msg) {
        statusLabel.setText(msg);
    }

    private void error(String msg, Throwable ex) {
        String detail = ex == null ? msg : msg + "\n\n" + ex.getMessage();
        JOptionPane.showMessageDialog(this, detail, "Accommodation", JOptionPane.ERROR_MESSAGE);
        status(msg);
    }

    // ---- Table models (inner classes) ----

    /** Backs the assignments {@link JTable}. */
    private static final class AllocationsTableModel extends AbstractTableModel {
        private final String[] cols = {"Participant", "Name", "Gender", "Category",
                "Room", "Building", "Type", "₹/night", "Nights", "Charge (₹)", "Cost"};
        private List<Allocation> rows = new ArrayList<>();

        void setData(List<Allocation> data) {
            this.rows = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Allocation a = rows.get(r);
            Participant p = a.participant();
            switch (c) {
                case 0: return p.id();
                case 1: return p.name();
                case 2: return p.gender();
                case 3: return p.category();
                case 4: return a.room().id();
                case 5: return a.room().building();
                case 6: return a.room().roomType();
                case 7: return a.room().pricePerNight();
                case 8: return p.nights();
                case 9: return a.charge();
                case 10: return a.cost();
                default: return "";
            }
        }
    }

    /** Backs the waitlist {@link JTable}. */
    private static final class WaitlistTableModel extends AbstractTableModel {
        private final String[] cols = {"Rank", "Participant", "Name", "Category", "Arrival Day"};
        private List<Participant> rows = new ArrayList<>();

        void setData(List<Participant> data) {
            this.rows = new ArrayList<>(data);
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Participant p = rows.get(r);
            switch (c) {
                case 0: return r + 1;
                case 1: return p.id();
                case 2: return p.name();
                case 3: return p.category();
                case 4: return p.arrivalDay();
                default: return "";
            }
        }
    }
}
