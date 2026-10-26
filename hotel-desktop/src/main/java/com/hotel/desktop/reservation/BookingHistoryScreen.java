package com.hotel.desktop.reservation;

import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.BookingHistoryEntry;
import com.hotel.core.model.enums.BookingHistoryPeriod;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

public final class BookingHistoryScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter EVENT_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a");

    private final ReservationService service;
    private final Supplier<AuthenticatedUser> currentAdmin;
    private final JComboBox<BookingHistoryPeriod> period = new JComboBox<>(BookingHistoryPeriod.values());
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Event date", "Reference", "Guest", "Room", "Stay", "Status", "Event", "Notes"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final JButton recordDispute = StaffUi.secondaryButton("Record dispute");
    private List<BookingHistoryEntry> entries = List.of();

    public BookingHistoryScreen(ReservationService service, Supplier<AuthenticatedUser> currentAdmin) {
        this.service = java.util.Objects.requireNonNull(service);
        this.currentAdmin = java.util.Objects.requireNonNull(currentAdmin);
        JPanel body = StaffUi.createPage(this, "Booking history",
                "Archived checkout, cancellation, and dispute events");
        JPanel card = StaffUi.card();
        JPanel tools = new JPanel(new BorderLayout(10, 0));
        tools.setOpaque(false);
        JPanel periodChoice = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodChoice.setOpaque(false);
        periodChoice.add(StaffUi.fieldLabel("Show"));
        period.setFont(StaffUi.BODY_FONT);
        periodChoice.add(period);
        JButton refresh = StaffUi.secondaryButton("Refresh");
        refresh.addActionListener(event -> refreshHistory());
        tools.add(periodChoice, BorderLayout.WEST);
        tools.add(refresh, BorderLayout.EAST);
        card.add(tools, BorderLayout.NORTH);

        StaffUi.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(175);
        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(270);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        table.getColumnModel().getColumn(6).setPreferredWidth(130);
        table.getColumnModel().getColumn(7).setPreferredWidth(330);
        table.getSelectionModel().addListSelectionListener(event -> updateActions());
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(event)) {
                    showSelectedDetails();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(StaffUi.LINE));
        card.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        recordDispute.setEnabled(false);
        recordDispute.addActionListener(event -> recordDispute());
        actions.add(recordDispute);
        footer.add(feedback, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        body.add(card, BorderLayout.CENTER);
        period.addActionListener(event -> refreshHistory());
    }

    public void refreshHistory() {
        AuthenticatedUser admin = currentAdmin.get();
        BookingHistoryPeriod selectedPeriod = (BookingHistoryPeriod) period.getSelectedItem();
        if (admin == null || selectedPeriod == null) {
            return;
        }
        recordDispute.setEnabled(false);
        StaffUi.showFeedback(feedback, "Loading booking history…", false);
        new SwingWorker<List<BookingHistoryEntry>, Void>() {
            @Override
            protected List<BookingHistoryEntry> doInBackground() throws SQLException {
                return service.findBookingHistory(admin, selectedPeriod, LocalDate.now());
            }

            @Override
            protected void done() {
                try {
                    entries = get();
                    model.setRowCount(0);
                    for (BookingHistoryEntry entry : entries) {
                        model.addRow(new Object[]{
                            EVENT_DATE.format(entry.eventAt()), entry.reference(), entry.guestName(),
                            entry.roomNumber(), DATE.format(entry.checkIn()) + " " + TIME.format(entry.checkInTime())
                                    + " – " + DATE.format(entry.checkOut()) + " " + TIME.format(entry.checkOutTime()),
                            entry.reservationStatus(), entry.eventType(), entry.eventNote() == null ? "—" : entry.eventNote()
                        });
                    }
                    StaffUi.showFeedback(feedback,
                            entries.size() + (entries.size() == 1 ? " history event." : " history events."), false);
                    updateActions();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    StaffUi.showFeedback(feedback, "Loading booking history was interrupted.", true);
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    StaffUi.showFeedback(feedback, "Unable to load booking history: "
                            + (cause == null ? exception.getMessage() : cause.getMessage()), true);
                }
            }
        }.execute();
    }

    private BookingHistoryEntry selectedEntry() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selectedRow);
        return modelRow < 0 || modelRow >= entries.size() ? null : entries.get(modelRow);
    }

    private void updateActions() {
        BookingHistoryEntry selected = selectedEntry();
        recordDispute.setEnabled(selected != null && !"Disputed".equals(selected.eventType()));
    }

    private void recordDispute() {
        BookingHistoryEntry selected = selectedEntry();
        if (selected == null) {
            return;
        }
        JTextArea reason = new JTextArea(4, 38);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        int result = JOptionPane.showConfirmDialog(this, new JScrollPane(reason),
                "Reason for dispute · " + selected.reference(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        recordDispute.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                service.recordBookingDispute(currentAdmin.get(), selected.reference(), reason.getText());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refreshHistory();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    StaffUi.showFeedback(feedback, "Recording the dispute was interrupted.", true);
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    StaffUi.showFeedback(feedback, "Unable to record the dispute: "
                            + (cause == null ? exception.getMessage() : cause.getMessage()), true);
                    updateActions();
                }
            }
        }.execute();
    }

    private void showSelectedDetails() {
        BookingHistoryEntry entry = selectedEntry();
        if (entry == null) {
            return;
        }
        JTextArea details = new JTextArea(
                "Reference: " + entry.reference()
                + "\nGuest: " + entry.guestName()
                + "\nRoom: " + entry.roomNumber()
                + "\nStay: " + DATE.format(entry.checkIn()) + " " + TIME.format(entry.checkInTime())
                        + " to " + DATE.format(entry.checkOut()) + " " + TIME.format(entry.checkOutTime())
                + "\nGuests: " + entry.guestCount()
                + "\nReservation type: " + entry.reservationType()
                + "\nStatus: " + entry.reservationStatus()
                + "\nEvent: " + entry.eventType()
                + "\nEvent date: " + EVENT_DATE.format(entry.eventAt())
                + "\nNotes: " + (entry.eventNote() == null ? "—" : entry.eventNote()));
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setFont(StaffUi.BODY_FONT);
        JOptionPane.showMessageDialog(this, new JScrollPane(details),
                "Booking history details", JOptionPane.INFORMATION_MESSAGE);
    }
}
