package com.hotel.desktop.checkOut;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.Reservation;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/** Checkout only changes the stay and room state. All payments are collected at check-in. */
public final class CheckOutScreen extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_INPUT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final ReservationService reservationService;
    private final Supplier<AuthenticatedUser> currentAdmin;
    private final Runnable onReservationChanged;
    private final List<Reservation> checkedIn = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[0][5], new String[]{"Reference", "Guest", "Room", "Departure", "Status"}) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel guestLabel = new JLabel("Select a checked-in guest.");
    private final JLabel feedback = StaffUi.feedbackLabel();
    private Reservation selectedReservation;
    private JButton checkoutButton;
    private JButton forceCheckoutButton;
    private JButton extendButton;

    public CheckOutScreen(ReservationService reservationService,
            Supplier<AuthenticatedUser> currentAdmin, Runnable onReservationChanged) {
        this.reservationService = java.util.Objects.requireNonNull(reservationService, "reservationService");
        this.currentAdmin = java.util.Objects.requireNonNull(currentAdmin, "currentAdmin");
        this.onReservationChanged = java.util.Objects.requireNonNull(onReservationChanged, "onReservationChanged");
        JPanel body = StaffUi.createPage(this, "Check-out",
                "Confirm the guest departure and send the room to housekeeping; payments are handled at check-in");
        body.add(buildContent(), BorderLayout.CENTER);
        StaffUi.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) showSelectedReservation();
        });
        refreshReservations();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        JPanel listCard = StaffUi.card();
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("Guests currently checked in");
        title.setFont(new java.awt.Font("Georgia", java.awt.Font.BOLD, 21));
        title.setForeground(StaffUi.INK);
        heading.add(title, BorderLayout.WEST);
        JButton refresh = StaffUi.secondaryButton("Refresh");
        refresh.addActionListener(event -> refreshReservations());
        heading.add(refresh, BorderLayout.EAST);
        listCard.add(heading, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, StaffUi.INK));
        scroll.getViewport().setBackground(java.awt.Color.WHITE);
        listCard.add(scroll, BorderLayout.CENTER);
        content.add(listCard, BorderLayout.CENTER);

        JPanel checkoutCard = StaffUi.card();
        checkoutCard.setLayout(new BorderLayout(0, 12));
        guestLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        guestLabel.setForeground(StaffUi.INK);
        checkoutCard.add(guestLabel, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        forceCheckoutButton = StaffUi.secondaryButton("Force check-out");
        forceCheckoutButton.addActionListener(event -> forceCheckOut());
        extendButton = StaffUi.secondaryButton("Extend stay");
        extendButton.addActionListener(event -> extendStay());
        checkoutButton = StaffUi.primaryButton("Complete check-out");
        checkoutButton.addActionListener(event -> completeCheckout());
        actions.add(forceCheckoutButton);
        actions.add(extendButton);
        actions.add(checkoutButton);
        checkoutCard.add(actions, BorderLayout.SOUTH);
        content.add(checkoutCard, BorderLayout.SOUTH);

        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.setOpaque(false);
        page.add(content, BorderLayout.CENTER);
        page.add(feedback, BorderLayout.SOUTH);
        return page;
    }

    public void refreshReservations() {
        AuthenticatedUser admin = currentAdmin.get();
        if (admin == null || !admin.getRole().canAccessAdminWorkspace()) return;
        try {
            checkedIn.clear();
            checkedIn.addAll(reservationService.findCheckedInReservations(admin));
            model.setRowCount(0);
            for (Reservation reservation : checkedIn) {
                model.addRow(new Object[]{reservation.reference(), reservation.guest().fullName(),
                    reservation.roomNumber(), DATE_FORMAT.format(reservation.checkOut()),
                    reservation.status().getDatabaseValue()});
            }
            selectedReservation = null;
            guestLabel.setText("Select a checked-in guest.");
            checkoutButton.setEnabled(false);
            forceCheckoutButton.setEnabled(false);
            extendButton.setEnabled(false);
            StaffUi.showFeedback(feedback, checkedIn.size()
                    + (checkedIn.size() == 1 ? " guest in house." : " guests in house."), false);
        } catch (SQLException exception) {
            checkedIn.clear();
            model.setRowCount(0);
            selectedReservation = null;
            StaffUi.showFeedback(feedback,
                    "Unable to load checked-in guests: " + exception.getMessage(), true);
        }
    }

    private void showSelectedReservation() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= checkedIn.size()) return;
        selectedReservation = checkedIn.get(table.convertRowIndexToModel(selected));
        guestLabel.setText("Ready to check out: " + selectedReservation.guest().fullName()
                + " · Room " + selectedReservation.roomNumber() + " · Departure "
                + DATE_FORMAT.format(selectedReservation.checkOut())
                + ". No payment is collected on this screen.");
        checkoutButton.setEnabled(true);
        forceCheckoutButton.setEnabled(true);
        extendButton.setEnabled(true);
        StaffUi.showFeedback(feedback, "Select Complete check-out to mark the room Dirty.", false);
    }

    private void extendStay() {
        if (selectedReservation == null) {
            StaffUi.showFeedback(feedback, "Select a checked-in guest first.", true);
            return;
        }
        Reservation reservation = selectedReservation;
        try {
            var details = reservationService.findAdminReservationDetails(
                    currentAdmin.get(), reservation.reference());
            LocalTime currentCheckoutTime = details.booking().checkOutTime();
            JTextField dateField = StaffUi.textField(12);
            dateField.setText(reservation.checkOut().plusDays(1).toString());
            JTextField timeField = StaffUi.textField(10);
            timeField.setText(currentCheckoutTime.format(TIME_FORMAT));
            JComboBox<String> methodBox = new JComboBox<>(
                    new String[]{"Cash", "GCash", "Bank Transfer", "Card / Other"});
            methodBox.setFont(StaffUi.bodyFont(13));
            JPanel form = new JPanel(new java.awt.GridBagLayout());
            form.setPreferredSize(new java.awt.Dimension(420, 128));
            addFormField(form, 0, "New check-out date (yyyy-MM-dd)", dateField);
            addFormField(form, 1, "New check-out time (hh:mm AM/PM)", timeField);
            addFormField(form, 2, "Extension payment method", methodBox);
            int choice = JOptionPane.showConfirmDialog(this, form,
                    "Extend stay for " + reservation.guest().fullName(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return;

            LocalDateTime proposedCheckout = LocalDateTime.of(
                    LocalDate.parse(dateField.getText().trim()),
                    LocalTime.parse(timeField.getText().trim(), TIME_INPUT));
            var quote = reservationService.quoteStayExtension(
                    currentAdmin.get(), reservation.reference(), proposedCheckout);
            String message = "Current checkout: " + quote.currentCheckout().format(DATE_FORMAT)
                    + " " + quote.currentCheckout().format(TIME_FORMAT) + "\n"
                    + "New checkout: " + quote.proposedCheckout().format(DATE_FORMAT)
                    + " " + quote.proposedCheckout().format(TIME_FORMAT) + "\n"
                    + "Current total: PHP " + money(quote.currentTotal()) + "\n"
                    + "New total: PHP " + money(quote.proposedTotal()) + "\n"
                    + "Extension charge: PHP " + money(quote.extensionCharge())
                    + (quote.extensionCharge().signum() > 0
                            ? "\nPayment method: " + methodBox.getSelectedItem() : "\nNo additional night charge")
                    + "\n\nThe system will reject the extension if another reservation conflicts.";
            int confirmed = JOptionPane.showConfirmDialog(this, message,
                    "Confirm stay extension", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirmed != JOptionPane.YES_OPTION) return;

            String method = String.valueOf(methodBox.getSelectedItem());
            reservationService.extendStay(currentAdmin.get(), quote,
                    method.equals("Card / Other") ? "Other" : method);
            onReservationChanged.run();
            refreshReservations();
            StaffUi.showFeedback(feedback,
                    "Stay extended through " + proposedCheckout.format(DATE_FORMAT) + " "
                            + proposedCheckout.format(TIME_FORMAT) + ".", false);
        } catch (DateTimeParseException exception) {
            StaffUi.showFeedback(feedback, "Enter the check-out date as yyyy-MM-dd and time as hh:mm AM/PM.", true);
        } catch (ValidationException exception) {
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        } catch (SQLException exception) {
            StaffUi.showFeedback(feedback, "Unable to extend the stay: " + exception.getMessage(), true);
        }
    }

    private static void addFormField(JPanel panel, int row, String labelText, java.awt.Component input) {
        java.awt.GridBagConstraints label = new java.awt.GridBagConstraints();
        label.gridx = 0;
        label.gridy = row;
        label.anchor = java.awt.GridBagConstraints.WEST;
        label.insets = new java.awt.Insets(5, 4, 5, 12);
        panel.add(StaffUi.fieldLabel(labelText), label);
        java.awt.GridBagConstraints field = new java.awt.GridBagConstraints();
        field.gridx = 1;
        field.gridy = row;
        field.weightx = 1;
        field.fill = java.awt.GridBagConstraints.HORIZONTAL;
        field.insets = new java.awt.Insets(5, 0, 5, 4);
        panel.add(input, field);
    }

    private static String money(java.math.BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private void completeCheckout() {
        if (selectedReservation == null) {
            StaffUi.showFeedback(feedback, "Select a checked-in guest first.", true);
            return;
        }
        Reservation reservation = selectedReservation;
        int confirmed = JOptionPane.showConfirmDialog(this,
                "Complete check-out for " + reservation.guest().fullName() + " in room "
                        + reservation.roomNumber() + "? The room will be marked Dirty.",
                "Confirm check-out", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirmed != JOptionPane.YES_OPTION) return;
        try {
            reservationService.completeCheckOut(currentAdmin.get(), reservation.reference());
            onReservationChanged.run();
            refreshReservations();
            StaffUi.showFeedback(feedback,
                    "Check-out complete. Room " + reservation.roomNumber() + " is Dirty for housekeeping.", false);
        } catch (ValidationException exception) {
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        } catch (SQLException exception) {
            StaffUi.showFeedback(feedback, "Unable to complete check-out: " + exception.getMessage(), true);
        }
    }

    private void forceCheckOut() {
        if (selectedReservation == null) {
            StaffUi.showFeedback(feedback, "Select a checked-in guest first.", true);
            return;
        }
        String reason = JOptionPane.showInputDialog(this,
                "Enter the reason for checking out this guest:", "Force check-out", JOptionPane.WARNING_MESSAGE);
        if (reason == null) return;
        String normalizedReason = reason.trim();
        if (normalizedReason.isEmpty()) {
            StaffUi.showFeedback(feedback, "A reason is required for a forced check-out.", true);
            return;
        }
        String reference = selectedReservation.reference();
        forceCheckoutButton.setEnabled(false);
        checkoutButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                reservationService.forceCheckOut(currentAdmin.get(), reference, normalizedReason);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    onReservationChanged.run();
                    refreshReservations();
                    StaffUi.showFeedback(feedback,
                            "Guest checked out. The reason was saved and the room is Dirty for housekeeping.", false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    StaffUi.showFeedback(feedback, "Forced check-out was interrupted.", true);
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    StaffUi.showFeedback(feedback,
                            cause == null ? exception.getMessage() : cause.getMessage(), true);
                }
            }
        }.execute();
    }
}
