package com.hotel.desktop.customer;

import com.hotel.core.model.CustomerReceipt;
import com.hotel.core.model.Reservation;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public final class CustomerReceiptScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color INK = new Color(28, 30, 37);
    private static final Color MUTED = new Color(104, 108, 117);
    private static final Color LINE = new Color(226, 228, 232);
    private final JPanel receiptContent = new JPanel(new BorderLayout());
    private final JPanel receiptRows = new JPanel(new GridBagLayout());
    private CustomerReceipt receipt;
    private final Runnable onBack;

    public CustomerReceiptScreen(Runnable onBack) {
        this.onBack = java.util.Objects.requireNonNull(onBack);
        JPanel body = StaffUi.createPage(this, "Booking receipt", "Reservation and payment summary");
        JPanel card = StaffUi.card();
        receiptContent.setBackground(Color.WHITE);
        receiptRows.setBackground(Color.WHITE);
        receiptRows.setBorder(new EmptyBorder(24, 30, 24, 30));
        receiptContent.add(receiptRows, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(receiptContent);
        scroll.setBorder(javax.swing.BorderFactory.createLineBorder(LINE));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(680, 475));
        card.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        JButton back = StaffUi.secondaryButton("Back to My Bookings");
        back.addActionListener(event -> onBack.run());
        JButton download = StaffUi.primaryButton("Download receipt PDF");
        download.addActionListener(event -> downloadReceipt());
        actions.add(back, BorderLayout.WEST);
        actions.add(download, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);
        body.add(card, BorderLayout.CENTER);
    }

    public void setReceipt(CustomerReceipt receipt) {
        this.receipt = java.util.Objects.requireNonNull(receipt);
        this.receiptContent.removeAll();
        this.receiptRows.removeAll();
        Reservation reservation = receipt.booking().reservation();
        JPanel heading = new JPanel(new BorderLayout(0, 8));
        heading.setBackground(INK);
        heading.setBorder(new EmptyBorder(22, 30, 22, 30));
        JLabel hotel = new JLabel("HOTEL RESERVATION");
        hotel.setFont(new Font("Georgia", Font.BOLD, 22));
        hotel.setForeground(Color.WHITE);
        JLabel reference = new JLabel("Receipt  ·  " + reservation.reference());
        reference.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        reference.setForeground(new Color(223, 225, 230));
        JPanel headingText = new JPanel(new BorderLayout(0, 6));
        headingText.setOpaque(false);
        headingText.add(hotel, BorderLayout.NORTH);
        headingText.add(reference, BorderLayout.CENTER);
        JLabel status = new JLabel(receipt.paymentStatus().getDatabaseValue().toUpperCase(java.util.Locale.ROOT));
        status.setFont(new Font("Segoe UI", Font.BOLD, 11));
        status.setForeground(new Color(255, 204, 0));
        heading.add(headingText, BorderLayout.CENTER);
        heading.add(status, BorderLayout.EAST);
        this.receiptContent.add(heading, BorderLayout.NORTH);

        int row = 0;
        row = addSection("STAY DETAILS", row);
        LocalDateTime issuedAt = LocalDateTime.now();
        row = addReceiptRow("Date issued", DATE.format(issuedAt.toLocalDate()) + "  "
                + TIME.format(issuedAt.toLocalTime()), row, false);
        row = addReceiptRow("Guest", reservation.guest().fullName(), row, false);
        row = addReceiptRow("Contact", reservation.guest().contactNumber(), row, false);
        row = addReceiptRow("Email", reservation.guest().email(), row, false);
        row = addReceiptRow("Classification", reservation.guest().classification().getDatabaseValue(), row, false);
        row = addReceiptRow("Room", reservation.roomNumber() + "  ·  " + reservation.roomType(), row, false);
        row = addReceiptRow("Check-in", DATE.format(reservation.checkIn()) + "  "
                + TIME.format(receipt.booking().checkInTime()), row, false);
        row = addReceiptRow("Check-out", DATE.format(reservation.checkOut()) + "  "
                + TIME.format(receipt.booking().checkOutTime()), row, false);
        row = addReceiptRow("Nights", Integer.toString(receipt.bill().nights()), row, false);
        row = addSection("CHARGES", row);
        row = addReceiptRow("Rate per night", money(receipt.bill().roomRate()), row, false);
        row = addReceiptRow("Room cost", money(receipt.bill().roomCost()), row, false);
        row = addReceiptRow("Discount", "-" + money(receipt.bill().discountAmount()), row, false);
        row = addReceiptRow("Tax", money(receipt.bill().taxAmount()), row, false);
        row = addReceiptRow("Total", money(receipt.bill().total()), row, true);
        row = addSection("PAYMENT", row);
        row = addReceiptRow("Amount paid", money(receipt.amountPaid()), row, false);
        row = addReceiptRow("Remaining balance", money(receipt.remainingBalance()), row, false);
        row = addReceiptRow("Payment method", receipt.paymentMethod(), row, false);
        addReceiptRow("Payment status", receipt.paymentStatus().getDatabaseValue(), row, false);
        this.receiptContent.revalidate();
        this.receiptContent.repaint();
    }

    private void downloadReceipt() {
        if (receipt == null) {
            javax.swing.JOptionPane.showMessageDialog(
                    this, "Receipt data is not available.",
                    "Receipt unavailable", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }
        ReceiptPdfExporter.chooseAndExport(
                this,
                "receipt-" + receipt.booking().reservation().reference(),
                "Hotel Reservation Receipt",
                receiptLines(receipt));
    }

    private static List<String> receiptLines(CustomerReceipt receipt) {
        Reservation reservation = receipt.booking().reservation();
        return List.of(
                "BOOKING & GUEST",
                "Receipt reference: " + reservation.reference(),
                "Date issued: " + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()),
                "Guest: " + reservation.guest().fullName(),
                "Contact: " + reservation.guest().contactNumber(),
                "Email: " + reservation.guest().email(),
                "Classification: " + reservation.guest().classification().getDatabaseValue(),
                "Room: " + reservation.roomNumber() + " · " + reservation.roomType(),
                "Check-in: " + DATE.format(reservation.checkIn()) + " " + TIME.format(receipt.booking().checkInTime()),
                "Check-out: " + DATE.format(reservation.checkOut()) + " " + TIME.format(receipt.booking().checkOutTime()),
                "Nights: " + receipt.bill().nights(),
                "CHARGE SUMMARY",
                "Rate per night: " + money(receipt.bill().roomRate()),
                "Room cost: " + money(receipt.bill().roomCost()),
                "Discount: " + money(receipt.bill().discountAmount()),
                "Tax: " + money(receipt.bill().taxAmount()),
                "Total: " + money(receipt.bill().total()),
                "PAYMENT DETAILS",
                "Amount submitted: " + money(receipt.amountPaid()),
                "Remaining balance: " + money(receipt.remainingBalance()),
                "Payment method: " + receipt.paymentMethod(),
                "Payment status: " + receipt.paymentStatus().getDatabaseValue(),
                "Receipt note: " + (receipt.paymentStatus()
                        == com.hotel.core.model.enums.CustomerPaymentStatus.FULLY_PAID
                        ? "Keep this receipt for your records."
                        : "Payment submitted; subject to hotel verification."));
    }

    private int addSection(String title, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(19, 0, 5, 0);
        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(MUTED);
        receiptRows.add(label, constraints);
        return row + 1;
    }

    private int addReceiptRow(String labelText, String valueText, int row, boolean emphasized) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.weightx = 1;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.insets = new Insets(7, 0, 7, 12);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", emphasized ? Font.BOLD : Font.PLAIN, 13));
        label.setForeground(emphasized ? INK : MUTED);
        receiptRows.add(label, labelConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1;
        valueConstraints.anchor = GridBagConstraints.EAST;
        valueConstraints.insets = new Insets(7, 12, 7, 0);
        JLabel value = new JLabel(valueText, SwingConstants.RIGHT);
        value.setFont(new Font("Segoe UI", emphasized ? Font.BOLD : Font.PLAIN, emphasized ? 15 : 13));
        value.setForeground(INK);
        receiptRows.add(value, valueConstraints);

        if (emphasized) {
            GridBagConstraints line = new GridBagConstraints();
            line.gridx = 0;
            line.gridy = row;
            line.gridwidth = 2;
            line.fill = GridBagConstraints.HORIZONTAL;
            line.anchor = GridBagConstraints.SOUTH;
            line.insets = new Insets(0, 0, 0, 0);
            JPanel divider = new JPanel();
            divider.setBackground(LINE);
            divider.setPreferredSize(new Dimension(1, 1));
            receiptRows.add(divider, line);
        }
        return row + 1;
    }

    private static String money(java.math.BigDecimal amount) {
        return "PHP " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
