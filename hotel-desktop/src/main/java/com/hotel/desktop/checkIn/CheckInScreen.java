package com.hotel.desktop.checkIn;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AdminReservationDetails;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.enums.ReservationType;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import javax.swing.table.DefaultTableModel;

public final class CheckInScreen extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    private final ReservationService service;
    private final Supplier<AuthenticatedUser> currentAdmin;
    private final Runnable onReservationChanged;
    private final List<Reservation> arrivals = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(new Object[0][6],
            new String[]{"Reference", "Guest", "Room", "Guests", "Check-in", "Check-out"}) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel guestName = new JLabel("Select an arrival to review check-in.");
    private final JLabel guestDetails = new JLabel(" ");
    private final JLabel billSummary = new JLabel(" ");
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final JTextField balancePayment = StaffUi.textField(10);
    private final JComboBox<String> paymentMethod = new JComboBox<>(
            new String[]{"Cash", "GCash", "Bank Transfer", "Card / Other"});
    private AdminReservationDetails selectedDetails;

    public CheckInScreen(ReservationService service, Supplier<AuthenticatedUser> currentAdmin,
            Runnable onReservationChanged) {
        this.service = java.util.Objects.requireNonNull(service);
        this.currentAdmin = java.util.Objects.requireNonNull(currentAdmin);
        this.onReservationChanged = java.util.Objects.requireNonNull(onReservationChanged);
        JPanel body = StaffUi.createPage(this, "Check in",
                "Review arrival details, collect any remaining online balance, then confirm check-in");
        body.add(buildContent(), BorderLayout.CENTER);
        StaffUi.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) showSelectedArrival();
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
        content.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 8));
        footer.setOpaque(false);
        JPanel guest = new JPanel();
        guest.setOpaque(false);
        guest.setLayout(new javax.swing.BoxLayout(guest, javax.swing.BoxLayout.Y_AXIS));
        guestName.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        guestName.setForeground(StaffUi.INK);
        guestDetails.setFont(StaffUi.bodyFont(13));
        guestDetails.setForeground(StaffUi.MUTED);
        billSummary.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        billSummary.setForeground(StaffUi.INK);
        guest.add(guestName);
        guest.add(guestDetails);
        guest.add(billSummary);
        footer.add(guest, BorderLayout.NORTH);

        JPanel payment = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        payment.setOpaque(false);
        payment.add(StaffUi.fieldLabel("Balance collected (PHP)"));
        balancePayment.setColumns(10);
        payment.add(balancePayment);
        payment.add(StaffUi.fieldLabel("Method"));
        paymentMethod.setFont(StaffUi.bodyFont(13));
        payment.add(paymentMethod);
        footer.add(payment, BorderLayout.CENTER);
        footer.add(feedback, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton confirm = StaffUi.primaryButton("Confirm check-in");
        confirm.addActionListener(event -> checkInSelected());
        actions.add(confirm);
        footer.add(actions, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    public void refreshArrivals() {
        AuthenticatedUser admin = currentAdmin.get();
        if (admin == null || !admin.getRole().canAccessAdminWorkspace()) return;
        try {
            arrivals.clear();
            arrivals.addAll(service.findArrivals(admin, LocalDate.now()));
            model.setRowCount(0);
            for (Reservation reservation : arrivals) {
                model.addRow(new Object[]{reservation.reference(), reservation.guest().fullName(),
                        reservation.roomNumber(), reservation.guestCount(), DATE.format(reservation.checkIn()),
                        DATE.format(reservation.checkOut())});
            }
            clearDetails();
            StaffUi.showFeedback(feedback, arrivals.size()
                    + (arrivals.size() == 1 ? " arrival ready." : " arrivals ready."), false);
        } catch (SQLException exception) {
            arrivals.clear();
            model.setRowCount(0);
            clearDetails();
            StaffUi.showFeedback(feedback, "Unable to load arrivals: " + exception.getMessage(), true);
        }
    }

    private void showSelectedArrival() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { clearDetails(); return; }
        Reservation reservation = arrivals.get(table.convertRowIndexToModel(viewRow));
        try {
            selectedDetails = service.findAdminReservationDetails(currentAdmin.get(), reservation.reference());
            var booking = selectedDetails.booking();
            BigDecimal balance = booking.total().subtract(selectedDetails.verifiedAmount())
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            guestName.setText(reservation.guest().fullName() + " · Reservation " + reservation.reference());
            guestDetails.setText("Room " + reservation.roomNumber() + " · " + reservation.guestCount()
                    + " guest(s) · " + reservation.guest().classification().getDatabaseValue()
                    + " · Check-in " + DATE.format(reservation.checkIn()) + " " + booking.checkInTime());
            billSummary.setText("Total: PHP " + money(booking.total()) + " · Downpayment verified: PHP "
                    + money(selectedDetails.verifiedAmount()) + " · Remaining balance: PHP " + money(balance));
            balancePayment.setText(money(balance));
            boolean collect = reservation.type() == ReservationType.ONLINE && balance.signum() > 0;
            balancePayment.setEnabled(collect);
            paymentMethod.setEnabled(collect);
        } catch (SQLException | ValidationException exception) {
            clearDetails();
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        }
    }

    private void checkInSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || selectedDetails == null) {
            StaffUi.showFeedback(feedback, "Select an arrival before checking in.", true);
            return;
        }
        Reservation reservation = arrivals.get(table.convertRowIndexToModel(row));
        try {
            var booking = selectedDetails.booking();
            BigDecimal balance = booking.total().subtract(selectedDetails.verifiedAmount())
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            String method = String.valueOf(paymentMethod.getSelectedItem());
            String enteredText = balancePayment.getText().trim().replace(",", "");
            BigDecimal entered = enteredText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(enteredText);
            int choice = JOptionPane.showConfirmDialog(this,
                    "Confirm check-in for " + reservation.guest().fullName() + "?\n"
                            + "Total room charge: PHP " + money(booking.total()) + "\n"
                            + "Downpayment verified: PHP " + money(selectedDetails.verifiedAmount()) + "\n"
                            + "Balance to collect: PHP " + money(balance)
                            + (balance.signum() > 0 ? "\nPayment method: " + method : ""),
                    "Confirm check-in", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice != JOptionPane.YES_OPTION) return;
            Reservation checkedIn;
            if (reservation.type() == ReservationType.ONLINE && balance.signum() > 0) {
                checkedIn = service.collectBalanceAndCheckIn(currentAdmin.get(), reservation.reference(),
                        entered, method.equals("Card / Other") ? "Other" : method);
            } else {
                checkedIn = service.checkIn(currentAdmin.get(), reservation.reference(), LocalDate.now());
            }
            onReservationChanged.run();
            refreshArrivals();
            StaffUi.showFeedback(feedback, checkedIn.guest().fullName() + " checked in to room "
                    + checkedIn.roomNumber() + ".", false);
        } catch (NumberFormatException | ArithmeticException exception) {
            StaffUi.showFeedback(feedback, "Enter the exact remaining balance with up to two decimals.", true);
        } catch (ValidationException exception) {
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        } catch (SQLException exception) {
            StaffUi.showFeedback(feedback, "Unable to check in guest: " + exception.getMessage(), true);
        }
    }

    private void clearDetails() {
        selectedDetails = null;
        guestName.setText("Select an arrival to review check-in.");
        guestDetails.setText(" ");
        billSummary.setText(" ");
        balancePayment.setText("");
        balancePayment.setEnabled(false);
        paymentMethod.setEnabled(false);
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
