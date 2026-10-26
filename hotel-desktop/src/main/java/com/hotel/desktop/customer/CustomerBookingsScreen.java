package com.hotel.desktop.customer;

import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.service.CustomerReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public final class CustomerBookingsScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final CustomerReservationService service;
    private final Consumer<CustomerBooking> onChange;
    private final Consumer<CustomerBooking> onPayBalance;
    private final Consumer<CustomerBooking> onViewReceipt;
    private final Runnable onUnreadCountChanged;
    private final List<CustomerBooking> bookings = new ArrayList<>();
    private final JPanel bookingList = new JPanel();
    private final JLabel feedback = StaffUi.feedbackLabel();
    private AuthenticatedUser customer;

    public CustomerBookingsScreen(
            CustomerReservationService service,
            Consumer<CustomerBooking> onChange,
            Consumer<CustomerBooking> onPayBalance,
            Consumer<CustomerBooking> onViewReceipt,
            Runnable onUnreadCountChanged) {
        this.service = java.util.Objects.requireNonNull(service);
        this.onChange = java.util.Objects.requireNonNull(onChange);
        this.onPayBalance = java.util.Objects.requireNonNull(onPayBalance);
        this.onViewReceipt = java.util.Objects.requireNonNull(onViewReceipt);
        this.onUnreadCountChanged = java.util.Objects.requireNonNull(onUnreadCountChanged);
        JPanel body = CustomerUi.createPage(this, "My bookings", "Review and manage your reservations");
        JPanel card = CustomerUi.card();
        bookingList.setOpaque(false);
        bookingList.setLayout(new javax.swing.BoxLayout(bookingList, javax.swing.BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(bookingList);
        CustomerUi.styleScrollPane(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(feedback, BorderLayout.CENTER);
        JButton retry = StaffUi.secondaryButton("Retry");
        retry.addActionListener(event -> refresh());
        footer.add(retry, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        body.add(card, BorderLayout.CENTER);
    }

    public void setCustomer(AuthenticatedUser customer) {
        this.customer = customer;
        refresh();
    }

    public void refresh() {
        if (customer == null) {
            return;
        }
        bookingList.removeAll();
        bookingList.revalidate();
        bookingList.repaint();
        StaffUi.showFeedback(feedback, "Loading your bookings…", false);
        new SwingWorker<List<CustomerBooking>, Void>() {
            @Override
            protected List<CustomerBooking> doInBackground() throws SQLException {
                return service.findBookings(customer);
            }

            @Override
            protected void done() {
                try {
                    bookings.clear();
                    bookings.addAll(get());
                    bookingList.removeAll();
                    if (bookings.isEmpty()) {
                        JLabel empty = new JLabel("No bookings found for this customer account.");
                        empty.setFont(StaffUi.bodyFont(15));
                        empty.setForeground(StaffUi.MUTED);
                        empty.setBorder(new EmptyBorder(12, 4, 12, 4));
                        bookingList.add(empty);
                    } else {
                        for (CustomerBooking booking : bookings) {
                            bookingList.add(createBookingCard(booking));
                            bookingList.add(javax.swing.Box.createVerticalStrut(12));
                        }
                    }
                    StaffUi.showFeedback(feedback,
                            bookings.size() + (bookings.size() == 1 ? " booking." : " bookings."), false);
                    bookingList.revalidate();
                    bookingList.repaint();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Loading bookings was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    showLoadError(bookingLoadError(exception.getCause()));
                }
            }
        }.execute();
    }

    private static String bookingLoadError(Throwable cause) {
        String message = cause.getMessage();
        if (cause instanceof SQLException && message != null
                && message.contains("g.discount_id_proof_path")) {
            return "Bookings could not load because the database is missing the customer booking schema. "
                    + "Ask your database administrator to back up the database, verify migrations 001 and 002, "
                    + "then apply migrations 003_customer_online_booking.sql and "
                    + "004_customer_discount_id_proof.sql in order, once each.";
        }
        return "Unable to load bookings: "
                + (message == null ? cause.getClass().getSimpleName() : message);
    }

    private void showLoadError(String message) {
        bookingList.removeAll();
        JPanel errorCard = CustomerUi.card();
        JLabel heading = new JLabel("Bookings could not be loaded");
        heading.setFont(StaffUi.bodyFont(16).deriveFont(java.awt.Font.BOLD));
        heading.setForeground(StaffUi.INK);
        JTextArea details = new JTextArea(message, 5, 30);
        details.setEditable(false);
        details.setFocusable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        details.setOpaque(false);
        details.setFont(StaffUi.bodyFont(14));
        details.setForeground(StaffUi.MUTED);
        errorCard.add(heading, BorderLayout.NORTH);
        errorCard.add(details, BorderLayout.CENTER);
        bookingList.add(errorCard);
        bookingList.revalidate();
        bookingList.repaint();
        StaffUi.showFeedback(feedback, "Unable to load bookings. Review the message above and retry.", true);
    }

    private JPanel createBookingCard(CustomerBooking booking) {
        var reservation = booking.reservation();
        JPanel card = StaffUi.card();
        JLabel heading = new JLabel("Reference " + reservation.reference()
                + "     ·     Room " + reservation.roomNumber() + " · " + reservation.roomType());
        heading.setFont(StaffUi.headingFont(19));
        heading.setForeground(StaffUi.INK);
        card.add(heading, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(0, 3, 12, 8));
        details.setOpaque(false);
        details.add(detail("Check-in",
                DATE.format(reservation.checkIn()) + " " + TIME.format(booking.checkInTime())));
        details.add(detail("Check-out",
                DATE.format(reservation.checkOut()) + " " + TIME.format(booking.checkOutTime())));
        details.add(detail("Reservation status", reservation.status().getDatabaseValue()));
        details.add(detail("Payment status", booking.paymentStatus().getDatabaseValue()));
        details.add(detail("Amount submitted", money(booking.amountSubmitted())));
        details.add(detail("Balance", money(booking.balance())));
        card.add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        JButton viewDetails = StaffUi.secondaryButton("View details");
        viewDetails.addActionListener(event -> showDetails(booking));
        actions.add(viewDetails);
        if (reservation.status() == ReservationStatus.PENDING) {
            JButton requestChange = StaffUi.secondaryButton("Request change");
            requestChange.addActionListener(event -> onChange.accept(booking));
            actions.add(requestChange);
            JButton cancel = StaffUi.secondaryButton("Cancel");
            cancel.addActionListener(event -> cancelBooking(booking));
            actions.add(cancel);
        }
        boolean canPay = booking.balance().signum() > 0
                && reservation.status() != ReservationStatus.CANCELLED
                && reservation.status() != ReservationStatus.CHECKED_OUT;
        if (canPay) {
            JButton payBalance = StaffUi.primaryButton("Pay remaining balance");
            payBalance.addActionListener(event -> onPayBalance.accept(booking));
            actions.add(payBalance);
        }
        JButton viewReceipt = StaffUi.secondaryButton("View receipt");
        viewReceipt.addActionListener(event -> onViewReceipt.accept(booking));
        actions.add(viewReceipt);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private static JPanel detail(String label, String value) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = StaffUi.fieldLabel(label);
        JLabel text = new JLabel(value);
        text.setFont(StaffUi.bodyFont(14));
        text.setForeground(StaffUi.INK);
        panel.add(title);
        panel.add(javax.swing.Box.createVerticalStrut(3));
        panel.add(text);
        return panel;
    }

    private void cancelBooking(CustomerBooking booking) {
        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel reservation " + booking.reservation().reference() + "?",
                "Cancel reservation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        StaffUi.showFeedback(feedback, "Cancelling reservation…", false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                service.cancelBooking(customer, booking.reservation().reference());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    StaffUi.showFeedback(feedback, "Reservation cancelled.", false);
                    onUnreadCountChanged.run();
                    refresh();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Cancellation was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    showError("Unable to cancel reservation: " + exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void showDetails(CustomerBooking booking) {
        var reservation = booking.reservation();
        String details = "Reference: " + reservation.reference()
                + "\nGuest: " + reservation.guest().fullName()
                + "\nClassification: " + reservation.guest().classification().getDatabaseValue()
                + "\nRoom: " + reservation.roomNumber() + " · " + reservation.roomType()
                + "\nCheck-in: " + DATE.format(reservation.checkIn()) + " " + TIME.format(booking.checkInTime())
                + "\nCheck-out: " + DATE.format(reservation.checkOut()) + " "
                        + TIME.format(booking.checkOutTime())
                + "\nReservation status: " + reservation.status().getDatabaseValue()
                + "\nPayment status: " + booking.paymentStatus().getDatabaseValue()
                + "\nAmount submitted: " + money(booking.amountSubmitted())
                + "\nRemaining balance: " + money(booking.balance());
        JOptionPane.showMessageDialog(this, details, "Booking details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        StaffUi.showFeedback(feedback, message, true);
    }

    private static String money(java.math.BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
