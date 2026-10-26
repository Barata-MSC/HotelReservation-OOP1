package com.hotel.desktop.customer;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.CustomerBookingQuote;
import com.hotel.core.model.CustomerBookingRequest;
import com.hotel.core.model.CustomerPaymentSubmission;
import com.hotel.core.model.CustomerReceipt;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.CustomerPaymentMethod;
import com.hotel.core.service.CustomerReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public final class CustomerPaymentScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private final CustomerReservationService service;
    private final Consumer<CustomerReceipt> onReceipt;
    private final JLabel paymentAmount = new JLabel("Loading payment amount…");
    private final JLabel fileLabel = new JLabel("No proof selected");
    private final JTextArea feedback = new JTextArea(2, 50);
    private final javax.swing.JTextField amountField = StaffUi.textField(16);
    private final JComboBox<CustomerPaymentMethod> method =
            new JComboBox<>(CustomerPaymentMethod.values());
    private final javax.swing.JTextField referenceDetails = StaffUi.textField(30);
    private final javax.swing.JButton submit = StaffUi.primaryButton("Submit & view receipt");
    private AuthenticatedUser customer;
    private CustomerBookingRequest newBooking;
    private CustomerBooking balanceBooking;
    private File proofFile;
    private BigDecimal bookingTotal;
    private BigDecimal amountAlreadySubmitted = BigDecimal.ZERO;
    private BigDecimal suggestedAmount;

    public CustomerPaymentScreen(
            CustomerReservationService service, Consumer<CustomerReceipt> onReceipt) {
        this.service = java.util.Objects.requireNonNull(service);
        this.onReceipt = java.util.Objects.requireNonNull(onReceipt);
        JPanel body = CustomerUi.createPage(this, "Payment", "Submit your payment details securely");
        body.setBorder(new EmptyBorder(14, 38, 56, 38));
        feedback.setFont(StaffUi.bodyFont(13));
        feedback.setForeground(StaffUi.MUTED);
        feedback.setOpaque(false);
        feedback.setEditable(false);
        feedback.setFocusable(false);
        feedback.setLineWrap(true);
        feedback.setWrapStyleWord(true);
        feedback.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 2, 2, 2));
        buildForm(body);
        method.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                String text = value instanceof CustomerPaymentMethod paymentMethod
                        ? paymentMethod.getDisplayName() : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        amountField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                updatePaymentSummary();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                updatePaymentSummary();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                updatePaymentSummary();
            }
        });
        submit.addActionListener(event -> submitPayment());
    }

    public void setCustomer(AuthenticatedUser customer) {
        this.customer = customer;
    }

    public void prepareBooking(CustomerBookingRequest request) {
        newBooking = request;
        balanceBooking = null;
        proofFile = null;
        fileLabel.setText("No proof selected");
        referenceDetails.setText("");
        amountField.setText("");
        bookingTotal = null;
        amountAlreadySubmitted = BigDecimal.ZERO;
        suggestedAmount = null;
        submit.setEnabled(false);
        showFeedback("Rechecking the room and calculating your suggested initial payment…", false);
        paymentAmount.setText("Loading payment amount…");
        new SwingWorker<CustomerBookingQuote, Void>() {
            @Override
            protected CustomerBookingQuote doInBackground() throws Exception {
                List<Room> rooms = service.findAvailableRooms(
                        customer, request.checkIn().toLocalDate(), request.checkOut().toLocalDate(),
                        request.guestCount(), request.roomType());
                Room room = rooms.stream()
                        .filter(available -> available.getRoomNumber() == request.roomNumber())
                        .findFirst()
                        .orElseThrow(() -> new ValidationException(
                                "That room is no longer available. Return to room search and choose another."));
                return service.quote(request.guest(), room.getPricePerDay(),
                        request.checkIn().toLocalDate(), request.checkOut().toLocalDate());
            }

            @Override
            protected void done() {
                try {
                    CustomerBookingQuote quote = get();
                    bookingTotal = quote.bill().total();
                    amountAlreadySubmitted = BigDecimal.ZERO;
                    suggestedAmount = quote.downpayment();
                    amountField.setText(money(quote.downpayment()));
                    updatePaymentSummary(quote.discountIsProvisional());
                    submit.setEnabled(true);
                    showFeedback(
                            "Enter the amount paid. Submitted payments remain partial until verified.", false);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    fail("Payment amount calculation was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    fail("Unable to prepare payment: " + exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    public void prepareBalancePayment(CustomerBooking booking) {
        newBooking = null;
        balanceBooking = booking;
        proofFile = null;
        fileLabel.setText("No proof selected");
        referenceDetails.setText("");
        bookingTotal = booking.total();
        amountAlreadySubmitted = booking.amountSubmitted();
        suggestedAmount = booking.balance();
        amountField.setText(money(booking.balance()));
        updatePaymentSummary(false);
        submit.setEnabled(true);
        showFeedback("Upload proof for the remaining balance.", false);
    }

    private void buildForm(JPanel body) {
        JPanel card = CustomerUi.card();
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        int row = 0;
        row = addField(fields, row, "Booking amount", paymentAmount);
        row = addField(fields, row, "Amount paid", amountField);
        row = addField(fields, row, "Payment method", method);
        row = addField(fields, row, "Reference or details", referenceDetails);

        JPanel proofPicker = new JPanel(new BorderLayout(8, 0));
        proofPicker.setOpaque(false);
        proofPicker.add(fileLabel, BorderLayout.CENTER);
        javax.swing.JButton choose = StaffUi.secondaryButton("Choose proof file");
        choose.addActionListener(event -> chooseProof());
        proofPicker.add(choose, BorderLayout.EAST);
        row = addField(fields, row, "Payment proof (any file type; max 5 MB)", proofPicker);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row;
        filler.gridwidth = 2;
        filler.weighty = 1;
        fields.add(javax.swing.Box.createVerticalGlue(), filler);
        card.add(fields, BorderLayout.CENTER);
        body.add(card, BorderLayout.CENTER);

        JPanel actions = new JPanel();
        actions.setOpaque(false);
        actions.setLayout(new javax.swing.BoxLayout(actions, javax.swing.BoxLayout.Y_AXIS));
        JPanel buttonRow = new JPanel(new BorderLayout(10, 0));
        buttonRow.setOpaque(false);
        buttonRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 0, 0, 0));
        buttonRow.add(submit, BorderLayout.EAST);
        actions.add(feedback);
        actions.add(buttonRow);
        body.add(actions, BorderLayout.SOUTH);
    }

    private static int addField(JPanel panel, int row, String label, Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(7, 0, 7, 18);
        panel.add(StaffUi.fieldLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(7, 0, 7, 0);
        panel.add(field, fieldConstraints);
        if (field instanceof JComboBox<?> comboBox) {
            comboBox.setFont(StaffUi.bodyFont(14));
        }
        return row + 1;
    }

    private void chooseProof() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select payment proof");
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            proofFile = chooser.getSelectedFile();
            fileLabel.setText(proofFile.getName());
        }
    }

    private void updatePaymentSummary() {
        updatePaymentSummary(false);
    }

    private void updatePaymentSummary(boolean discountIsProvisional) {
        if (bookingTotal == null) {
            return;
        }
        StringBuilder summary = new StringBuilder("<html>Booking total: ")
                .append(money(bookingTotal));
        if (newBooking != null && suggestedAmount != null) {
            summary.append("<br>Minimum 50% downpayment required: <b>")
                    .append(money(suggestedAmount)).append("</b>");
        } else if (suggestedAmount != null) {
            summary.append("<br>Full remaining balance required: <b>")
                    .append(money(suggestedAmount)).append("</b>");
        }
        try {
            BigDecimal entered = new BigDecimal(amountField.getText().trim());
            BigDecimal remaining = bookingTotal.subtract(amountAlreadySubmitted).subtract(entered)
                    .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            summary.append("<br>Balance after this submission: ").append(money(remaining));
        } catch (NumberFormatException | ArithmeticException ignored) {
            summary.append("<br>Enter an amount to see the updated balance.");
        }
        if (discountIsProvisional) {
            summary.append("<br>Discounted rate is provisional until staff verifies your ID.");
        }
        paymentAmount.setText(summary.append("</html>").toString());
    }

    private void submitPayment() {
        if (customer == null) {
            fail("Sign in with a customer account before submitting a payment.");
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountField.getText().trim());
        } catch (NumberFormatException exception) {
            fail("Enter a valid payment amount.");
            return;
        }
        if (suggestedAmount == null || amount.compareTo(suggestedAmount) < 0) {
            String required = suggestedAmount == null ? "the required amount" : "PHP " + money(suggestedAmount);
            fail("The minimum payment is " + required + ". Enter the required amount to continue.");
            javax.swing.JOptionPane.showMessageDialog(this,
                    "The minimum payment is " + required + ". Your receipt will be available after a valid submission.",
                    "Minimum payment required", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newBooking == null && amount.compareTo(suggestedAmount) != 0) {
            fail("Pay the full remaining balance of PHP " + money(suggestedAmount) + ".");
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Please enter the full remaining balance of PHP " + money(suggestedAmount) + ".",
                    "Full balance required", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newBooking != null && bookingTotal != null && amount.compareTo(bookingTotal) > 0) {
            fail("Payment cannot exceed the booking total of PHP " + money(bookingTotal) + ".");
            return;
        }
        if (proofFile == null) {
            fail("Choose a payment proof file.");
            return;
        }
        CustomerPaymentSubmission payment = new CustomerPaymentSubmission(
                (CustomerPaymentMethod) method.getSelectedItem(),
                amount,
                referenceDetails.getText().trim(),
                proofFile.toPath());
        submit.setEnabled(false);
        showFeedback("Saving your booking and payment submission…", false);
        new SwingWorker<CustomerReceipt, Void>() {
            @Override
            protected CustomerReceipt doInBackground() throws Exception {
                if (newBooking != null) {
                    return service.createBooking(customer, newBooking, payment);
                }
                return service.payRemainingBalance(
                        customer, balanceBooking.reservation().reference(), payment);
            }

            @Override
            protected void done() {
                try {
                    CustomerReceipt receipt = get();
                    onReceipt.accept(receipt);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    fail("Payment submission was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String message = cause.getMessage() == null
                            ? cause.getClass().getSimpleName() : cause.getMessage();
                    fail("Unable to submit booking/payment: " + message);
                }
            }
        }.execute();
    }

    private void fail(String message) {
        submit.setEnabled(true);
        showFeedback(message, true);
    }

    private void showFeedback(String message, boolean error) {
        feedback.setText(message);
        feedback.setForeground(error ? new java.awt.Color(179, 38, 30) : StaffUi.MUTED);
    }

    private static String money(java.math.BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
