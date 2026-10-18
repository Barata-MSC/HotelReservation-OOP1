package com.hotel.desktop.checkIn;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Reservation;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public final class CheckInScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final ReservationService reservationService;
    private final Runnable onReservationChanged;
    private final List<Reservation> arrivals = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[0][6],
            new String[]{"Reference", "Guest", "Room", "Guests", "Check-in", "Check-out"}) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel guestName = new JLabel("Select an arrival to review check-in.");
    private final JLabel guestDetails = new JLabel(" ");
    private final JLabel feedback = StaffUi.feedbackLabel();

    public CheckInScreen(ReservationService reservationService, Runnable onReservationChanged) {
        this.reservationService = java.util.Objects.requireNonNull(reservationService, "reservationService");
        this.onReservationChanged = java.util.Objects.requireNonNull(onReservationChanged, "onReservationChanged");
        JPanel body = StaffUi.createPage(this, "Check in",
                "Check in confirmed arrivals after verifying their reservation and room readiness");
        body.add(buildContent(), BorderLayout.CENTER);
        StaffUi.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedArrival();
            }
        });
        refreshArrivals();
    }

    private JPanel buildContent() {
        JPanel content = StaffUi.card();
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Today's confirmed arrivals");
        title.setFont(new java.awt.Font("Georgia", java.awt.Font.BOLD, 21));
        title.setForeground(StaffUi.INK);
        header.add(title, BorderLayout.WEST);
        JButton refresh = StaffUi.secondaryButton("Refresh");
        refresh.addActionListener(event -> refreshArrivals());
        header.add(refresh, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, StaffUi.INK));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        content.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 8));
        footer.setOpaque(false);
        JPanel guest = new JPanel();
        guest.setOpaque(false);
        guest.setLayout(new javax.swing.BoxLayout(guest, javax.swing.BoxLayout.Y_AXIS));
        guestName.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        guestName.setForeground(StaffUi.INK);
        guestDetails.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        guestDetails.setForeground(StaffUi.MUTED);
        guest.add(guestName);
        guest.add(guestDetails);
        footer.add(guest, BorderLayout.NORTH);
        footer.add(feedback, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton checkIn = StaffUi.primaryButton("Check in guest");
        checkIn.addActionListener(event -> checkInSelected());
        actions.add(checkIn);
        footer.add(actions, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    public void refreshArrivals() {
        try {
            List<Reservation> loaded = reservationService.findArrivals(LocalDate.now());
            arrivals.clear();
            arrivals.addAll(loaded);
            model.setRowCount(0);
            for (Reservation reservation : arrivals) {
                model.addRow(new Object[]{
                    reservation.reference(),
                    reservation.guest().fullName(),
                    reservation.roomNumber(),
                    reservation.guestCount(),
                    DATE_FORMAT.format(reservation.checkIn()),
                    DATE_FORMAT.format(reservation.checkOut())
                });
            }
            guestName.setText("Select an arrival to review check-in.");
            guestDetails.setText(" ");
            StaffUi.showFeedback(feedback, arrivals.size()
                    + (arrivals.size() == 1 ? " arrival ready." : " arrivals ready."), false);
        } catch (SQLException exception) {
            arrivals.clear();
            model.setRowCount(0);
            guestName.setText("Select an arrival to review check-in.");
            guestDetails.setText(" ");
            StaffUi.showFeedback(feedback, "Unable to load arrivals: " + exception.getMessage(), true);
        }
    }

    private void showSelectedArrival() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= arrivals.size()) {
            guestName.setText("Select an arrival to review check-in.");
            guestDetails.setText(" ");
            return;
        }
        Reservation reservation = arrivals.get(table.convertRowIndexToModel(selected));
        guestName.setText(reservation.guest().fullName() + " · Reservation " + reservation.reference());
        guestDetails.setText("Room " + reservation.roomNumber() + " (" + reservation.roomType()
                + ") · " + reservation.guestCount() + " guest(s) · "
                + reservation.guest().classification().getDatabaseValue());
    }

    private void checkInSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= arrivals.size()) {
            StaffUi.showFeedback(feedback, "Select an arrival before checking in.", true);
            return;
        }
        Reservation reservation = arrivals.get(table.convertRowIndexToModel(selected));
        try {
            Reservation checkedIn = reservationService.checkIn(reservation.reference(), LocalDate.now());
            onReservationChanged.run();
            refreshArrivals();
            StaffUi.showFeedback(feedback,
                    checkedIn.guest().fullName() + " checked in to room "
                            + checkedIn.roomNumber() + ".", false);
        } catch (ValidationException exception) {
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        } catch (SQLException exception) {
            StaffUi.showFeedback(feedback,
                    "Unable to check in guest: " + exception.getMessage(), true);
        }
    }
}
