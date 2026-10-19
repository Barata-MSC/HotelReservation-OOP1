package com.hotel.desktop.checkOut;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.CheckoutReceipt;
import com.hotel.core.model.Reservation;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public final class CheckOutScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final ReservationService reservationService;
    private final Runnable onReservationChanged;
    private final List<Reservation> checkedIn = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[0][5],
            new String[]{"Reference", "Guest", "Room", "Departure", "Status"}) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel guestLabel = new JLabel("Select a checked-in guest.");
    private final JLabel billLabel = new JLabel("Amount due: —");
    private final JLabel changeLabel = new JLabel("Change: —");
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final JTextField cashField = StaffUi.textField(14);
    private Reservation selectedReservation;
    private CheckoutBill selectedBill;
    private CheckoutReceipt lastReceipt;
    private JButton checkoutButton;
    private JButton printButton;
    private JButton refreshButton;
    private boolean operationPending;

    public CheckOutScreen(ReservationService reservationService, Runnable onReservationChanged) {
        this.reservationService = java.util.Objects.requireNonNull(reservationService, "reservationService");
        this.onReservationChanged = java.util.Objects.requireNonNull(onReservationChanged, "onReservationChanged");
        JPanel body = StaffUi.createPage(this, "Check-out & Billing",
                "Review an in-house guest, collect cash, issue a receipt, and send the room to housekeeping");
        body.add(buildContent(), BorderLayout.CENTER);
        StaffUi.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showSelectedReservation();
            }
        });
        cashField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                updateChange();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                updateChange();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                updateChange();
            }
        });
        refreshReservations();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        JPanel listCard = StaffUi.card();
        JPanel listHeading = new JPanel(new BorderLayout());
        listHeading.setOpaque(false);
        JLabel title = new JLabel("Guests currently checked in");
        title.setFont(new java.awt.Font("Georgia", java.awt.Font.BOLD, 21));
        title.setForeground(StaffUi.INK);
        listHeading.add(title, BorderLayout.WEST);
        refreshButton = StaffUi.secondaryButton("Refresh");
        refreshButton.addActionListener(event -> refreshReservations());
        listHeading.add(refreshButton, BorderLayout.EAST);
        listCard.add(listHeading, BorderLayout.NORTH);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, StaffUi.INK));
        tableScroll.getViewport().setBackground(java.awt.Color.WHITE);
        listCard.add(tableScroll, BorderLayout.CENTER);
        content.add(listCard, BorderLayout.CENTER);

        JPanel checkoutCard = StaffUi.card();
        checkoutCard.setLayout(new BorderLayout(0, 12));
        JPanel paymentFields = new JPanel(new GridBagLayout());
        paymentFields.setOpaque(false);
        guestLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        guestLabel.setForeground(StaffUi.INK);
        billLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        billLabel.setForeground(StaffUi.INK);
        changeLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        changeLabel.setForeground(StaffUi.MUTED);
        addField(paymentFields, 0, StaffUi.fieldLabel("Amount due"), billLabel);
        addField(paymentFields, 1, StaffUi.fieldLabel("Cash tendered"), cashField);
        addField(paymentFields, 2, StaffUi.fieldLabel("Change"), changeLabel);
        JPanel paymentPanel = new JPanel(new BorderLayout(0, 8));
        paymentPanel.setOpaque(false);
        paymentPanel.add(guestLabel, BorderLayout.NORTH);
        paymentPanel.add(paymentFields, BorderLayout.CENTER);
        checkoutCard.add(paymentPanel, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        printButton = StaffUi.secondaryButton("Print last receipt");
        printButton.setEnabled(false);
        printButton.addActionListener(event -> printReceipt());
        checkoutButton = StaffUi.primaryButton("Complete check-out");
        checkoutButton.addActionListener(event -> completeCheckout());
        actions.add(printButton);
        actions.add(checkoutButton);
        checkoutCard.add(actions, BorderLayout.SOUTH);
        content.add(checkoutCard, BorderLayout.SOUTH);

        JPanel page = new JPanel(new BorderLayout(0, 8));
        page.setOpaque(false);
        page.add(content, BorderLayout.CENTER);
        page.add(feedback, BorderLayout.SOUTH);
        return page;
    }

    private static void addField(JPanel panel, int row, java.awt.Component label, java.awt.Component field) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(5, 0, 5, 16);
        panel.add(label, left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.insets = new Insets(5, 0, 5, 0);
        panel.add(field, right);
    }

    public void refreshReservations() {
        if (operationPending) {
            return;
        }
        setOperationPending(true);
        StaffUi.showFeedback(feedback, "Loading checked-in guests…", false);
        StaffUi.runInBackground(
                reservationService::findCheckedInReservations,
                loaded -> {
                    setOperationPending(false);
                    checkedIn.clear();
                    checkedIn.addAll(loaded);
                    model.setRowCount(0);
                    for (Reservation reservation : checkedIn) {
                        model.addRow(new Object[]{
                            reservation.reference(),
                            reservation.guest().fullName(),
                            reservation.roomNumber(),
                            DATE_FORMAT.format(reservation.checkOut()),
                            reservation.status().getDatabaseValue()
                        });
                    }
                    selectedReservation = null;
                    selectedBill = null;
                    guestLabel.setText("Select a checked-in guest.");
                    billLabel.setText("Amount due: —");
                    cashField.setText("");
                    StaffUi.showFeedback(feedback, checkedIn.size()
                            + (checkedIn.size() == 1
                                    ? " guest in house." : " guests in house."), false);
                    updateChange();
                },
                error -> {
                    setOperationPending(false);
                    checkedIn.clear();
                    model.setRowCount(0);
                    selectedReservation = null;
                    selectedBill = null;
                    guestLabel.setText("Select a checked-in guest.");
                    billLabel.setText("Amount due: —");
                    StaffUi.showFeedback(feedback,
                            "Unable to load checked-in guests: " + error.getMessage(), true);
                });
    }

    private void setOperationPending(boolean pending) {
        operationPending = pending;
        checkoutButton.setEnabled(!pending);
        refreshButton.setEnabled(!pending);
        cashField.setEnabled(!pending);
        table.setEnabled(!pending);
        printButton.setEnabled(!pending && lastReceipt != null);
    }

    private void showSelectedReservation() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= checkedIn.size()) {
            return;
        }
        selectedReservation = checkedIn.get(table.convertRowIndexToModel(selected));
        try {
            selectedBill = reservationService.calculateCheckoutBill(selectedReservation);
            guestLabel.setText(selectedReservation.guest().fullName()
                    + " · Room " + selectedReservation.roomNumber()
                    + " · " + selectedBill.nights() + " night(s)");
            billLabel.setText("Amount due: " + money(selectedBill.total()));
            updateChange();
            StaffUi.showFeedback(feedback,
                    LocalDate.now().isBefore(selectedReservation.checkOut())
                            ? "Checkout is available from " + DATE_FORMAT.format(selectedReservation.checkOut()) + "."
                            : "Enter the cash received to complete checkout.",
                    false);
        } catch (ValidationException exception) {
            selectedBill = null;
            billLabel.setText("Amount due: —");
            changeLabel.setText("Change: —");
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        }
    }

    private void updateChange() {
        BigDecimal tendered = parseCash(cashField.getText());
        if (tendered == null || selectedBill == null) {
            changeLabel.setText("Change: —");
            return;
        }
        BigDecimal difference = tendered.subtract(selectedBill.total());
        changeLabel.setText(difference.signum() >= 0
                ? "Change: " + money(difference) : "Short by: " + money(difference.abs()));
    }

    private void completeCheckout() {
        if (operationPending) {
            return;
        }
        if (selectedReservation == null || selectedBill == null) {
            StaffUi.showFeedback(feedback, "Select a checked-in guest first.", true);
            return;
        }
        BigDecimal cash = parseCash(cashField.getText());
        if (cash == null) {
            StaffUi.showFeedback(feedback, "Enter a valid positive cash amount.", true);
            return;
        }
        String reference = selectedReservation.reference();
        setOperationPending(true);
        StaffUi.showFeedback(feedback, "Calculating payment and saving checkout…", false);
        StaffUi.runInBackground(
                () -> reservationService.checkOut(reference, cash, LocalDate.now()),
                receipt -> {
                    lastReceipt = receipt;
                    setOperationPending(false);
                    printButton.setEnabled(true);
                    onReservationChanged.run();
                    StaffUi.showFeedback(feedback,
                            "Checkout complete. Receipt " + receipt.receiptReference()
                                    + " issued; room " + receipt.reservation().roomNumber()
                                    + " is now Dirty for housekeeping.",
                            false);
                    refreshReservations();
                },
                error -> {
                    setOperationPending(false);
                    StaffUi.showFeedback(feedback,
                            error instanceof ValidationException
                                    ? error.getMessage()
                                    : "Unable to complete checkout: " + error.getMessage(),
                            true);
                });
    }

    private void printReceipt() {
        if (lastReceipt == null) {
            StaffUi.showFeedback(feedback, "There is no completed checkout receipt to print.", true);
            return;
        }
        JTextArea receipt = new JTextArea(receiptText(lastReceipt), 20, 42);
        receipt.setEditable(false);
        receipt.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
        try {
            receipt.print();
        } catch (PrinterException exception) {
            JOptionPane.showMessageDialog(this,
                    "Unable to print receipt: " + exception.getMessage(),
                    "Print receipt", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String receiptText(CheckoutReceipt receipt) {
        Reservation reservation = receipt.reservation();
        CheckoutBill bill = receipt.bill();
        return "                 RECEIPT\n"
                + "        Hotel Reservation · Front desk\n"
                + "------------------------------------------\n"
                + "Receipt:         " + receipt.receiptReference() + "\n"
                + "Reservation:     " + reservation.reference() + "\n"
                + "Guest:           " + reservation.guest().fullName() + "\n"
                + "Classification:  " + reservation.guest().classification().getDatabaseValue() + "\n"
                + "Room:            " + reservation.roomNumber() + " · " + reservation.roomType() + "\n"
                + "Check-in:        " + DATE_FORMAT.format(reservation.checkIn()) + "\n"
                + "Check-out:       " + DATE_FORMAT.format(reservation.checkOut()) + "\n"
                + "Nights:          " + bill.nights() + "\n"
                + "Rate per night:  " + money(bill.roomRate()) + "\n"
                + "Room cost:       " + money(bill.roomCost()) + "\n"
                + "Discount:       -" + money(bill.discountAmount()) + "\n"
                + "Tax:            +" + money(bill.taxAmount()) + "\n"
                + "TOTAL:           " + money(bill.total()) + "\n"
                + "Cash tendered:   " + money(receipt.cashTendered()) + "\n"
                + "Change:          " + money(receipt.change()) + "\n"
                + "Payment status:  PAID\n"
                + "Room status:     DIRTY";
    }

    private static BigDecimal parseCash(String value) {
        try {
            String normalized = value == null ? "" : value.replace(",", "").trim();
            if (normalized.isEmpty()) {
                return null;
            }
            BigDecimal amount = new BigDecimal(normalized);
            if (amount.signum() <= 0) {
                return null;
            }
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private static String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
