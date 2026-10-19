package com.hotel.desktop.houseKeepingStatus;

import com.hotel.core.exception.RoomNotFoundException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.RoomMonitoringStatus;
import com.hotel.core.service.ReservationService;
import com.hotel.core.service.RoomService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Housekeeping status editor backed by the shared room service. */
public final class HousekeepingScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(20, 18, 26);
    private static final Color SUN = new Color(255, 204, 0);
    private static final Color CANVAS = new Color(255, 222, 62);
    private static final Color MUTED = new Color(105, 91, 33);
    private static final Color HEADER_TEXT = new Color(74, 65, 0);
    private static final Color ROW_LINE = new Color(236, 230, 196);
    private static final Color SELECTED = new Color(255, 243, 184);
    private static final Color GREEN = new Color(46, 125, 50);
    private static final Color DIRTY = new Color(245, 158, 11);
    private static final Color MAINTENANCE = new Color(75, 67, 98);
    private static final Color OUT_OF_SERVICE = new Color(107, 114, 128);
    private static final Color ERROR = new Color(179, 38, 30);

    private final RoomService roomService;
    private final ReservationService reservationService;
    private final Runnable onRoomChanged;
    private final JTextField roomNumberField = new JTextField();
    private final JLabel currentStatusLabel = new JLabel("Current status   Find a room");
    private final JLabel roomValue = new JLabel("—");
    private final JLabel typeValue = new JLabel("Not recorded");
    private final JLabel floorValue = new JLabel("—");
    private final JLabel occupancyValue = new JLabel("—");
    private final JLabel bannerTitle = new JLabel(" ");
    private final JLabel bannerMessage = new JLabel(" ");
    private final JLabel bannerIcon = new JLabel("i");
    private final JPanel banner = new JPanel(new BorderLayout(12, 0));
    private final JPanel bannerHolder = new JPanel(new BorderLayout());
    private final JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
    private final ButtonGroup statusGroup = new ButtonGroup();
    private final JButton updateButton = primaryButton("Update");
    private final JButton cancelButton = outlinedButton("Cancel");
    private JButton findButton;
    private int currentRoomNumber = -1;
    private Room currentRoom;
    private HousekeepingStatus selectedStatus;
    private boolean operationPending;

    public HousekeepingScreen(RoomService roomService, Runnable onRoomChanged) {
        this(roomService, new ReservationService(), onRoomChanged);
    }

    public HousekeepingScreen(
            RoomService roomService,
            ReservationService reservationService,
            Runnable onRoomChanged) {
        this.roomService = Objects.requireNonNull(roomService, "roomService");
        this.reservationService = Objects.requireNonNull(reservationService, "reservationService");
        this.onRoomChanged = Objects.requireNonNull(onRoomChanged, "onRoomChanged");

        setLayout(new BorderLayout());
        setBackground(CANVAS);
        setPreferredSize(new Dimension(1024, 800));
        add(createContent(), BorderLayout.CENTER);
        setStatusOptionsEnabled(false);
        updateButton.setEnabled(false);
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CANVAS);
        JPanel topStrip = new JPanel();
        topStrip.setBackground(SUN);
        topStrip.setPreferredSize(new Dimension(1, 15));
        content.add(topStrip, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setBackground(CANVAS);
        body.setBorder(new EmptyBorder(14, 38, 40, 38));
        JPanel workflow = createWorkflow();
        body.add(createHeader(), BorderLayout.NORTH);
        body.add(workflow, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Housekeeping");
        title.setFont(new Font("Georgia", Font.BOLD, 36));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Review room readiness and update housekeeping status");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(MUTED);
        JLabel date = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));
        date.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        date.setForeground(MUTED);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new javax.swing.BoxLayout(labels, javax.swing.BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(javax.swing.Box.createVerticalStrut(3));
        labels.add(subtitle);
        labels.add(javax.swing.Box.createVerticalStrut(5));
        labels.add(date);
        header.add(labels, BorderLayout.CENTER);
        return header;
    }

    private JPanel createWorkflow() {
        JPanel workflow = new JPanel(new BorderLayout(0, 12));
        workflow.setOpaque(false);

        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.add(createLookupColumn());
        columns.add(createStatusColumn());
        Dimension columnsSize = columns.getPreferredSize();
        columns.setPreferredSize(new Dimension(0, columnsSize.height));
        JScrollPane workArea = new JScrollPane(
                columns,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        workArea.setBorder(BorderFactory.createEmptyBorder());
        workArea.setOpaque(false);
        workArea.getViewport().setOpaque(false);
        workArea.getVerticalScrollBar().setUnitIncrement(16);
        workflow.add(workArea, BorderLayout.CENTER);

        bannerHolder.setOpaque(false);
        bannerHolder.setPreferredSize(new Dimension(10, 64));
        bannerHolder.setMinimumSize(new Dimension(0, 64));
        bannerHolder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        bannerHolder.add(banner, BorderLayout.CENTER);
        workflow.add(bannerHolder, BorderLayout.SOUTH);
        return workflow;
    }

    private JPanel createLookupColumn() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));

        JPanel findCard = card();
        findCard.setLayout(new javax.swing.BoxLayout(findCard, javax.swing.BoxLayout.Y_AXIS));
        findCard.add(stepHeading("1", "Find the room"));
        findCard.add(hint("Enter a room number to view its current status."));
        findCard.add(fieldLabel("Room no."));
        roomNumberField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        roomNumberField.setForeground(INK);
        roomNumberField.setToolTipText("Enter a room number, for example 101");
        roomNumberField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 185, 168), 1, true),
                new EmptyBorder(0, 14, 0, 14)));
        roomNumberField.setPreferredSize(new Dimension(100, 48));
        roomNumberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        roomNumberField.addActionListener(event -> findRoom());

        findButton = primaryButton("Find");
        findButton.addActionListener(event -> findRoom());
        JPanel lookup = new JPanel(new BorderLayout(10, 0));
        lookup.setOpaque(false);
        lookup.add(roomNumberField, BorderLayout.CENTER);
        lookup.add(findButton, BorderLayout.EAST);
        lookup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        findCard.add(lookup);
        findCard.add(sampleRooms());
        alignChildrenLeft(findCard);
        column.add(findCard);
        column.add(javax.swing.Box.createVerticalStrut(16));
        column.add(createStatusGuide());

        return column;
    }

    private JPanel createStatusGuide() {
        JPanel guide = card();
        guide.setLayout(new javax.swing.BoxLayout(guide, javax.swing.BoxLayout.Y_AXIS));
        guide.add(stepHeading("i", "Status guide"));
        guide.add(statusGuideRow("Clean", "Ready for the next guest.", GREEN));
        guide.add(statusGuideRow("Dirty", "Needs cleaning before use.", DIRTY));
        guide.add(statusGuideRow("Under Maintenance", "Unavailable while being repaired.", MAINTENANCE));
        guide.add(statusGuideRow("Out of Service", "Removed from the room inventory.", OUT_OF_SERVICE));

        JLabel note = new JLabel("<html>Occupied rooms cannot be moved to Under Maintenance "
                + "or Out of Service.</html>");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        note.setForeground(MUTED);
        note.setBorder(new EmptyBorder(12, 0, 0, 0));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        guide.add(note);
        alignChildrenLeft(guide);
        return guide;
    }

    private JPanel statusGuideRow(String status, String description, Color color) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(9, 0, 0, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badge = new JLabel(status, JLabel.CENTER);
        badge.setOpaque(true);
        badge.setBackground(color);
        badge.setForeground(color == DIRTY || color == GREEN ? INK : Color.WHITE);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1, true),
                new EmptyBorder(3, 7, 3, 7)));
        row.add(badge, BorderLayout.WEST);

        JLabel detail = new JLabel(description);
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detail.setForeground(MUTED);
        row.add(detail, BorderLayout.CENTER);
        return row;
    }

    private JPanel createStatusColumn() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));

        banner.setOpaque(true);
        banner.setBackground(Color.WHITE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true), new EmptyBorder(10, 14, 10, 14)));
        bannerIcon.setOpaque(true);
        bannerIcon.setBackground(SELECTED);
        bannerIcon.setForeground(INK);
        bannerIcon.setFont(new Font("Georgia", Font.BOLD, 16));
        bannerIcon.setHorizontalAlignment(JLabel.CENTER);
        bannerIcon.setPreferredSize(new Dimension(28, 28));
        banner.add(bannerIcon, BorderLayout.WEST);
        JPanel bannerText = new JPanel();
        bannerText.setOpaque(false);
        bannerText.setLayout(new javax.swing.BoxLayout(bannerText, javax.swing.BoxLayout.Y_AXIS));
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        bannerTitle.setForeground(INK);
        bannerMessage.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        bannerMessage.setForeground(INK);
        bannerText.add(bannerTitle);
        bannerText.add(bannerMessage);
        banner.add(bannerText, BorderLayout.CENTER);
        banner.setVisible(false);

        JPanel statusCard = card();
        statusCard.setLayout(new javax.swing.BoxLayout(statusCard, javax.swing.BoxLayout.Y_AXIS));
        statusCard.add(stepHeading("2", "Room status"));
        currentStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        currentStatusLabel.setForeground(MUTED);
        currentStatusLabel.setOpaque(true);
        currentStatusLabel.setPreferredSize(new Dimension(300, 42));
        currentStatusLabel.setMinimumSize(new Dimension(0, 42));
        currentStatusLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        currentStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentStatusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ROW_LINE, 1, true),
                new EmptyBorder(7, 12, 7, 12)));
        statusCard.add(currentStatusLabel);
        statusCard.add(javax.swing.Box.createVerticalStrut(8));

        JPanel facts = new JPanel(new GridLayout(2, 2, 0, 0));
        facts.setOpaque(false);
        facts.setPreferredSize(new Dimension(300, 78));
        facts.setMinimumSize(new Dimension(0, 78));
        facts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        facts.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ROW_LINE),
                new EmptyBorder(6, 0, 0, 0)));
        facts.add(fact("Room", roomValue));
        facts.add(fact("Type", typeValue));
        facts.add(fact("Floor", floorValue));
        facts.add(fact("Occupancy", occupancyValue));
        statusCard.add(facts);
        statusCard.add(javax.swing.Box.createVerticalStrut(12));
        statusCard.add(stepHeading("3", "Choose new status"));
        statusCard.add(hint("Pick one status, then press Update."));
        optionsPanel.setOpaque(false);
        optionsPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
        addStatusOption("Clean", "Ready to be offered to guests.", HousekeepingStatus.CLEAN, GREEN);
        addStatusOption("Dirty", "Needs cleaning before the next guest.", HousekeepingStatus.DIRTY, DIRTY);
        addStatusOption("Under Maintenance", "Being repaired. Cannot be booked.",
                HousekeepingStatus.UNDER_MAINTENANCE, MAINTENANCE);
        addStatusOption("Out of Service", "Taken offline. Cannot be booked.",
                HousekeepingStatus.OUT_OF_SERVICE, OUT_OF_SERVICE);
        statusCard.add(optionsPanel);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(14, 0, 0, 0));
        cancelButton.addActionListener(event -> cancel());
        updateButton.addActionListener(event -> updateStatus());
        actions.add(cancelButton);
        actions.add(updateButton);
        statusCard.add(actions);
        alignChildrenLeft(statusCard);
        column.add(statusCard);
        return column;
    }

    private void addStatusOption(
            String name, String description, HousekeepingStatus status, Color swatchColor) {
        JPanel option = new JPanel(new BorderLayout(10, 0));
        option.setBackground(Color.WHITE);
        option.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 216, 204), 2, true),
                new EmptyBorder(10, 12, 10, 12)));
        option.setPreferredSize(new Dimension(180, 100));
        option.setMinimumSize(new Dimension(120, 90));

        JRadioButton radio = new JRadioButton();
        radio.setOpaque(false);
        radio.setBackground(Color.WHITE);
        statusGroup.add(radio);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new javax.swing.BoxLayout(copy, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel(name);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(INK);
        JLabel detail = new JLabel("<html>" + description + "</html>");
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        detail.setForeground(MUTED);
        JLabel swatch = new JLabel(name);
        swatch.setOpaque(true);
        swatch.setBackground(swatchColor);
        swatch.setForeground(status == HousekeepingStatus.CLEAN
                || status == HousekeepingStatus.DIRTY ? INK : Color.WHITE);
        swatch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        swatch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(swatchColor.darker(), 1, true),
                new EmptyBorder(2, 8, 2, 8)));
        copy.add(title);
        copy.add(detail);
        copy.add(javax.swing.Box.createVerticalStrut(4));
        copy.add(swatch);
        JPanel radioHolder = new JPanel(new BorderLayout());
        radioHolder.setOpaque(false);
        radioHolder.add(radio, BorderLayout.NORTH);
        option.add(radioHolder, BorderLayout.WEST);
        option.add(copy, BorderLayout.CENTER);

        ActionListener select = event -> {
            if (currentRoomNumber < 0) {
                return;
            }
            selectedStatus = status;
            updateButton.setEnabled(currentRoomNumber >= 0);
            resetStatusOptionStyles();
            option.setBackground(new Color(255, 249, 221));
            option.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SUN, 2, true),
                    new EmptyBorder(10, 12, 10, 12)));
            clearBanner();
        };
        radio.addActionListener(select);
        option.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (currentRoomNumber < 0) {
                    return;
                }
                radio.setSelected(true);
                select.actionPerformed(null);
            }
        });
        optionsPanel.add(option);
    }

    private JPanel sampleRooms() {
        JLabel label = new JLabel("<html>Use the room number shown in Room Management.</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(8, 0, 0, 0));
        return wrap(label);
    }

    private static JPanel wrap(Component component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void findRoom() {
        if (operationPending) {
            return;
        }
        String input = roomNumberField.getText().trim();
        if (input.isEmpty()) {
            clearCurrentRoom();
            showBanner("error", "Enter a room number.", "Type a room number before searching.");
            return;
        }
        final int number;
        try {
            number = Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            clearCurrentRoom();
            showBanner("error", "Room not found.", "Enter a numeric room number.");
            return;
        }

        setOperationPending(true);
        showBanner("info", "Loading room.", "Looking up room " + number + "…");
        StaffUi.runInBackground(
                () -> roomService.findByNumber(number),
                room -> {
                    setOperationPending(false);
                    currentRoomNumber = number;
                    currentRoom = room;
                    selectedStatus = null;
                    statusGroup.clearSelection();
                    resetStatusOptionStyles();
                    setStatusOptionsEnabled(true);
                    updateButton.setEnabled(false);
                    setCurrentStatus(RoomMonitoringStatus.fromRoom(room));
                    roomValue.setText(Integer.toString(room.getRoomNumber()));
                    typeValue.setText(room.getRoomType().getDatabaseValue());
                    floorValue.setText(Integer.toString(room.getFloor()));
                    occupancyValue.setText(room.isOccupied() ? "Occupied" : "Not occupied");
                    showBanner("info", "Room found.", "Review the current status and choose an update.");
                    revalidate();
                    repaint();
                },
                error -> {
                    setOperationPending(false);
                    clearCurrentRoom();
                    if (error instanceof RoomNotFoundException) {
                        showBanner("error", "Room not found.",
                                "No room with number " + number + " exists.");
                    } else {
                        showBanner("error", "Database unavailable.", error.getMessage());
                    }
                });
    }

    private void updateStatus() {
        if (operationPending) {
            return;
        }
        if (currentRoom == null || selectedStatus == null) {
            return;
        }
        if (currentRoom.getHousekeepingStatus() == selectedStatus) {
            showBanner("info", "No change needed.", "Room " + currentRoomNumber
                    + " is already " + displayStatus(RoomMonitoringStatus.fromRoom(currentRoom)) + ".");
            return;
        }
        if (currentRoom.isOccupied()) {
            showBanner("error", "Status not changed.",
                    "Housekeeping status cannot be changed while a guest is checked in.");
            return;
        }
        if (selectedStatus == HousekeepingStatus.UNDER_MAINTENANCE
                || selectedStatus == HousekeepingStatus.OUT_OF_SERVICE) {
            setOperationPending(true);
            showBanner("info", "Checking reservations.", "Looking for upcoming bookings…");
            StaffUi.runInBackground(
                    () -> reservationService.findReservationsAffectedByHousekeepingChange(
                            currentRoomNumber, LocalDate.now()),
                    affected -> {
                        setOperationPending(false);
                        if (!affected.isEmpty() && !confirmAffectedReservations(affected)) {
                            showBanner("info", "Status not changed.",
                                    "Move or cancel the affected reservations before changing room status.");
                            return;
                        }
                        persistStatusUpdate();
                    },
                    error -> {
                        setOperationPending(false);
                        showBanner("error", "Unable to check reservations.", error.getMessage());
                    });
        } else {
            persistStatusUpdate();
        }
    }

    private void persistStatusUpdate() {
        int roomNumber = currentRoomNumber;
        HousekeepingStatus status = selectedStatus;
        setOperationPending(true);
        showBanner("info", "Saving status.", "Updating room " + roomNumber + "…");
        StaffUi.runInBackground(() -> {
            roomService.setHousekeepingStatus(roomNumber, status);
            return roomService.findByNumber(roomNumber);
        }, room -> {
            setOperationPending(false);
            currentRoom = room;
            setCurrentStatus(RoomMonitoringStatus.fromRoom(room));
            occupancyValue.setText(room.isOccupied() ? "Occupied" : "Not occupied");
            selectedStatus = null;
            statusGroup.clearSelection();
            resetStatusOptionStyles();
            updateButton.setEnabled(false);
            onRoomChanged.run();
            showBanner("success", "Status updated.",
                    "Room " + roomNumber + " is now "
                            + displayStatus(RoomMonitoringStatus.fromRoom(room)) + ".");
        }, error -> {
            setOperationPending(false);
            if (error instanceof ValidationException) {
                showBanner("error", "Status not changed.", error.getMessage());
            } else if (error instanceof RoomNotFoundException) {
                clearCurrentRoom();
                showBanner("error", "Room no longer exists.", "Find another room and try again.");
            } else {
                showBanner("error", "Database unavailable.", error.getMessage());
            }
        });
    }

    private void setOperationPending(boolean pending) {
        operationPending = pending;
        findButton.setEnabled(!pending);
        roomNumberField.setEnabled(!pending);
        cancelButton.setEnabled(!pending);
        updateButton.setEnabled(!pending && currentRoom != null && selectedStatus != null);
        setStatusOptionsEnabled(!pending && currentRoom != null);
    }

    private boolean confirmAffectedReservations(List<Reservation> affected) {
        StringBuilder message = new StringBuilder(
                "These Pending or Confirmed reservations use this room:\n");
        for (Reservation reservation : affected) {
            message.append("\n")
                    .append(reservation.reference())
                    .append(" · ")
                    .append(reservation.guest().fullName())
                    .append(" · ")
                    .append(reservation.checkIn())
                    .append(" to ")
                    .append(reservation.checkOut());
        }
        message.append("\n\nMove or cancel them before setting this room unavailable. "
                + "Continue only if that has been handled.");
        return javax.swing.JOptionPane.showConfirmDialog(
                this, message.toString(), "Reservations use this room",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.WARNING_MESSAGE) == javax.swing.JOptionPane.OK_OPTION;
    }

    private void cancel() {
        roomNumberField.setText("");
        selectedStatus = null;
        statusGroup.clearSelection();
        updateButton.setEnabled(false);
        clearCurrentRoom();
        clearBanner();
        roomNumberField.requestFocusInWindow();
    }

    private void clearCurrentRoom() {
        currentRoomNumber = -1;
        currentRoom = null;
        selectedStatus = null;
        statusGroup.clearSelection();
        updateButton.setEnabled(false);
        setStatusOptionsEnabled(false);
        currentStatusLabel.setText("Current status   Find a room");
        currentStatusLabel.setForeground(MUTED);
        currentStatusLabel.setBackground(Color.WHITE);
        roomValue.setText("—");
        typeValue.setText("Not recorded");
        floorValue.setText("—");
        occupancyValue.setText("—");
        revalidate();
        repaint();
    }

    private void showBanner(String kind, String title, String message) {
        bannerTitle.setText(title);
        bannerMessage.setText(message);
        bannerIcon.setText(switch (kind) {
            case "error" -> "!";
            case "success" -> "✓";
            default -> "i";
        });
        Color tone = switch (kind) {
            case "error" -> new Color(255, 235, 232);
            case "success" -> new Color(232, 245, 233);
            default -> new Color(255, 249, 221);
        };
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(kind.equals("error") ? ERROR : INK, 2, true),
                new EmptyBorder(10, 14, 10, 14)));
        banner.setBackground(tone);
        banner.setVisible(true);
        bannerHolder.setVisible(true);
        revalidate();
        repaint();
    }

    private void clearBanner() {
        banner.setVisible(false);
        bannerHolder.setVisible(true);
    }

    private void setStatusOptionsEnabled(boolean enabled) {
        setEnabledRecursively(optionsPanel, enabled);
        optionsPanel.setBackground(enabled ? Color.WHITE : new Color(245, 245, 245));
    }

    private void resetStatusOptionStyles() {
        for (Component component : optionsPanel.getComponents()) {
            if (component instanceof JPanel option) {
                option.setBackground(Color.WHITE);
                option.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 216, 204), 2, true),
                        new EmptyBorder(10, 12, 10, 12)));
            }
        }
    }

    private static void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    private static String displayStatus(RoomMonitoringStatus status) {
        return status == RoomMonitoringStatus.AVAILABLE ? "Clean" : status.getDisplayName();
    }

    private void setCurrentStatus(RoomMonitoringStatus status) {
        Color statusColor = switch (status) {
            case AVAILABLE -> GREEN;
            case DIRTY -> DIRTY;
            case OCCUPIED -> INK;
            case UNDER_MAINTENANCE -> MAINTENANCE;
            case OUT_OF_SERVICE -> OUT_OF_SERVICE;
        };
        currentStatusLabel.setText("Current status   " + displayStatus(status));
        currentStatusLabel.setBackground(new Color(
                statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 35));
        currentStatusLabel.setForeground(statusColor);
    }

    /** BoxLayout centres children whose alignment differs, so every child is aligned left. */
    private static void alignChildrenLeft(JPanel parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof javax.swing.JComponent component) {
                component.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 216, 204), 1, true),
                new EmptyBorder(20, 22, 20, 22)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel stepHeading(String number, String title) {
        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        JLabel step = new JLabel(number);
        step.setOpaque(true);
        step.setBackground(SELECTED);
        step.setForeground(INK);
        step.setFont(new Font("Segoe UI", Font.BOLD, 14));
        step.setHorizontalAlignment(JLabel.CENTER);
        step.setPreferredSize(new Dimension(28, 28));
        step.setBorder(BorderFactory.createLineBorder(new Color(232, 205, 72), 1, true));
        JLabel text = new JLabel(title);
        text.setForeground(INK);
        text.setFont(new Font("Georgia", Font.BOLD, 19));
        heading.add(step, BorderLayout.WEST);
        heading.add(text, BorderLayout.CENTER);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        return heading;
    }

    private static JLabel hint(String text) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(MUTED);
        label.setBorder(new EmptyBorder(2, 38, 12, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(INK);
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JPanel fact(String label, JLabel value) {
        JPanel fact = new JPanel();
        fact.setOpaque(false);
        fact.setLayout(new javax.swing.BoxLayout(fact, javax.swing.BoxLayout.Y_AXIS));
        fact.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, ROW_LINE));
        JLabel key = new JLabel(label);
        key.setFont(new Font("Segoe UI", Font.BOLD, 12));
        key.setForeground(HEADER_TEXT);
        value.setFont(new Font("Segoe UI", Font.BOLD, 15));
        value.setForeground(INK);
        fact.add(key);
        fact.add(value);
        return fact;
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(INK);
        button.setForeground(SUN);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(INK, 2, true));
        button.setPreferredSize(new Dimension(100, 48));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }

    private static JButton outlinedButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(Color.WHITE);
        button.setForeground(INK);
        button.setFont(new Font("Segoe UI", Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(INK, 2, true));
        button.setPreferredSize(new Dimension(100, 48));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        return button;
    }
}
