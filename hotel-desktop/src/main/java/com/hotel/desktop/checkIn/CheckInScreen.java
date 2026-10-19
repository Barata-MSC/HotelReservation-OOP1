package com.hotel.desktop.checkIn;

import com.hotel.core.exception.RoomNotReadyException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public final class CheckInScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final ReservationService reservationService;
    private final Runnable onReservationChanged;
    private final List<Reservation> arrivals = new ArrayList<>();
    private final JTextField searchField = StaffUi.textField(22);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[0][7],
            new String[]{"Reference", "Guest", "Room", "Guests", "Check-in", "Check-out", "Status"}) {
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
    private JButton checkInButton;
    private JButton findButton;
    private JButton refreshButton;
    private boolean operationPending;

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
        JLabel title = new JLabel("Today's arrivals");
        title.setFont(new java.awt.Font("Georgia", java.awt.Font.BOLD, 21));
        title.setForeground(StaffUi.INK);
        header.add(title, BorderLayout.WEST);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        search.setOpaque(false);
        searchField.setToolTipText("Search by reservation reference or guest name");
        searchField.addActionListener(event -> searchReservations());
        findButton = StaffUi.secondaryButton("Find");
        findButton.addActionListener(event -> searchReservations());
        search.add(searchField);
        search.add(findButton);
        refreshButton = StaffUi.secondaryButton("Refresh");
        refreshButton.addActionListener(event -> refreshArrivals());
        search.add(refreshButton);
        header.add(search, BorderLayout.EAST);
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
        checkInButton = StaffUi.primaryButton("Check in guest");
        checkInButton.addActionListener(event -> checkInSelected());
        actions.add(checkInButton);
        footer.add(actions, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    public void refreshArrivals() {
        if (operationPending) {
            return;
        }
        setOperationPending(true, "Loading arrivals…");
        StaffUi.runInBackground(
                () -> reservationService.findArrivals(LocalDate.now()),
                loaded -> {
                    setOperationPending(false, null);
                    displayReservations(loaded);
                    StaffUi.showFeedback(feedback, arrivals.size()
                            + (arrivals.size() == 1 ? " arrival ready." : " arrivals ready."), false);
                },
                error -> {
                    setOperationPending(false, null);
                    displayReservations(List.of());
                    StaffUi.showFeedback(feedback,
                            "Unable to load arrivals: " + error.getMessage(), true);
                });
    }

    private void searchReservations() {
        if (operationPending) {
            return;
        }
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            refreshArrivals();
            return;
        }
        setOperationPending(true, "Searching reservations…");
        StaffUi.runInBackground(
                () -> reservationService.findReservationsForCheckIn(query),
                matches -> {
                    setOperationPending(false, null);
                    displayReservations(matches);
                    StaffUi.showFeedback(feedback, matches.isEmpty()
                            ? "No reservations matched that reference or guest name."
                            : matches.size() + (matches.size() == 1
                                    ? " reservation found." : " reservations found."),
                            matches.isEmpty());
                },
                error -> {
                    setOperationPending(false, null);
                    displayReservations(List.of());
                    StaffUi.showFeedback(feedback, error.getMessage(), true);
                });
    }

    private void setOperationPending(boolean pending, String message) {
        operationPending = pending;
        checkInButton.setEnabled(!pending && !arrivals.isEmpty());
        findButton.setEnabled(!pending);
        refreshButton.setEnabled(!pending);
        searchField.setEnabled(!pending);
        if (message != null) {
            StaffUi.showFeedback(feedback, message, false);
        }
    }

    private void displayReservations(List<Reservation> reservations) {
        arrivals.clear();
        arrivals.addAll(reservations);
        model.setRowCount(0);
        for (Reservation reservation : arrivals) {
            model.addRow(new Object[]{
                reservation.reference(),
                reservation.guest().fullName(),
                reservation.roomNumber(),
                reservation.guestCount(),
                DATE_FORMAT.format(reservation.checkIn()),
                DATE_FORMAT.format(reservation.checkOut()),
                reservation.status().getDatabaseValue()
            });
        }
        guestName.setText("Select a reservation to review check-in.");
        guestDetails.setText(" ");
        checkInButton.setEnabled(!arrivals.isEmpty());
    }

    private void showSelectedArrival() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= arrivals.size()) {
            guestName.setText("Select a reservation to review check-in.");
            guestDetails.setText(" ");
            return;
        }
        Reservation reservation = arrivals.get(table.convertRowIndexToModel(selected));
        guestName.setText(reservation.guest().fullName() + " · Reservation " + reservation.reference());
        guestDetails.setText("Room " + reservation.roomNumber() + " (" + reservation.roomType()
                + ") · " + reservation.guestCount() + " guest(s) · "
                + reservation.guest().classification().getDatabaseValue() + " · "
                + reservation.status().getDatabaseValue());
    }

    private void checkInSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= arrivals.size()) {
            StaffUi.showFeedback(feedback, "Select an arrival before checking in.", true);
            return;
        }
        Reservation reservation = arrivals.get(table.convertRowIndexToModel(selected));
        if (reservation.status() == ReservationStatus.PENDING) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "This reservation is Pending. Confirm it and re-check room availability before check-in?",
                    "Confirm pending reservation", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        boolean discountVerified = verifyDiscountId(reservation);
        checkInReservation(reservation, null, discountVerified);
    }

    private boolean verifyDiscountId(Reservation reservation) {
        if (reservation.guest().classification() != GuestClassification.DISCOUNTED) {
            return false;
        }
        if (!reservation.guest().discountVerified() || !reservation.guest().discountNameMatches()
                || reservation.guest().discountIdExpiry() == null
                || reservation.guest().discountIdExpiry().isBefore(LocalDate.now())) {
            JOptionPane.showMessageDialog(this,
                    "The discount ID details are missing, expired, or were not verified. "
                            + "This guest will be billed as Regular.",
                    "Discount not eligible", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        String details = "Verify the original " + reservation.guest().discountIdType() + " ID, "
                + "confirm its name matches " + reservation.guest().fullName()
                + ", and confirm it is unexpired (through "
                + reservation.guest().discountIdExpiry() + "). Apply the discount?";
        return JOptionPane.showConfirmDialog(this, details, "Verify discount eligibility",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void checkInReservation(
            Reservation reservation, Integer replacementRoomNumber, boolean discountVerified) {
        if (operationPending) {
            return;
        }
        setOperationPending(true, "Checking in guest…");
        StaffUi.runInBackground(
                () -> reservationService.checkIn(
                        reservation.reference(), LocalDate.now(),
                        replacementRoomNumber, discountVerified),
                checkedIn -> {
                    setOperationPending(false, null);
                    onReservationChanged.run();
                    StaffUi.showFeedback(feedback,
                            checkedIn.guest().fullName() + " checked in to room "
                                    + checkedIn.roomNumber() + ".", false);
                    if (searchField.getText().isBlank()) {
                        refreshArrivals();
                    } else {
                        searchReservations();
                    }
                },
                error -> {
                    setOperationPending(false, null);
                    if (error instanceof RoomNotReadyException notReady) {
                        offerReplacementRoom(reservation, discountVerified, notReady);
                    } else {
                        StaffUi.showFeedback(feedback,
                                error instanceof ValidationException
                                        ? error.getMessage()
                                        : "Unable to check in guest: " + error.getMessage(),
                                true);
                    }
                });
    }

    private void offerReplacementRoom(
            Reservation reservation, boolean discountVerified, RoomNotReadyException reason) {
        setOperationPending(true, "Checking replacement-room availability…");
        StaffUi.runInBackground(
                () -> reservationService.findReplacementRooms(reservation.reference()),
                replacements -> {
                    setOperationPending(false, null);
                    if (replacements.isEmpty()) {
                        StaffUi.showFeedback(feedback,
                                reason.getMessage() + " No replacement room is available; the guest "
                                        + "must wait for housekeeping.",
                                true);
                        return;
                    }
                    promptForReplacement(reservation, discountVerified, reason, replacements);
                },
                error -> {
                    setOperationPending(false, null);
                    StaffUi.showFeedback(feedback,
                            "Unable to check replacement-room availability: " + error.getMessage(), true);
                });
    }

    private void promptForReplacement(
            Reservation reservation,
            boolean discountVerified,
            RoomNotReadyException reason,
            List<Room> replacements) {
        JComboBox<Room> roomChoice = new JComboBox<>(replacements.toArray(Room[]::new));
        roomChoice.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = new JLabel(value == null ? "" : "Room " + value.getRoomNumber()
                    + " · " + value.getRoomType().getDatabaseValue() + " · "
                    + value.getCapacity() + " guest(s)");
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        JPanel prompt = new JPanel(new BorderLayout(8, 8));
        prompt.add(new JLabel(reason.getMessage()
                + " Choose a replacement room or wait for housekeeping."), BorderLayout.NORTH);
        prompt.add(roomChoice, BorderLayout.CENTER);
        Object[] options = {"Use replacement room", "Wait for housekeeping", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, prompt, "Room not ready",
                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);
        if (choice == 1) {
            StaffUi.showFeedback(feedback,
                    "Check-in was not completed. The guest must wait for housekeeping.", true);
        } else if (choice == 0 && roomChoice.getSelectedItem() instanceof Room replacement) {
            checkInReservation(reservation, replacement.getRoomNumber(), discountVerified);
        }
    }
}
