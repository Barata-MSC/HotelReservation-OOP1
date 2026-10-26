package com.hotel.desktop.reservation;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Guest;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.WalkInReceipt;
import com.hotel.core.service.ReservationService;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.desktop.customer.ReceiptPdfExporter;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JOptionPane;
import java.awt.print.PrinterException;
import java.math.BigDecimal;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class WalkInReservationScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ReservationService reservationService;
    private final Supplier<AuthenticatedUser> currentAdmin;
    private final Runnable onReservationCreated;
    private final JTextField nameField = StaffUi.textField(22);
    private final JTextField contactField = StaffUi.textField(16);
    private final JTextField emailField = StaffUi.textField(22);
    private final JComboBox<String> classificationBox =
            new JComboBox<>(new String[]{"Regular", "Discounted"});
    private final JTextField idTypeField = StaffUi.textField(16);
    private final JTextField idNumberField = StaffUi.textField(16);
    private final JCheckBox idVerified = new JCheckBox("Discount ID checked and verified");
    private final JTextField checkInField = StaffUi.textField(12);
    private final JTextField checkInTimeField = StaffUi.textField(8);
    private final JTextField checkOutField = StaffUi.textField(12);
    private final JTextField checkOutTimeField = StaffUi.textField(8);
    private final JTextField guestCountField = StaffUi.textField(4);
    private final JComboBox<Room> roomBox = new JComboBox<>();
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final JLabel rateLabel = new JLabel("Search dates to list available rooms.");
    private final JButton downloadReceipt = StaffUi.secondaryButton("Download receipt PDF");
    private final JButton printReceipt = StaffUi.secondaryButton("Print receipt");
    private final JTextField cashTenderedField = StaffUi.textField(10);
    private final JLabel changeLabel = new JLabel("Change: —");
    private Reservation lastCreatedReservation;
    private WalkInReceipt lastWalkInReceipt;
    private LocalDateTime lastCreatedCheckIn;
    private LocalDateTime lastCreatedCheckOut;
    private final Timer availabilityRefresh = new Timer(450, event -> reloadAvailableRooms());

    public WalkInReservationScreen(
            ReservationService reservationService,
            Supplier<AuthenticatedUser> currentAdmin,
            Runnable onReservationCreated) {
        this.reservationService = java.util.Objects.requireNonNull(reservationService, "reservationService");
        this.currentAdmin = java.util.Objects.requireNonNull(currentAdmin, "currentAdmin");
        this.onReservationCreated = java.util.Objects.requireNonNull(onReservationCreated, "onReservationCreated");
        JPanel body = StaffUi.createPage(this, "Walk-in reservation",
                "Register an arriving guest and reserve a clean, available room");
        body.add(buildForm(), BorderLayout.CENTER);
        checkInField.setText(LocalDate.now().toString());
        checkInTimeField.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)));
        checkOutField.setText(LocalDate.now().plusDays(1).toString());
        checkOutTimeField.setText("11:00 AM");
        guestCountField.setText("1");
        availabilityRefresh.setRepeats(false);
        addAvailabilityListener(checkInField);
        addAvailabilityListener(checkInTimeField);
        addAvailabilityListener(checkOutField);
        addAvailabilityListener(checkOutTimeField);
        addAvailabilityListener(guestCountField);
        cashTenderedField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { updateCashChange(); }
            @Override public void removeUpdate(DocumentEvent event) { updateCashChange(); }
            @Override public void changedUpdate(DocumentEvent event) { updateCashChange(); }
        });
        classificationBox.addActionListener(event -> {
            updateDiscountFields();
            updateSelectedRate();
            updatePaymentSummary();
        });
        idVerified.addActionListener(event -> updatePaymentSummary());
        roomBox.addActionListener(event -> {
            updateSelectedRate();
            updatePaymentSummary();
        });
        roomBox.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                String text = value instanceof Room room
                        ? "Room " + room.getRoomNumber() + " · " + room.getRoomType().getDatabaseValue()
                                + " · " + room.getCapacity() + " guest(s) · PHP "
                                + room.getPricePerDay().setScale(2).toPlainString() + "/night"
                        : "No room selected";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        idTypeField.setText("Senior Citizen / PWD");
        updateDiscountFields();
        reloadAvailableRooms();
    }

    private JPanel buildForm() {
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        JPanel formCard = StaffUi.card();
        formCard.setLayout(new BorderLayout(0, 12));
        JLabel title = new JLabel("Guest and stay details");
        title.setFont(new java.awt.Font("Georgia", java.awt.Font.BOLD, 21));
        title.setForeground(StaffUi.INK);
        formCard.add(title, BorderLayout.NORTH);

        JPanel guestFields = new JPanel(new GridBagLayout());
        guestFields.setOpaque(false);
        int guestRow = 0;
        guestRow = addField(guestFields, guestRow, "Guest full name", nameField);
        guestRow = addField(guestFields, guestRow, "Contact number", contactField);
        guestRow = addField(guestFields, guestRow, "Email address", emailField);
        guestRow = addField(guestFields, guestRow, "Guest classification", classificationBox);
        guestRow = addField(guestFields, guestRow, "Discount ID type", idTypeField);
        guestRow = addField(guestFields, guestRow, "Discount ID number", idNumberField);
        addField(guestFields, guestRow, "", idVerified);

        JPanel stayFields = new JPanel(new GridBagLayout());
        stayFields.setOpaque(false);
        checkInField.setToolTipText("Check-in date (yyyy-MM-dd)");
        checkInTimeField.setToolTipText("Check-in time (hh:mm AM/PM)");
        checkOutField.setToolTipText("Check-out date (yyyy-MM-dd)");
        checkOutTimeField.setToolTipText("Check-out time (hh:mm AM/PM)");
        int stayRow = 0;
        stayRow = addField(stayFields, stayRow, "Check-in date / time", dateTimeFields(checkInField, checkInTimeField));
        stayRow = addField(stayFields, stayRow, "Check-out date / time", dateTimeFields(checkOutField, checkOutTimeField));
        stayRow = addField(stayFields, stayRow, "Number of guests", guestCountField);
        JPanel roomSelection = new JPanel(new BorderLayout(8, 0));
        roomSelection.setOpaque(false);
        roomSelection.add(roomBox, BorderLayout.CENTER);
        javax.swing.JButton findRooms = StaffUi.secondaryButton("Find rooms");
        findRooms.addActionListener(event -> reloadAvailableRooms());
        roomSelection.add(findRooms, BorderLayout.EAST);
        stayRow = addField(stayFields, stayRow, "Available rooms", roomSelection);
        rateLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        rateLabel.setForeground(StaffUi.MUTED);
        stayRow = addField(stayFields, stayRow, "Stay total / payment", rateLabel);
        JPanel paymentEntry = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        paymentEntry.setOpaque(false);
        cashTenderedField.setToolTipText("Cash received from the guest");
        changeLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        changeLabel.setForeground(StaffUi.MUTED);
        paymentEntry.add(cashTenderedField);
        paymentEntry.add(changeLabel);
        addField(stayFields, stayRow, "Cash tendered / change", paymentEntry);
        JPanel columns = new JPanel(new GridLayout(1, 2, 18, 0));
        columns.setOpaque(false);
        columns.add(fieldSection("Guest details", guestFields));
        columns.add(fieldSection("Stay and payment", stayFields));
        formCard.add(columns, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(feedback, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        javax.swing.JButton create = StaffUi.primaryButton("Create walk-in reservation");
        create.addActionListener(event -> createReservation());
        downloadReceipt.setEnabled(false);
        printReceipt.setEnabled(false);
        downloadReceipt.addActionListener(event -> downloadLastReceipt());
        printReceipt.addActionListener(event -> printLastReceipt());
        buttons.add(printReceipt);
        buttons.add(downloadReceipt);
        buttons.add(create);
        actions.add(buttons, BorderLayout.EAST);
        formCard.add(actions, BorderLayout.SOUTH);
        content.add(formCard, BorderLayout.CENTER);
        return content;
    }

    private static JPanel fieldSection(String title, JPanel fields) {
        JPanel section = new JPanel(new BorderLayout(0, 6));
        section.setBackground(java.awt.Color.WHITE);
        section.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(StaffUi.LINE),
                javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        JLabel heading = new JLabel(title);
        heading.setFont(StaffUi.headingFont(17));
        heading.setForeground(StaffUi.INK);
        section.add(heading, BorderLayout.NORTH);
        section.add(fields, BorderLayout.CENTER);
        return section;
    }

    private int addField(JPanel panel, int row, String label, Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(5, 0, 5, 18);
        panel.add(StaffUi.fieldLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(5, 0, 5, 0);
        panel.add(field, fieldConstraints);
        if (field instanceof JComboBox<?> comboBox) {
            comboBox.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 14));
        }
        return row + 1;
    }

    private static JPanel dateTimeFields(JTextField dateField, JTextField timeField) {
        JPanel pair = new JPanel(new java.awt.GridLayout(1, 2, 8, 0));
        pair.setOpaque(false);
        pair.add(dateField);
        pair.add(timeField);
        return pair;
    }

    private void reloadAvailableRooms() {
        try {
            LocalDateTime checkIn = parseDateTime(checkInField, checkInTimeField);
            LocalDateTime checkOut = parseDateTime(checkOutField, checkOutTimeField);
            int guests = Integer.parseInt(guestCountField.getText().trim());
            List<Room> available = reservationService.findAvailableRooms(checkIn, checkOut, guests);
            roomBox.removeAllItems();
            for (Room room : available) {
                roomBox.addItem(room);
            }
            StaffUi.showFeedback(feedback,
                    available.size() + (available.size() == 1 ? " available room found." : " available rooms found."),
                    false);
            if (available.isEmpty()) {
                StaffUi.showFeedback(feedback, "No clean room is available for those dates and guest count.", true);
            }
            updateSelectedRate();
            updatePaymentSummary();
        } catch (DateTimeParseException | NumberFormatException exception) {
            roomBox.removeAllItems();
            rateLabel.setText("—");
            StaffUi.showFeedback(feedback, "Enter valid ISO dates and a whole guest count.", true);
        } catch (ValidationException exception) {
            roomBox.removeAllItems();
            rateLabel.setText("—");
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        } catch (SQLException exception) {
            roomBox.removeAllItems();
            rateLabel.setText("—");
            StaffUi.showFeedback(feedback, "Unable to load room availability: " + exception.getMessage(), true);
        }
    }

    public void refreshAvailableRooms() {
        reloadAvailableRooms();
    }

    private void createReservation() {
        try {
            GuestClassification classification = "Discounted".equals(classificationBox.getSelectedItem())
                    ? GuestClassification.DISCOUNTED : GuestClassification.REGULAR;
            Guest guest = new Guest(
                    0,
                    nameField.getText().trim(),
                    contactField.getText().trim(),
                    emailField.getText().trim(),
                    classification,
                    classification == GuestClassification.DISCOUNTED ? idTypeField.getText().trim() : null,
                    classification == GuestClassification.DISCOUNTED ? idNumberField.getText().trim() : null,
                    classification == GuestClassification.DISCOUNTED && idVerified.isSelected());
            Room room = (Room) roomBox.getSelectedItem();
            if (room == null) {
                throw new ValidationException("Find and select an available room first.");
            }
            int guestCount = Integer.parseInt(guestCountField.getText().trim());
            lastCreatedCheckIn = parseDateTime(checkInField, checkInTimeField);
            lastCreatedCheckOut = parseDateTime(checkOutField, checkOutTimeField);
            BigDecimal cashTendered = parseMoney(cashTenderedField.getText());
            if (cashTendered == null) {
                throw new ValidationException("Enter the cash received from the walk-in guest.");
            }
            CheckoutBill quote = selectedBill(guest, room, lastCreatedCheckIn, lastCreatedCheckOut);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Confirm this walk-in reservation and payment?\n"
                            + "Guest: " + guest.fullName() + "\nRoom: " + room.getRoomNumber()
                            + "\nTotal: PHP " + money(quote.total())
                            + "\nCash received: PHP " + money(cashTendered)
                            + "\nChange: PHP " + money(cashTendered.subtract(quote.total())),
                    "Confirm walk-in payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            lastWalkInReceipt = reservationService.createWalkInPaid(currentAdmin.get(), guest,
                    room.getRoomNumber(), lastCreatedCheckIn, lastCreatedCheckOut, guestCount, cashTendered);
            Reservation reservation = lastWalkInReceipt.reservation();
            lastCreatedReservation = reservation;
            downloadReceipt.setEnabled(true);
            printReceipt.setEnabled(true);
            onReservationCreated.run();
            reloadAvailableRooms();
            StaffUi.showFeedback(feedback,
                    "Reservation " + reservation.reference() + " confirmed. Room "
                            + reservation.roomNumber() + " is ready for check-in.",
                    false);
        } catch (NumberFormatException | DateTimeParseException exception) {
            StaffUi.showFeedback(feedback, "Enter valid dates and whole numbers for guest count.", true);
        } catch (ValidationException exception) {
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        } catch (SQLException exception) {
            StaffUi.showFeedback(feedback,
                    "Unable to save the reservation: " + exception.getMessage(), true);
        }
    }

    private static LocalDateTime parseDateTime(JTextField dateField, JTextField timeField) {
        return LocalDateTime.of(LocalDate.parse(dateField.getText().trim()),
                LocalTime.parse(timeField.getText().trim(),
                        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)));
    }

    private void addAvailabilityListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { availabilityRefresh.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { availabilityRefresh.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { availabilityRefresh.restart(); }
        });
    }

    private void updateDiscountFields() {
        boolean discounted = "Discounted".equals(classificationBox.getSelectedItem());
        idTypeField.setEnabled(discounted);
        idNumberField.setEnabled(discounted);
        idVerified.setEnabled(discounted);
        if (!discounted) {
            idVerified.setSelected(false);
        }
    }

    private void updateSelectedRate() {
        Room room = (Room) roomBox.getSelectedItem();
        rateLabel.setText(room == null ? "—"
                : "PHP " + room.getPricePerDay().setScale(2).toPlainString() + " per night");
    }

    private void updatePaymentSummary() {
        Room room = (Room) roomBox.getSelectedItem();
        if (room == null) {
            changeLabel.setText("Change: —");
            return;
        }
        try {
            LocalDateTime checkIn = parseDateTime(checkInField, checkInTimeField);
            LocalDateTime checkOut = parseDateTime(checkOutField, checkOutTimeField);
            CheckoutBill bill = billFor(quoteGuest(), room, checkIn, checkOut);
            rateLabel.setText("<html>PHP " + money(room.getPricePerDay()) + " / night"
                    + " &nbsp;·&nbsp; Room: " + money(bill.roomCost())
                    + " &nbsp;·&nbsp; Discount: -" + money(bill.discountAmount())
                    + " &nbsp;·&nbsp; Tax: " + money(bill.taxAmount())
                    + " &nbsp;·&nbsp; <b>Total: PHP " + money(bill.total()) + "</b></html>");
            BigDecimal tendered = parseMoney(cashTenderedField.getText());
            if (tendered == null) {
                changeLabel.setText("Due: PHP " + money(bill.total()));
            } else {
                BigDecimal difference = tendered.subtract(bill.total());
                changeLabel.setText(difference.signum() >= 0
                        ? "Change: PHP " + money(difference)
                        : "Short by: PHP " + money(difference.abs()));
            }
        } catch (RuntimeException exception) {
            changeLabel.setText("Enter valid stay dates and times.");
        }
    }

    private void updateCashChange() {
        updatePaymentSummary();
    }

    private Guest quoteGuest() {
        return new Guest(0, "Quote", "", "",
                "Discounted".equals(classificationBox.getSelectedItem())
                        ? GuestClassification.DISCOUNTED : GuestClassification.REGULAR,
                null, null, idVerified.isSelected());
    }

    private CheckoutBill selectedBill(Guest guest, Room room,
            LocalDateTime checkIn, LocalDateTime checkOut) {
        return billFor(guest, room, checkIn, checkOut);
    }

    private CheckoutBill billFor(Guest guest, Room room,
            LocalDateTime checkIn, LocalDateTime checkOut) {
        int nights = Math.max(1, Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(
                checkIn.toLocalDate(), checkOut.toLocalDate())));
        Reservation quote = new Reservation(0, "WALK-IN-QUOTE", guest, room.getRoomNumber(),
                room.getRoomType().getDatabaseValue(), room.getPricePerDay(), checkIn.toLocalDate(),
                checkOut.toLocalDate(), nights, 1, com.hotel.core.model.enums.ReservationType.WALK_IN,
                com.hotel.core.model.enums.ReservationStatus.CONFIRMED);
        return reservationService.calculateCheckoutBill(quote);
    }

    private static BigDecimal parseMoney(String value) {
        try {
            String normalized = value == null ? "" : value.replace(",", "").trim();
            if (normalized.isEmpty()) return null;
            BigDecimal amount = new BigDecimal(normalized).setScale(2, java.math.RoundingMode.UNNECESSARY);
            return amount.signum() > 0 ? amount : null;
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private void downloadLastReceipt() {
        if (lastCreatedReservation == null || lastWalkInReceipt == null) {
            StaffUi.showFeedback(feedback, "Create a reservation before downloading its receipt.", true);
            return;
        }
        CheckoutBill bill = lastWalkInReceipt.bill();
        List<String> lines = List.of(
                "Reservation reference: " + lastCreatedReservation.reference(),
                "Guest: " + lastCreatedReservation.guest().fullName(),
                "Classification: " + lastCreatedReservation.guest().classification().getDatabaseValue(),
                "Room: " + lastCreatedReservation.roomNumber() + " · " + lastCreatedReservation.roomType(),
                "Check-in: " + formatDateTime(lastCreatedCheckIn),
                "Check-out: " + formatDateTime(lastCreatedCheckOut),
                "Reservation status: " + lastCreatedReservation.status().getDatabaseValue(),
                "Nights: " + bill.nights(),
                "Rate per night: " + bill.roomRate().setScale(2).toPlainString(),
                "Room cost: " + bill.roomCost().setScale(2).toPlainString(),
                "Discount: " + bill.discountAmount().setScale(2).toPlainString(),
                "Tax: " + bill.taxAmount().setScale(2).toPlainString(),
                "Total: " + bill.total().setScale(2).toPlainString(),
                "Amount paid: " + money(bill.total()),
                "Cash tendered: " + money(lastWalkInReceipt.cashTendered()),
                "Change: " + money(lastWalkInReceipt.change()),
                "Payment status: Paid",
                "Receipt: " + lastWalkInReceipt.receiptReference());
        ReceiptPdfExporter.chooseAndExport(
                this, "walk-in-" + lastCreatedReservation.reference(),
                "Walk-in Reservation Receipt", lines);
    }

    private void printLastReceipt() {
        if (lastWalkInReceipt == null) return;
        CheckoutBill bill = lastWalkInReceipt.bill();
        String receipt = "                 RECEIPT\n"
                + "        Hotel Reservation · Front desk\n"
                + "------------------------------------------\n"
                + "Receipt:         " + lastWalkInReceipt.receiptReference() + "\n"
                + "Reservation:     " + lastCreatedReservation.reference() + "\n"
                + "Guest:           " + lastCreatedReservation.guest().fullName() + "\n"
                + "Room:            " + lastCreatedReservation.roomNumber() + " · "
                        + lastCreatedReservation.roomType() + "\n"
                + "Check-in:        " + formatDateTime(lastCreatedCheckIn) + "\n"
                + "Check-out:       " + formatDateTime(lastCreatedCheckOut) + "\n"
                + "Nights:          " + bill.nights() + "\n"
                + "Room cost:       PHP " + money(bill.roomCost()) + "\n"
                + "Discount:        -PHP " + money(bill.discountAmount()) + "\n"
                + "Tax:             +PHP " + money(bill.taxAmount()) + "\n"
                + "TOTAL PAID:      PHP " + money(bill.total()) + "\n"
                + "Cash tendered:   PHP " + money(lastWalkInReceipt.cashTendered()) + "\n"
                + "Change:          PHP " + money(lastWalkInReceipt.change()) + "\n"
                + "Payment status:  PAID";
        JTextArea receiptArea = new JTextArea(receipt, 22, 44);
        receiptArea.setEditable(false);
        receiptArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 13));
        try {
            receiptArea.print();
        } catch (PrinterException exception) {
            JOptionPane.showMessageDialog(this, "Unable to print receipt: " + exception.getMessage(),
                    "Print receipt", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a", Locale.ENGLISH));
    }
}
