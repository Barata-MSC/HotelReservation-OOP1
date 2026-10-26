package com.hotel.desktop.customer;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CustomerBookingQuote;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.CustomerBookingRequest;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.service.CustomerReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JFileChooser;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

public final class CustomerBookingScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private final CustomerReservationService service;
    private final BiConsumer<String, CustomerBookingRequest> onContinue;
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final javax.swing.JTextField name = StaffUi.textField(22);
    private final javax.swing.JTextField email = StaffUi.textField(22);
    private final javax.swing.JTextField contact = StaffUi.textField(16);
    private final JComboBox<GuestClassification> classification =
            new JComboBox<>(GuestClassification.values());
    private final PlaceholderTextField discountIdNumber = new PlaceholderTextField(16,
            "Enter Senior Citizen or PWD ID number");
    private final JLabel discountIdFileLabel = new JLabel("No ID proof selected");
    private final javax.swing.JButton chooseDiscountIdProof = StaffUi.secondaryButton("Upload ID");
    private final JSpinner checkInDate = dateSpinner(LocalDate.now().plusDays(1));
    private final JSpinner checkInTime = timeSpinner(LocalTime.of(15, 0));
    private final JSpinner checkOutDate = dateSpinner(LocalDate.now().plusDays(2));
    private final JSpinner checkOutTime = timeSpinner(LocalTime.of(11, 0));
    private final JSpinner guestCount = new JSpinner(new SpinnerNumberModel(1, 1, Integer.MAX_VALUE, 1));
    private final JComboBox<RoomType> roomType = new JComboBox<>(RoomType.values());
    private final JComboBox<Room> availableRooms = new JComboBox<>();
    private final JLabel rateSummary = new JLabel("Choose dates and find available rooms.");
    private final javax.swing.JButton findRooms = StaffUi.secondaryButton("Find available rooms");
    private AuthenticatedUser customer;
    private long searchGeneration;
    private String changeReference;
    private int preferredRoomNumber;
    private CustomerBooking editingBooking;
    private File discountIdFile;
    private final javax.swing.JButton continueButton = StaffUi.primaryButton("Continue to payment");

    public CustomerBookingScreen(
            CustomerReservationService service, BiConsumer<String, CustomerBookingRequest> onContinue) {
        this.service = java.util.Objects.requireNonNull(service);
        this.onContinue = java.util.Objects.requireNonNull(onContinue);
        JPanel body = StaffUi.createPage(this, "Book a room", "Tell us about your stay");
        body.add(buildForm(), BorderLayout.CENTER);
        classification.setSelectedItem(GuestClassification.REGULAR);
        classification.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                String text = value instanceof GuestClassification kind ? kind.getDatabaseValue() : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        roomType.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                String text = value instanceof RoomType kind ? kind.getDatabaseValue() : "";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        classification.addActionListener(event -> updateDiscountFields());
        roomType.addActionListener(event -> searchRooms());
        checkInDate.addChangeListener(event -> searchRooms());
        checkInTime.addChangeListener(event -> searchRooms());
        checkOutDate.addChangeListener(event -> searchRooms());
        checkOutTime.addChangeListener(event -> searchRooms());
        guestCount.addChangeListener(event -> searchRooms());
        availableRooms.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean selected, boolean focus) {
                String text = value instanceof Room room
                        ? "Room " + room.getRoomNumber() + " · " + room.getRoomType().getDatabaseValue()
                                + " · " + room.getCapacity() + " guest(s) · "
                                + room.getPricePerDay().setScale(2).toPlainString() + " per night"
                        : "No room selected";
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        availableRooms.addActionListener(event -> updateRateSummary());
        findRooms.addActionListener(event -> searchRooms());
        continueButton.addActionListener(event -> continueToPayment());
        updateDiscountFields();
    }

    public void setCustomer(AuthenticatedUser customer) {
        this.customer = customer;
        searchRooms();
    }

    public void startNewBooking() {
        changeReference = null;
        preferredRoomNumber = 0;
        editingBooking = null;
        name.setText(customer == null ? "" : customer.getFullName());
        email.setText(customer == null ? "" : customer.getEmail());
        contact.setText(customer == null ? "" : customer.getContactNumber());
        classification.setSelectedItem(GuestClassification.REGULAR);
        discountIdNumber.setText("");
        discountIdFile = null;
        discountIdFileLabel.setText("No ID proof selected");
        checkInDate.setValue(Date.from(LocalDate.now().plusDays(1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        checkInTime.setValue(Date.from(LocalDate.now().atTime(15, 0)
                .atZone(ZoneId.systemDefault()).toInstant()));
        checkOutDate.setValue(Date.from(LocalDate.now().plusDays(2)
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        checkOutTime.setValue(Date.from(LocalDate.now().atTime(11, 0)
                .atZone(ZoneId.systemDefault()).toInstant()));
        guestCount.setValue(1);
        roomType.setSelectedItem(RoomType.SINGLE);
        continueButton.setText("Continue to payment");
        searchRooms();
    }

    public void prepareChange(CustomerBooking booking) {
        changeReference = booking.reservation().reference();
        preferredRoomNumber = booking.reservation().roomNumber();
        editingBooking = booking;
        name.setText(booking.reservation().guest().fullName());
        email.setText(booking.reservation().guest().email());
        contact.setText(booking.reservation().guest().contactNumber());
        classification.setSelectedItem(booking.reservation().guest().classification());
        discountIdNumber.setText(booking.reservation().guest().discountIdNumber());
        discountIdFile = null;
        discountIdFileLabel.setText(booking.reservation().guest().discountIdProofPath() == null
                ? "No ID proof on file — upload required"
                : "ID proof on file — upload a replacement if needed");
        setDate(checkInDate, booking.reservation().checkIn());
        setTime(checkInTime, booking.checkInTime());
        setDate(checkOutDate, booking.reservation().checkOut());
        setTime(checkOutTime, booking.checkOutTime());
        guestCount.setValue(booking.reservation().guestCount());
        roomType.setSelectedItem(RoomType.fromDatabaseValue(booking.reservation().roomType()));
        continueButton.setText("Save reservation changes");
        searchRooms();
    }

    private JPanel buildForm() {
        JPanel card = CustomerUi.card();
        card.setLayout(new BorderLayout(0, 12));
        JPanel details = new JPanel(new GridLayout(1, 2, 18, 0));
        details.setOpaque(false);
        details.add(fieldSection("Guest details", new FieldEntry[]{
            new FieldEntry("Full name", name),
            new FieldEntry("Email address", email),
            new FieldEntry("Contact number", contact),
            new FieldEntry("Classification", classification),
            new FieldEntry("Discount ID number", discountIdNumber),
            new FieldEntry("Discount ID proof", idProofPicker())
        }));
        details.add(fieldSection("Stay details", new FieldEntry[]{
            new FieldEntry("Check-in date", checkInDate),
            new FieldEntry("Check-in time", checkInTime),
            new FieldEntry("Check-out date", checkOutDate),
            new FieldEntry("Check-out time", checkOutTime),
            new FieldEntry("Number of guests", guestCount),
            new FieldEntry("Room type", roomType)
        }));
        JPanel formContent = new JPanel(new GridBagLayout());
        formContent.setBackground(CustomerUi.SURFACE);
        GridBagConstraints detailsConstraints = new GridBagConstraints();
        detailsConstraints.gridx = 0;
        detailsConstraints.gridy = 0;
        detailsConstraints.weightx = 1;
        detailsConstraints.fill = GridBagConstraints.HORIZONTAL;
        formContent.add(details, detailsConstraints);
        JPanel roomPicker = new JPanel(new BorderLayout(8, 0));
        roomPicker.setOpaque(false);
        roomPicker.add(availableRooms, BorderLayout.CENTER);
        roomPicker.add(findRooms, BorderLayout.EAST);
        JPanel roomSection = fieldSection("Room selection", new FieldEntry[]{
            new FieldEntry("Available rooms", roomPicker)
        });
        GridBagConstraints roomConstraints = new GridBagConstraints();
        roomConstraints.gridx = 0;
        roomConstraints.gridy = 1;
        roomConstraints.weightx = 1;
        roomConstraints.fill = GridBagConstraints.HORIZONTAL;
        roomConstraints.insets = new Insets(12, 0, 0, 0);
        formContent.add(roomSection, roomConstraints);
        rateSummary.setFont(StaffUi.bodyFont(14));
        rateSummary.setForeground(CustomerUi.MUTED);
        GridBagConstraints summaryConstraints = new GridBagConstraints();
        summaryConstraints.gridx = 0;
        summaryConstraints.gridy = 2;
        summaryConstraints.anchor = GridBagConstraints.WEST;
        summaryConstraints.insets = new Insets(10, 0, 0, 0);
        formContent.add(rateSummary, summaryConstraints);

        JScrollPane scroll = new JScrollPane(formContent);
        CustomerUi.styleScrollPane(scroll);
        scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(CustomerUi.SURFACE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.add(feedback, BorderLayout.CENTER);
        actions.add(continueButton, BorderLayout.EAST);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel idProofPicker() {
        JPanel picker = new JPanel(new BorderLayout(8, 0));
        picker.setOpaque(false);
        picker.add(discountIdFileLabel, BorderLayout.CENTER);
        chooseDiscountIdProof.addActionListener(event -> chooseDiscountIdFile());
        picker.add(chooseDiscountIdProof, BorderLayout.EAST);
        return picker;
    }

    private JPanel fieldSection(String title, FieldEntry[] entries) {
        JPanel section = new JPanel(new GridBagLayout());
        section.setBackground(CustomerUi.SURFACE);
        section.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(CustomerUi.LINE),
                javax.swing.BorderFactory.createEmptyBorder(10, 12, 8, 12)));
        JLabel heading = new JLabel(title);
        heading.setFont(StaffUi.headingFont(18));
        heading.setForeground(CustomerUi.INK);
        GridBagConstraints headingConstraints = new GridBagConstraints();
        headingConstraints.gridx = 0;
        headingConstraints.gridy = 0;
        headingConstraints.gridwidth = 2;
        headingConstraints.anchor = GridBagConstraints.WEST;
        headingConstraints.insets = new Insets(0, 0, 5, 0);
        section.add(heading, headingConstraints);
        int row = 1;
        for (FieldEntry entry : entries) {
            row = addField(section, row, entry.label(), entry.component());
        }
        return section;
    }

    private static int addField(JPanel panel, int row, String label, Component field) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(3, 0, 3, 8);
        panel.add(StaffUi.fieldLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(3, 0, 3, 0);
        panel.add(field, fieldConstraints);
        if (field instanceof javax.swing.JTextField textField) {
            textField.setBackground(CustomerUi.SURFACE);
        }
        if (field instanceof JComboBox<?> comboBox) {
            comboBox.setFont(StaffUi.bodyFont(14));
            comboBox.setBackground(CustomerUi.SURFACE);
        }
        if (field instanceof JSpinner spinner) {
            spinner.setFont(StaffUi.bodyFont(14));
            spinner.setBackground(CustomerUi.SURFACE);
            if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
                editor.getTextField().setBackground(CustomerUi.SURFACE);
            }
        }
        return row + 1;
    }

    private void searchRooms() {
        if (customer == null) {
            return;
        }
        LocalDate arrival = spinnerDate(checkInDate);
        LocalDate departure = spinnerDate(checkOutDate);
        RoomType selectedType = (RoomType) roomType.getSelectedItem();
        int guests = (Integer) guestCount.getValue();
        long generation = ++searchGeneration;
        String referenceToExclude = changeReference;
        findRooms.setEnabled(false);
        StaffUi.showFeedback(feedback, "Checking room availability…", false);
        availableRooms.removeAllItems();
        new SwingWorker<List<Room>, Void>() {
            @Override
            protected List<Room> doInBackground() throws SQLException {
                return referenceToExclude == null
                        ? service.findAvailableRooms(customer, arrival, departure, guests, selectedType)
                        : service.findAvailableRoomsForChange(
                                customer, referenceToExclude, arrival, departure, guests, selectedType);
            }

            @Override
            protected void done() {
                if (generation != searchGeneration) {
                    return;
                }
                findRooms.setEnabled(true);
                try {
                    List<Room> rooms = get();
                    rooms.forEach(availableRooms::addItem);
                    if (preferredRoomNumber > 0) {
                        for (int index = 0; index < availableRooms.getItemCount(); index++) {
                            if (availableRooms.getItemAt(index).getRoomNumber() == preferredRoomNumber) {
                                availableRooms.setSelectedIndex(index);
                                preferredRoomNumber = 0;
                                break;
                            }
                        }
                    }
                    StaffUi.showFeedback(feedback, rooms.isEmpty()
                            ? "No rooms match the selected dates, capacity and room type."
                            : rooms.size() + " available room(s) found.", rooms.isEmpty());
                    updateRateSummary();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    StaffUi.showFeedback(feedback, "Room search was interrupted.", true);
                } catch (java.util.concurrent.ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    StaffUi.showFeedback(feedback, "Unable to load room availability: "
                            + cause.getMessage(), true);
                }
            }
        }.execute();
    }

    private void continueToPayment() {
        try {
            Guest guest = guestFromForm();
            Room room = (Room) availableRooms.getSelectedItem();
            if (room == null) {
                throw new ValidationException("Find and select an available room first.");
            }
            LocalDateTime arrival = dateTime(checkInDate, checkInTime);
            LocalDateTime departure = dateTime(checkOutDate, checkOutTime);
            CustomerBookingRequest request = new CustomerBookingRequest(
                    guest, room.getRoomNumber(), room.getRoomType(), arrival, departure,
                    (Integer) guestCount.getValue());
            CustomerBookingQuote quote = service.quote(
                    guest, room.getPricePerDay(), arrival.toLocalDate(), departure.toLocalDate());
            String provisional = quote.discountIsProvisional()
                    ? " · Discount provisional until staff verifies your ID" : "";
            if (changeReference == null) {
                rateSummary.setText("Total " + money(quote.bill().total())
                        + " · suggested 50% initial payment "
                        + money(quote.downpayment()) + provisional);
                StaffUi.showFeedback(feedback, "Review the estimate and submit your payment proof.", false);
            } else {
                java.math.BigDecimal newBalance = quote.bill().total()
                        .subtract(editingBooking.amountSubmitted()).max(java.math.BigDecimal.ZERO);
                rateSummary.setText("Updated total " + money(quote.bill().total())
                        + " · amount submitted " + money(editingBooking.amountSubmitted())
                        + " · balance " + money(newBalance) + provisional);
                StaffUi.showFeedback(feedback, "Room availability will be rechecked when you save.", false);
            }
            onContinue.accept(changeReference, request);
        } catch (ValidationException exception) {
            StaffUi.showFeedback(feedback, exception.getMessage(), true);
        }
    }

    private Guest guestFromForm() {
        GuestClassification selected = (GuestClassification) classification.getSelectedItem();
        String proofPath = selected == GuestClassification.DISCOUNTED
                ? selectedDiscountProofPath() : null;
        if (selected == GuestClassification.DISCOUNTED
                && (proofPath == null || proofPath.isBlank())) {
            throw new ValidationException("Upload your discount ID proof before continuing.");
        }
        return new Guest(
                0, name.getText().trim(), contact.getText().trim(), email.getText().trim(),
                selected,
                null,
                selected == GuestClassification.DISCOUNTED ? discountIdNumber.getText().trim() : null,
                false,
                proofPath);
    }

    private String selectedDiscountProofPath() {
        if (discountIdFile != null) {
            return discountIdFile.getAbsolutePath();
        }
        return editingBooking == null
                ? null : editingBooking.reservation().guest().discountIdProofPath();
    }

    private void updateDiscountFields() {
        boolean discounted = classification.getSelectedItem() == GuestClassification.DISCOUNTED;
        discountIdNumber.setEnabled(discounted);
        chooseDiscountIdProof.setEnabled(discounted);
        discountIdFileLabel.setEnabled(discounted);
        if (!discounted) {
            discountIdNumber.setText("");
            discountIdFile = null;
            discountIdFileLabel.setText("Not required for Regular guests");
        } else if (discountIdFile == null
                && (editingBooking == null
                        || editingBooking.reservation().guest().discountIdProofPath() == null)) {
            discountIdFileLabel.setText("No ID proof selected");
        }
    }

    private void chooseDiscountIdFile() {
        javax.swing.JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Upload discount ID proof");
        chooser.setAcceptAllFileFilterUsed(true);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            discountIdFile = chooser.getSelectedFile();
            discountIdFileLabel.setText(discountIdFile.getName());
        }
    }

    private void updateRateSummary() {
        Room room = (Room) availableRooms.getSelectedItem();
        rateSummary.setText(room == null ? "Choose dates and find available rooms."
                : "Room " + room.getRoomNumber() + " · "
                        + money(room.getPricePerDay()) + " per night");
    }

    private static JSpinner dateSpinner(LocalDate date) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(
                Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                null, null, java.util.Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    private static JSpinner timeSpinner(LocalTime time) {
        Date value = Date.from(LocalDate.now().atTime(time).atZone(ZoneId.systemDefault()).toInstant());
        JSpinner spinner = new JSpinner(new SpinnerDateModel(value, null, null, java.util.Calendar.MINUTE));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "hh:mm a"));
        return spinner;
    }

    private static void setDate(JSpinner spinner, LocalDate date) {
        spinner.setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private static void setTime(JSpinner spinner, LocalTime time) {
        spinner.setValue(Date.from(LocalDate.now().atTime(time)
                .atZone(ZoneId.systemDefault()).toInstant()));
    }

    private static LocalDate spinnerDate(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static LocalDateTime dateTime(JSpinner date, JSpinner time) {
        return spinnerDate(date).atTime(
                ((Date) time.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime());
    }

    private static String money(java.math.BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private record FieldEntry(String label, Component component) {
    }

    private static final class PlaceholderTextField extends javax.swing.JTextField {
        private static final long serialVersionUID = 1L;
        private final String placeholder;

        private PlaceholderTextField(int columns, String placeholder) {
            super(columns);
            this.placeholder = placeholder;
            setFont(StaffUi.BODY_FONT);
            setForeground(StaffUi.INK);
            setBackground(CustomerUi.SURFACE);
            setToolTipText(placeholder);
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            super.paintComponent(graphics);
            if (getText().isEmpty() && !hasFocus()) {
                java.awt.Graphics2D copy = (java.awt.Graphics2D) graphics.create();
                copy.setFont(getFont());
                copy.setColor(StaffUi.MUTED);
                copy.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                copy.setRenderingHint(java.awt.RenderingHints.KEY_FRACTIONALMETRICS,
                        java.awt.RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                java.awt.Insets insets = getInsets();
                copy.drawString(placeholder, insets.left + 2,
                        getHeight() - insets.bottom - copy.getFontMetrics().getDescent());
                copy.dispose();
            }
        }
    }
}
