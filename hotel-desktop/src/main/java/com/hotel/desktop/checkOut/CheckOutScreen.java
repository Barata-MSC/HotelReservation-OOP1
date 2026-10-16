package com.hotel.desktop.checkOut;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/** Checkout and billing UI preview hosted in the staff workspace. */
public class CheckOutScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color INK = new Color(20, 18, 26);
    private static final Color SUN = new Color(255, 204, 0);
    private static final Color CANVAS = new Color(255, 222, 62);
    private static final Color MUTED = new Color(105, 91, 33);
    private static final Color LINE = new Color(236, 230, 196);
    private static final Color SELECTED = new Color(255, 243, 184);
    private static final Color GREEN = new Color(0, 230, 80);
    private static final Color ERROR = new Color(179, 38, 30);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final NumberFormat AMOUNT_FORMAT =
            NumberFormat.getNumberInstance(Locale.US);

    private final List<Reservation> reservations = createSampleReservations();
    private final JPanel stepsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    private final JPanel stagePanel = new JPanel(new BorderLayout());
    private int step = 1;
    private Reservation selectedReservation;
    private Bill bill;
    private Payment payment;
    private boolean roomClean;
    private JTextField paymentField;
    private JLabel changeLabel;
    private JLabel paymentMessage;
    private JLabel searchFeedback;

    public CheckOutScreen() {
        setLayout(new BorderLayout());
        setBackground(CANVAS);
        setPreferredSize(new Dimension(1024, 800));
        add(buildContent(), BorderLayout.CENTER);
        showStep();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CANVAS);

        JPanel strip = new JPanel();
        strip.setBackground(SUN);
        strip.setPreferredSize(new Dimension(10, 15));
        content.add(strip, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 38, 36, 38));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Check-out & Billing");
        title.setFont(new Font("Georgia", Font.BOLD, 36));
        title.setForeground(INK);
        JLabel date = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))
                + "  ·  UI preview — sample data is not saved");
        date.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        date.setForeground(MUTED);
        heading.add(title);
        heading.add(Box.createVerticalStrut(3));
        heading.add(date);
        heading.add(Box.createVerticalStrut(18));

        stepsPanel.setOpaque(false);
        heading.add(stepsPanel);
        body.add(heading, BorderLayout.NORTH);

        stagePanel.setOpaque(false);
        body.add(stagePanel, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        content.add(scroll, BorderLayout.CENTER);
        return content;
    }

    private void showStep() {
        updateSteps();
        stagePanel.removeAll();
        if (step == 1) {
            stagePanel.add(buildFindStage(), BorderLayout.CENTER);
        } else if (step == 2) {
            stagePanel.add(buildReviewStage(), BorderLayout.CENTER);
        } else if (step == 3) {
            stagePanel.add(buildPaymentStage(), BorderLayout.CENTER);
        } else {
            stagePanel.add(buildReceiptStage(), BorderLayout.CENTER);
        }
        stagePanel.revalidate();
        stagePanel.repaint();
    }

    private void updateSteps() {
        stepsPanel.removeAll();
        String[] labels = {"Find reservation", "Review bill", "Cash payment", "Receipt & housekeeping"};
        for (int i = 0; i < labels.length; i++) {
            int stage = i + 1;
            boolean done = stage < step;
            boolean current = stage == step;
            JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
            chip.setBackground(done ? INK : (current ? SUN : Color.WHITE));
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(INK, 2, true),
                    BorderFactory.createEmptyBorder(0, 8, 0, 12)));
            JLabel number = new JLabel(done ? "✓" : Integer.toString(stage), SwingConstants.CENTER);
            number.setPreferredSize(new Dimension(24, 24));
            number.setOpaque(true);
            number.setBackground(done || current ? INK : Color.WHITE);
            number.setForeground(done || current ? SUN : INK);
            number.setFont(new Font("Segoe UI", Font.BOLD, 12));
            number.setBorder(BorderFactory.createLineBorder(done ? SUN : INK, 1, true));
            JLabel text = new JLabel(labels[i]);
            text.setFont(new Font("Segoe UI", Font.BOLD, 12));
            text.setForeground(done ? SUN : INK);
            chip.add(number);
            chip.add(text);
            stepsPanel.add(chip);
        }
        stepsPanel.revalidate();
        stepsPanel.repaint();
    }

    private JPanel buildFindStage() {
        JPanel grid = twoColumns();
        JPanel findCard = card();
        findCard.setLayout(new BoxLayout(findCard, BoxLayout.Y_AXIS));
        addCardTitle(findCard, "Find reservation", null);
        JLabel label = new JLabel("Reference or guest name");
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(INK);
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
        findCard.add(label);

        JPanel search = new JPanel(new BorderLayout(10, 0));
        search.setOpaque(false);
        JTextField query = styledField();
        query.setToolTipText("Search by reservation reference or guest name");
        query.addActionListener(event -> findReservation(query.getText()));
        search.add(query, BorderLayout.CENTER);
        JButton searchButton = primaryButton("Search");
        searchButton.addActionListener(event -> findReservation(query.getText()));
        search.add(searchButton, BorderLayout.EAST);
        findCard.add(search);

        searchFeedback = new JLabel(" ");
        searchFeedback.setFont(new Font("Segoe UI", Font.BOLD, 13));
        searchFeedback.setForeground(ERROR);
        searchFeedback.setBorder(BorderFactory.createEmptyBorder(9, 0, 2, 0));
        findCard.add(searchFeedback);
        JLabel hint = new JLabel("<html>Only reservations with status <b>Checked-In</b> can be checked out. "
                + "Other statuses are blocked.</html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(MUTED);
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        findCard.add(hint);

        JPanel guestsCard = card();
        guestsCard.setLayout(new BorderLayout(0, 8));
        long inHouseCount = reservations.stream()
                .filter(reservation -> "Checked-In".equals(reservation.status)).count();
        addCardTitle(guestsCard, "Guests in house", inHouseCount + " checked in");
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Reference", "Guest", "Room", "Status"}, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Reservation reservation : reservations) {
            if ("Checked-In".equals(reservation.status)) {
                model.addRow(new Object[]{reservation.reference, reservation.name,
                    reservation.room, reservation.status});
            }
        }
        JTable table = new JTable(model);
        styleTable(table);
        table.setRowHeight(38);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    chooseReservation(reservations.get(table.convertRowIndexToModel(table.getSelectedRow())),
                            searchFeedback);
                }
            }
        });
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(LINE));
        guestsCard.add(tableScroll, BorderLayout.CENTER);

        JButton selectButton = secondaryButton("Select guest");
        selectButton.addActionListener(event -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow < 0) {
                searchFeedback.setForeground(ERROR);
                searchFeedback.setText("Select a reservation from the list first.");
                return;
            }
            chooseReservation(reservations.get(table.convertRowIndexToModel(selectedRow)), searchFeedback);
        });
        JPanel selectRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        selectRow.setOpaque(false);
        selectRow.add(selectButton);
        guestsCard.add(selectRow, BorderLayout.SOUTH);

        addColumn(grid, findCard, 0);
        addColumn(grid, guestsCard, 1);
        return grid;
    }

    private void findReservation(String value) {
        String query = value == null ? "" : value.trim();
        if (query.isEmpty()) {
            searchFeedback.setForeground(ERROR);
            searchFeedback.setText("Enter a reservation reference or guest name.");
            return;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        Reservation match = reservations.stream()
                .filter(reservation -> reservation.reference.equalsIgnoreCase(query))
                .findFirst()
                .orElseGet(() -> reservations.stream()
                        .filter(reservation -> reservation.name.toLowerCase(Locale.ROOT).contains(normalized))
                        .findFirst().orElse(null));
        if (match == null) {
            searchFeedback.setForeground(ERROR);
            searchFeedback.setText("Reservation not found.");
            return;
        }
        chooseReservation(match, searchFeedback);
    }

    private void chooseReservation(Reservation reservation, JLabel feedback) {
        if (!"Checked-In".equals(reservation.status)) {
            feedback.setForeground(ERROR);
            feedback.setText("Guest is not checked in (" + reservation.reference + " is "
                    + reservation.status + "). Checkout not allowed.");
            return;
        }
        selectedReservation = reservation;
        bill = calculateBill(reservation);
        payment = null;
        roomClean = false;
        step = 2;
        showStep();
    }

    private JPanel buildReviewStage() {
        JPanel grid = twoColumns();
        JPanel stayCard = card();
        stayCard.setLayout(new BoxLayout(stayCard, BoxLayout.Y_AXIS));
        addCardTitle(stayCard, "Reservation & stay", selectedReservation.reference);
        addKeyValue(stayCard, "Guest", selectedReservation.name);
        addKeyValue(stayCard, "Classification", selectedReservation.classification);
        if (selectedReservation.discountId != null) {
            addKeyValue(stayCard, "Discount ID", selectedReservation.discountId);
        }
        addKeyValue(stayCard, "Contact", selectedReservation.contact);
        addKeyValue(stayCard, "Room", selectedReservation.room + " · " + selectedReservation.roomType);
        addKeyValue(stayCard, "Reservation type", selectedReservation.reservationType);
        addKeyValue(stayCard, "Check-in", formatDate(selectedReservation.checkIn));
        addKeyValue(stayCard, "Check-out", formatDate(selectedReservation.checkOut));
        addKeyValue(stayCard, "Status", selectedReservation.status);

        JPanel billCard = buildBillCard("Final bill", "Room charges only");
        addColumn(grid, stayCard, 0);
        addColumn(grid, billCard, 1);

        JPanel actions = actionRow();
        JButton back = secondaryButton("Back");
        back.addActionListener(event -> {
            step = 1;
            showStep();
        });
        JButton proceed = primaryButton("Proceed to Payment");
        proceed.addActionListener(event -> {
            step = 3;
            showStep();
        });
        actions.add(back);
        actions.add(proceed);
        JPanel stage = new JPanel(new BorderLayout(0, 16));
        stage.setOpaque(false);
        stage.add(grid, BorderLayout.CENTER);
        stage.add(actions, BorderLayout.SOUTH);
        return stage;
    }

    private JPanel buildBillCard(String title, String subtitle) {
        JPanel billCard = card();
        billCard.setLayout(new BoxLayout(billCard, BoxLayout.Y_AXIS));
        addCardTitle(billCard, title, subtitle);
        addKeyValue(billCard, "Nights", Long.toString(bill.nights));
        addKeyValue(billCard, "Rate per night", money(selectedReservation.rate));
        addKeyValue(billCard, "Room cost", money(bill.roomCost));
        addKeyValue(billCard, "Discount (" + percent(bill.discountRate) + ")", "− " + money(bill.discount));
        addKeyValue(billCard, "Tax (" + percent(bill.taxRate) + ")", "+ " + money(bill.tax));
        addTotal(billCard, "Total due", money(bill.total));
        JLabel formula = new JLabel("<html>Room cost = rate × nights · Discount = room cost × discount rate"
                + "<br>Tax = (room cost − discount) × tax rate · Total = room cost − discount + tax</html>");
        formula.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formula.setForeground(MUTED);
        formula.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        billCard.add(formula);
        return billCard;
    }

    private JPanel buildPaymentStage() {
        JPanel grid = twoColumns();
        JPanel amountCard = buildBillCard("Amount due",
                selectedReservation.name + " · Room " + selectedReservation.room);
        JPanel paymentCard = card();
        paymentCard.setLayout(new BoxLayout(paymentCard, BoxLayout.Y_AXIS));
        addCardTitle(paymentCard, "Cash payment", "Cash only");
        JLabel label = new JLabel("Cash tendered (PHP)");
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(INK);
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 6, 0));
        paymentCard.add(label);
        paymentField = styledField();
        paymentField.setToolTipText("Enter a positive amount in Philippine pesos");
        paymentField.addActionListener(event -> confirmPayment());
        paymentCard.add(paymentField);

        JPanel quickAmounts = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        quickAmounts.setOpaque(false);
        addQuickAmount(quickAmounts, "Exact amount", bill.total);
        addQuickAmount(quickAmounts, "Nearest 500", roundUpTo(bill.total, 500));
        addQuickAmount(quickAmounts, "Nearest 1,000", roundUpTo(bill.total, 1000));
        paymentCard.add(quickAmounts);

        JPanel change = new JPanel(new BorderLayout());
        change.setOpaque(false);
        change.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(INK, 1, 4, 3, true),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JLabel changeTitle = new JLabel("Change");
        changeTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        changeLabel = new JLabel(money(BigDecimal.ZERO));
        changeLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        changeLabel.setForeground(INK);
        change.add(changeTitle, BorderLayout.WEST);
        change.add(changeLabel, BorderLayout.EAST);
        paymentCard.add(change);

        paymentMessage = new JLabel(" ");
        paymentMessage.setFont(new Font("Segoe UI", Font.BOLD, 13));
        paymentMessage.setForeground(ERROR);
        paymentMessage.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        paymentCard.add(paymentMessage);
        paymentField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
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

        JLabel note = new JLabel("<html>Change = cash tendered − total. Payment is rejected if the amount is "
                + "invalid or less than the amount due. This preview does not save payment data.</html>");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        note.setForeground(MUTED);
        note.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        paymentCard.add(note);
        addColumn(grid, amountCard, 0);
        addColumn(grid, paymentCard, 1);

        JPanel actions = actionRow();
        JButton cancel = secondaryButton("Cancel (not paid)");
        cancel.addActionListener(event -> {
            step = 2;
            showStep();
        });
        JButton confirm = primaryButton("Confirm payment");
        confirm.addActionListener(event -> confirmPayment());
        actions.add(cancel);
        actions.add(confirm);

        JPanel stage = new JPanel(new BorderLayout(0, 16));
        stage.setOpaque(false);
        stage.add(grid, BorderLayout.CENTER);
        stage.add(actions, BorderLayout.SOUTH);
        return stage;
    }

    private void addQuickAmount(JPanel parent, String label, BigDecimal amount) {
        JButton button = secondaryButton(label);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setMargin(new Insets(7, 11, 7, 11));
        button.addActionListener(event -> paymentField.setText(amount.setScale(2).toPlainString()));
        parent.add(button);
    }

    private void updateChange() {
        BigDecimal tendered = parseAmount(paymentField.getText());
        if (tendered != null && tendered.compareTo(bill.total) >= 0) {
            changeLabel.setText(money(tendered.subtract(bill.total)));
        } else {
            changeLabel.setText(money(BigDecimal.ZERO));
        }
        paymentMessage.setText(" ");
    }

    private void confirmPayment() {
        BigDecimal tendered = parseAmount(paymentField.getText());
        if (tendered == null || tendered.signum() <= 0) {
            paymentMessage.setText("Amount is invalid. Enter a positive number.");
            return;
        }
        if (tendered.compareTo(bill.total) < 0) {
            paymentMessage.setText("Cash tendered is less than the amount due (" + money(bill.total) + ").");
            return;
        }
        payment = new Payment(tendered, tendered.subtract(bill.total),
                "RCT-" + selectedReservation.reference.substring(4));
        selectedReservation.status = "Checked-Out";
        roomClean = false;
        step = 4;
        showStep();
    }

    private JPanel buildReceiptStage() {
        JPanel grid = twoColumns();
        JPanel receiptCard = card();
        receiptCard.setLayout(new BoxLayout(receiptCard, BoxLayout.Y_AXIS));
        addCardTitle(receiptCard, "Receipt", payment.receiptReference);
        JTextArea receiptText = new JTextArea(buildReceiptText());
        receiptText.setRows(20);
        receiptText.setColumns(42);
        receiptText.setEditable(false);
        receiptText.setFocusable(false);
        receiptText.setBackground(new Color(255, 253, 240));
        receiptText.setForeground(INK);
        receiptText.setFont(new Font("Monospaced", Font.PLAIN, 13));
        receiptText.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(INK, 1, 5, 3, true),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        receiptText.setAlignmentX(Component.LEFT_ALIGNMENT);
        receiptCard.add(receiptText);
        JButton print = secondaryButton("Print receipt");
        print.setAlignmentX(Component.LEFT_ALIGNMENT);
        print.addActionListener(event -> printReceipt(receiptText));
        JPanel printRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
        printRow.setOpaque(false);
        printRow.add(print);
        receiptCard.add(printRow);

        JPanel housekeepingCard = card();
        housekeepingCard.setLayout(new BoxLayout(housekeepingCard, BoxLayout.Y_AXIS));
        addCardTitle(housekeepingCard, "Room housekeeping", "Room " + selectedReservation.room);
        addKeyValue(housekeepingCard, "Reservation", "Checked-Out");
        addKeyValue(housekeepingCard, "Room occupancy", "Not occupied");
        addKeyValue(housekeepingCard, "Housekeeping", roomClean ? "Clean" : "Dirty");
        addKeyValue(housekeepingCard, "Sellable", roomClean ? "Yes" : "No — waiting for cleaning");
        if (roomClean) {
            JLabel cleaned = new JLabel("Room " + selectedReservation.room + " is Clean and can be sold again.");
            cleaned.setFont(new Font("Segoe UI", Font.BOLD, 13));
            cleaned.setOpaque(true);
            cleaned.setBackground(GREEN);
            cleaned.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            cleaned.setAlignmentX(Component.LEFT_ALIGNMENT);
            housekeepingCard.add(cleaned);
        }
        JLabel housekeepingNote = new JLabel("<html>After checkout, a room remains Dirty until housekeeping "
                + "is completed. This sample state is only held in memory.</html>");
        housekeepingNote.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        housekeepingNote.setForeground(MUTED);
        housekeepingNote.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        housekeepingCard.add(housekeepingNote);

        JPanel housekeepingActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        housekeepingActions.setOpaque(false);
        if (!roomClean) {
            JButton clean = primaryButton("Cleaning completed → mark Clean");
            clean.addActionListener(event -> {
                roomClean = true;
                showStep();
            });
            housekeepingActions.add(clean);
        }
        JButton done = secondaryButton(roomClean ? "Check out another guest" : "Done — check again later");
        done.addActionListener(event -> reset());
        housekeepingActions.add(done);
        housekeepingCard.add(housekeepingActions);

        addColumn(grid, receiptCard, 0);
        addColumn(grid, housekeepingCard, 1);
        return grid;
    }

    private String buildReceiptText() {
        StringBuilder text = new StringBuilder();
        text.append("                 RECEIPT\n")
                .append("        Hotel Reservation · Front desk\n")
                .append("------------------------------------------\n")
                .append("Reservation ref: ").append(selectedReservation.reference).append('\n')
                .append("Guest:           ").append(selectedReservation.name).append('\n')
                .append("Classification:  ").append(selectedReservation.classification).append('\n')
                .append("Room:            ").append(selectedReservation.room).append(" · ")
                .append(selectedReservation.roomType).append('\n')
                .append("Check-in:        ").append(formatDate(selectedReservation.checkIn)).append('\n')
                .append("Check-out:       ").append(formatDate(selectedReservation.checkOut)).append('\n')
                .append("------------------------------------------\n")
                .append("Nights:          ").append(bill.nights).append('\n')
                .append("Rate per night:  ").append(money(selectedReservation.rate)).append('\n')
                .append("Room cost:       ").append(money(bill.roomCost)).append('\n')
                .append("Discount:       -").append(money(bill.discount)).append('\n')
                .append("Tax:            +").append(money(bill.tax)).append('\n')
                .append("------------------------------------------\n")
                .append("TOTAL:           ").append(money(bill.total)).append('\n')
                .append("Cash tendered:   ").append(money(payment.tendered)).append('\n')
                .append("Change:          ").append(money(payment.change)).append('\n')
                .append("------------------------------------------\n")
                .append("Payment status: PAID\n")
                .append("   Thank you for staying with us.");
        return text.toString();
    }

    private void printReceipt(JTextArea receipt) {
        try {
            receipt.print();
        } catch (java.awt.print.PrinterException exception) {
            JOptionPane.showMessageDialog(this,
                    "Unable to print the receipt: " + exception.getMessage(),
                    "Print receipt", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void reset() {
        selectedReservation = null;
        bill = null;
        payment = null;
        roomClean = false;
        step = 1;
        showStep();
    }

    private static JPanel twoColumns() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        return grid;
    }

    private static void addColumn(JPanel grid, JPanel panel, int column) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.insets = new Insets(0, column == 0 ? 0 : 8, 0, column == 1 ? 0 : 8);
        grid.add(panel, constraints);
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(18, 22, 18, 22)));
        return panel;
    }

    private static void addCardTitle(JPanel panel, String title, String subtitle) {
        JPanel heading = new JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        heading.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, INK),
                BorderFactory.createEmptyBorder(0, 0, 9, 0)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 19));
        titleLabel.setForeground(INK);
        heading.add(titleLabel, BorderLayout.WEST);
        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            subtitleLabel.setForeground(MUTED);
            heading.add(subtitleLabel, BorderLayout.EAST);
        }
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(heading);
    }

    private static void addKeyValue(JPanel panel, String key, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, LINE));
        row.setPreferredSize(new Dimension(10, 42));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        keyLabel.setForeground(MUTED);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(INK);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(keyLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        panel.add(row);
    }

    private static void addTotal(JPanel panel, String label, String amount) {
        JPanel total = new JPanel(new BorderLayout(12, 0));
        total.setOpaque(true);
        total.setBackground(INK);
        total.setBorder(BorderFactory.createEmptyBorder(13, 17, 13, 17));
        total.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        total.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelText.setForeground(SUN);
        JLabel amountText = new JLabel(amount);
        amountText.setFont(new Font("Georgia", Font.BOLD, 24));
        amountText.setForeground(SUN);
        total.add(labelText, BorderLayout.WEST);
        total.add(amountText, BorderLayout.EAST);
        panel.add(Box.createVerticalStrut(12));
        panel.add(total);
    }

    private static JTextField styledField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        field.setForeground(INK);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(100, 46));
        return field;
    }

    private static JButton primaryButton(String text) {
        JButton button = button(text, INK, SUN);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)));
        return button;
    }

    private static JButton secondaryButton(String text) {
        JButton button = button(text, Color.WHITE, INK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(9, 16, 9, 16)));
        return button;
    }

    private static JButton button(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JPanel actionRow() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        return actions;
    }

    private static void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(INK);
        table.setBackground(Color.WHITE);
        table.setGridColor(LINE);
        table.setSelectionBackground(SELECTED);
        table.setSelectionForeground(INK);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setForeground(MUTED);
        table.getTableHeader().setBackground(new Color(255, 253, 240));
        table.getTableHeader().setReorderingAllowed(false);
    }

    private static List<Reservation> createSampleReservations() {
        return new ArrayList<>(Arrays.asList(
                new Reservation("RES-001", "Juan Dela Cruz", "0917 555 0101", "203", "Double",
                        new BigDecimal("2500.00"), "Walk-in", "Confirmed", "Regular",
                        LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 18), null),
                new Reservation("RES-002", "Maria Santos", "0918 555 0102", "105", "Single",
                        new BigDecimal("1500.00"), "Online", "Pending", "Regular",
                        LocalDate.of(2026, 10, 17), LocalDate.of(2026, 10, 19), null),
                new Reservation("RES-003", "Pedro Reyes", "0917 555 0103", "301", "Deluxe",
                        new BigDecimal("3500.00"), "Walk-in", "Checked-In", "Regular",
                        LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 16), null),
                new Reservation("RES-007", "Ana Lim", "0919 555 0107", "208", "Double",
                        new BigDecimal("2500.00"), "Online", "Checked-In", "Discounted",
                        LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 16),
                        "Senior Citizen ID · SC-204518"),
                new Reservation("RES-009", "Liza Ramos", "0920 555 0109", "102", "Double",
                        new BigDecimal("2500.00"), "Walk-in", "Checked-In", "Regular",
                        LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16), null)));
    }

    private static Bill calculateBill(Reservation reservation) {
        long nights = ChronoUnit.DAYS.between(reservation.checkIn, reservation.checkOut);
        if (nights <= 0) {
            throw new IllegalStateException("Sample reservation check-out must be after check-in.");
        }
        BigDecimal roomCost = reservation.rate.multiply(BigDecimal.valueOf(nights));
        boolean discounted = "Discounted".equals(reservation.classification);
        BigDecimal discountRate = discounted ? new BigDecimal("0.20") : BigDecimal.ZERO;
        BigDecimal taxRate = discounted ? BigDecimal.ZERO : new BigDecimal("0.12");
        BigDecimal discount = roomCost.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = roomCost.subtract(discount);
        BigDecimal tax = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        return new Bill(nights, roomCost, discountRate, discount, taxRate, tax,
                taxableAmount.add(tax).setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal parseAmount(String value) {
        try {
            String normalized = value == null ? "" : value.replace(",", "").trim();
            if (normalized.isEmpty()) {
                return null;
            }
            BigDecimal amount = new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
            return amount.signum() > 0 ? amount : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static BigDecimal roundUpTo(BigDecimal amount, int increment) {
        BigDecimal step = BigDecimal.valueOf(increment);
        return amount.divide(step, 0, RoundingMode.CEILING)
                .multiply(step).setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String money(BigDecimal value) {
        AMOUNT_FORMAT.setMinimumFractionDigits(2);
        AMOUNT_FORMAT.setMaximumFractionDigits(2);
        return "PHP " + AMOUNT_FORMAT.format(value);
    }

    private static String percent(BigDecimal value) {
        return value.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }

    private static String formatDate(LocalDate date) {
        return DATE_FORMAT.format(date);
    }

    private static final class Reservation {
        private final String reference;
        private final String name;
        private final String contact;
        private final String room;
        private final String roomType;
        private final BigDecimal rate;
        private final String reservationType;
        private String status;
        private final String classification;
        private final LocalDate checkIn;
        private final LocalDate checkOut;
        private final String discountId;

        private Reservation(String reference, String name, String contact, String room, String roomType,
                BigDecimal rate, String reservationType, String status, String classification,
                LocalDate checkIn, LocalDate checkOut, String discountId) {
            this.reference = reference;
            this.name = name;
            this.contact = contact;
            this.room = room;
            this.roomType = roomType;
            this.rate = rate;
            this.reservationType = reservationType;
            this.status = status;
            this.classification = classification;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.discountId = discountId;
        }
    }

    private static final class Bill {
        private final long nights;
        private final BigDecimal roomCost;
        private final BigDecimal discountRate;
        private final BigDecimal discount;
        private final BigDecimal taxRate;
        private final BigDecimal tax;
        private final BigDecimal total;

        private Bill(long nights, BigDecimal roomCost, BigDecimal discountRate, BigDecimal discount,
                BigDecimal taxRate, BigDecimal tax, BigDecimal total) {
            this.nights = nights;
            this.roomCost = roomCost;
            this.discountRate = discountRate;
            this.discount = discount;
            this.taxRate = taxRate;
            this.tax = tax;
            this.total = total;
        }
    }

    private static final class Payment {
        private final BigDecimal tendered;
        private final BigDecimal change;
        private final String receiptReference;

        private Payment(BigDecimal tendered, BigDecimal change, String receiptReference) {
            this.tendered = tendered;
            this.change = change;
            this.receiptReference = receiptReference;
        }
    }
}
