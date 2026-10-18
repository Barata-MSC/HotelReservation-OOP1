package com.hotel.desktop.reservation;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.service.ReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public final class WalkInReservationScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ReservationService reservationService;
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
    private final JTextField checkOutField = StaffUi.textField(12);
    private final JTextField guestCountField = StaffUi.textField(4);
    private final JComboBox<Room> roomBox = new JComboBox<>();
    private final JLabel feedback = StaffUi.feedbackLabel();
    private final JLabel rateLabel = new JLabel("Search dates to list available rooms.");

    public WalkInReservationScreen(ReservationService reservationService, Runnable onReservationCreated) {
        this.reservationService = java.util.Objects.requireNonNull(reservationService, "reservationService");
        this.onReservationCreated = java.util.Objects.requireNonNull(onReservationCreated, "onReservationCreated");
        JPanel body = StaffUi.createPage(this, "Walk-in reservation",
                "Register an arriving guest and reserve a clean, available room");
        body.add(buildForm(), BorderLayout.CENTER);
        checkInField.setText(LocalDate.now().toString());
        checkOutField.setText(LocalDate.now().plusDays(1).toString());
        guestCountField.setText("1");
        classificationBox.addActionListener(event -> updateDiscountFields());
        roomBox.addActionListener(event -> updateSelectedRate());
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

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        int row = 0;
        row = addField(fields, row, "Guest full name", nameField);
        row = addField(fields, row, "Contact number", contactField);
        row = addField(fields, row, "Email address", emailField);
        row = addField(fields, row, "Guest classification", classificationBox);
        row = addField(fields, row, "Discount ID type", idTypeField);
        row = addField(fields, row, "Discount ID number", idNumberField);
        row = addField(fields, row, "", idVerified);
        row = addField(fields, row, "Check-in (yyyy-MM-dd)", checkInField);
        row = addField(fields, row, "Check-out (yyyy-MM-dd)", checkOutField);
        row = addField(fields, row, "Number of guests", guestCountField);
        JPanel roomSelection = new JPanel(new BorderLayout(8, 0));
        roomSelection.setOpaque(false);
        roomSelection.add(roomBox, BorderLayout.CENTER);
        javax.swing.JButton findRooms = StaffUi.secondaryButton("Find rooms");
        findRooms.addActionListener(event -> reloadAvailableRooms());
        roomSelection.add(findRooms, BorderLayout.EAST);
        row = addField(fields, row, "Available rooms", roomSelection);
        rateLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        rateLabel.setForeground(StaffUi.MUTED);
        row = addField(fields, row, "Selected room rate", rateLabel);
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row;
        filler.gridwidth = 2;
        filler.weighty = 1;
        fields.add(Box.createVerticalGlue(), filler);
        formCard.add(fields, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(feedback, BorderLayout.CENTER);
        javax.swing.JButton create = StaffUi.primaryButton("Create walk-in reservation");
        create.addActionListener(event -> createReservation());
        actions.add(create, BorderLayout.EAST);
        formCard.add(actions, BorderLayout.SOUTH);
        content.add(formCard, BorderLayout.CENTER);
        return content;
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

    private void reloadAvailableRooms() {
        try {
            LocalDate checkIn = LocalDate.parse(checkInField.getText().trim());
            LocalDate checkOut = LocalDate.parse(checkOutField.getText().trim());
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
            Reservation reservation = reservationService.createWalkIn(
                    guest,
                    room.getRoomNumber(),
                    LocalDate.parse(checkInField.getText().trim()),
                    LocalDate.parse(checkOutField.getText().trim()),
                    guestCount);
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
}
