package com.hotel.desktop.houseKeepingStatus;

import com.hotel.desktop.roomMonitoring.RoomInventory;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** UI-only housekeeping status editor backed by the shared sample room inventory. */
public final class HousekeepingScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(20, 18, 26);
    private static final Color SUN = new Color(255, 204, 0);
    private static final Color CANVAS = new Color(255, 222, 62);
    private static final Color MUTED = new Color(105, 91, 33);
    private static final Color HEADER_TEXT = new Color(74, 65, 0);
    private static final Color ROW_LINE = new Color(236, 230, 196);
    private static final Color SELECTED = new Color(255, 243, 184);
    private static final Color GREEN = new Color(0, 230, 80);
    private static final Color ERROR = new Color(179, 38, 30);

    private final RoomInventory roomInventory;
    private final JTextField roomNumberField = new JTextField();
    private final JLabel noRoomLabel = new JLabel("Find a room to see its current status.");
    private final JLabel currentStatusLabel = new JLabel(" ");
    private final JLabel roomValue = new JLabel("—");
    private final JLabel typeValue = new JLabel("Not recorded");
    private final JLabel floorValue = new JLabel("—");
    private final JLabel occupancyValue = new JLabel("—");
    private final JLabel bannerTitle = new JLabel(" ");
    private final JLabel bannerMessage = new JLabel(" ");
    private final JPanel banner = new JPanel(new BorderLayout(12, 0));
    private final JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 12, 12));
    private final ButtonGroup statusGroup = new ButtonGroup();
    private final JButton updateButton = primaryButton("Update");
    private final JButton cancelButton = outlinedButton("Cancel");
    private int currentRoomNumber = -1;
    private RoomInventory.Status selectedStatus;

    public HousekeepingScreen(RoomInventory roomInventory) {
        this.roomInventory = Objects.requireNonNull(roomInventory, "roomInventory");

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

        JPanel body = new JPanel(new BorderLayout(0, 20));
        body.setBackground(CANVAS);
        body.setBorder(new EmptyBorder(14, 40, 32, 40));
        body.add(createHeader(), BorderLayout.NORTH);
        body.add(createWorkflow(), BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Housekeeping");
        title.setFont(new Font("Georgia", Font.BOLD, 36));
        title.setForeground(INK);
        JLabel date = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));
        date.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        date.setForeground(MUTED);
        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new javax.swing.BoxLayout(labels, javax.swing.BoxLayout.Y_AXIS));
        labels.add(title);
        labels.add(date);
        header.add(labels, BorderLayout.CENTER);
        return header;
    }

    private JPanel createWorkflow() {
        JPanel columns = new JPanel(new GridLayout(1, 2, 16, 0));
        columns.setOpaque(false);
        columns.add(createLookupColumn());
        columns.add(createStatusColumn());
        return columns;
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
        roomNumberField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true), new EmptyBorder(0, 14, 0, 14)));
        roomNumberField.setPreferredSize(new Dimension(100, 48));
        roomNumberField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        roomNumberField.addActionListener(event -> findRoom());

        JButton findButton = primaryButton("Find");
        findButton.addActionListener(event -> findRoom());
        JPanel lookup = new JPanel(new BorderLayout(10, 0));
        lookup.setOpaque(false);
        lookup.add(roomNumberField, BorderLayout.CENTER);
        lookup.add(findButton, BorderLayout.EAST);
        lookup.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        findCard.add(lookup);
        findCard.add(sampleRooms());
        column.add(findCard);
        column.add(javax.swing.Box.createVerticalStrut(16));

        JPanel currentCard = card();
        currentCard.setLayout(new javax.swing.BoxLayout(currentCard, javax.swing.BoxLayout.Y_AXIS));
        currentCard.add(stepHeading("2", "Current status"));
        noRoomLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        noRoomLabel.setForeground(MUTED);
        noRoomLabel.setBorder(new EmptyBorder(10, 38, 4, 0));
        currentCard.add(noRoomLabel);
        currentStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        currentStatusLabel.setForeground(INK);
        currentStatusLabel.setBorder(new EmptyBorder(8, 38, 2, 0));
        currentCard.add(currentStatusLabel);

        JPanel facts = new JPanel(new GridLayout(2, 2, 0, 0));
        facts.setOpaque(false);
        facts.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, INK));
        facts.add(fact("Room", roomValue));
        facts.add(fact("Type", typeValue));
        facts.add(fact("Floor", floorValue));
        facts.add(fact("Occupancy", occupancyValue));
        facts.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, INK), new EmptyBorder(4, 0, 0, 0)));
        currentCard.add(facts);
        column.add(currentCard);
        return column;
    }

    private JPanel createStatusColumn() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));

        banner.setOpaque(true);
        banner.setBackground(Color.WHITE);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true), new EmptyBorder(10, 14, 10, 14)));
        JLabel bannerIcon = new JLabel("i");
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
        column.add(banner);
        column.add(javax.swing.Box.createVerticalStrut(12));

        JPanel statusCard = card();
        statusCard.setLayout(new javax.swing.BoxLayout(statusCard, javax.swing.BoxLayout.Y_AXIS));
        statusCard.add(stepHeading("3", "Choose new housekeeping status"));
        statusCard.add(hint("Pick one status, then press Update."));
        optionsPanel.setOpaque(false);
        optionsPanel.setBorder(new EmptyBorder(2, 0, 0, 0));
        addStatusOption("Clean", "Ready to be offered to guests.", RoomInventory.Status.AVAILABLE, GREEN);
        addStatusOption("Dirty", "Needs cleaning before the next guest.", RoomInventory.Status.DIRTY, SELECTED);
        addStatusOption("Under Maintenance", "Being repaired. Cannot be booked.",
                RoomInventory.Status.UNDER_MAINTENANCE, INK);
        addStatusOption("Out of Service", "Taken offline. Cannot be booked.",
                RoomInventory.Status.OUT_OF_SERVICE, Color.WHITE);
        statusCard.add(optionsPanel);

        JLabel restriction = new JLabel("<html>A room marked Occupied cannot be set to Under Maintenance "
                + "or Out of Service in this preview.</html>");
        restriction.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        restriction.setForeground(MUTED);
        restriction.setBorder(new EmptyBorder(14, 0, 0, 0));
        statusCard.add(restriction);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(14, 0, 0, 0));
        cancelButton.addActionListener(event -> cancel());
        updateButton.addActionListener(event -> updateStatus());
        actions.add(cancelButton);
        actions.add(updateButton);
        statusCard.add(actions);
        column.add(statusCard);
        return column;
    }

    private void addStatusOption(String name, String description, RoomInventory.Status status, Color swatchColor) {
        JPanel option = new JPanel(new BorderLayout(10, 0));
        option.setBackground(Color.WHITE);
        option.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true), new EmptyBorder(8, 10, 8, 10)));
        option.setPreferredSize(new Dimension(180, 94));
        option.setMinimumSize(new Dimension(120, 84));

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
        swatch.setForeground(status == RoomInventory.Status.UNDER_MAINTENANCE ? SUN : INK);
        swatch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        swatch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 1, true), new EmptyBorder(2, 8, 2, 8)));
        copy.add(title);
        copy.add(detail);
        copy.add(javax.swing.Box.createVerticalStrut(4));
        copy.add(swatch);
        option.add(radio, BorderLayout.WEST);
        option.add(copy, BorderLayout.CENTER);

        ActionListener select = event -> {
            if (currentRoomNumber < 0) {
                return;
            }
            selectedStatus = status;
            updateButton.setEnabled(currentRoomNumber >= 0);
            option.setBackground(SELECTED);
            for (Component component : optionsPanel.getComponents()) {
                if (component instanceof JPanel other && other != option) {
                    other.setBackground(Color.WHITE);
                }
            }
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
        List<RoomInventory.Room> rooms = roomInventory.getRooms();
        StringBuilder samples = new StringBuilder("<html><b>Sample rooms:</b> ");
        int count = Math.min(6, rooms.size());
        for (int index = 0; index < count; index++) {
            if (index > 0) {
                samples.append(" &nbsp; ");
            }
            samples.append(rooms.get(index).getNumber());
        }
        samples.append("</html>");
        JLabel label = new JLabel(samples.toString());
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

        RoomInventory.Room room = findRoomInInventory(number);
        if (room == null) {
            clearCurrentRoom();
            showBanner("error", "Room not found.", "No room with number " + number + " exists.");
            return;
        }

        currentRoomNumber = number;
        selectedStatus = null;
        statusGroup.clearSelection();
        setStatusOptionsEnabled(true);
        updateButton.setEnabled(false);
        noRoomLabel.setVisible(false);
        RoomInventory.Status status = room.getStatus();
        currentStatusLabel.setText("Current status: " + displayStatus(status));
        roomValue.setText(Integer.toString(room.getNumber()));
        floorValue.setText(Integer.toString(room.getFloor()));
        occupancyValue.setText(status == RoomInventory.Status.OCCUPIED ? "Occupied" : "Not occupied");
        showBanner("info", "Room found.", "Review the current status and choose an update.");
        revalidate();
        repaint();
    }

    private RoomInventory.Room findRoomInInventory(int number) {
        for (RoomInventory.Room room : roomInventory.getRooms()) {
            if (room.getNumber() == number) {
                return room;
            }
        }
        return null;
    }

    private void updateStatus() {
        if (currentRoomNumber < 0 || selectedStatus == null) {
            return;
        }
        RoomInventory.Room room = findRoomInInventory(currentRoomNumber);
        if (room == null) {
            clearCurrentRoom();
            showBanner("error", "Room no longer exists.", "Find another room and try again.");
            return;
        }
        if (room.getStatus() == selectedStatus) {
            showBanner("info", "No change needed.", "Room " + currentRoomNumber
                    + " is already " + displayStatus(room.getStatus()) + ".");
            return;
        }
        if (room.getStatus() == RoomInventory.Status.OCCUPIED
                && (selectedStatus == RoomInventory.Status.UNDER_MAINTENANCE
                || selectedStatus == RoomInventory.Status.OUT_OF_SERVICE)) {
            showBanner("error", "Status not changed.",
                    "An occupied room cannot be set to Under Maintenance or Out of Service.");
            return;
        }

        roomInventory.setStatus(currentRoomNumber, selectedStatus);
        room = findRoomInInventory(currentRoomNumber);
        currentStatusLabel.setText("Current status: " + displayStatus(room.getStatus()));
        occupancyValue.setText(room.getStatus() == RoomInventory.Status.OCCUPIED ? "Occupied" : "Not occupied");
        selectedStatus = null;
        statusGroup.clearSelection();
        updateButton.setEnabled(false);
        showBanner("success", "Status updated.",
                "Room " + currentRoomNumber + " is now " + displayStatus(room.getStatus()) + ".");
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
        selectedStatus = null;
        statusGroup.clearSelection();
        updateButton.setEnabled(false);
        setStatusOptionsEnabled(false);
        noRoomLabel.setVisible(true);
        currentStatusLabel.setText(" ");
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
        Color tone = switch (kind) {
            case "error" -> ERROR;
            case "success" -> GREEN;
            default -> SELECTED;
        };
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(kind.equals("error") ? ERROR : INK, 2, true),
                new EmptyBorder(10, 14, 10, 14)));
        banner.setBackground(tone);
        banner.setVisible(true);
        revalidate();
        repaint();
    }

    private void clearBanner() {
        banner.setVisible(false);
    }

    private void setStatusOptionsEnabled(boolean enabled) {
        setEnabledRecursively(optionsPanel, enabled);
        optionsPanel.setBackground(enabled ? Color.WHITE : new Color(245, 245, 245));
    }

    private static void setEnabledRecursively(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setEnabledRecursively(child, enabled);
            }
        }
    }

    private static String displayStatus(RoomInventory.Status status) {
        return status == RoomInventory.Status.AVAILABLE ? "Clean" : status.getLabel();
    }

    private static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true), new EmptyBorder(18, 20, 18, 20)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel stepHeading(String number, String title) {
        JPanel heading = new JPanel(new BorderLayout(10, 0));
        heading.setOpaque(false);
        JLabel step = new JLabel(number);
        step.setOpaque(true);
        step.setBackground(INK);
        step.setForeground(SUN);
        step.setFont(new Font("Segoe UI", Font.BOLD, 14));
        step.setHorizontalAlignment(JLabel.CENTER);
        step.setPreferredSize(new Dimension(28, 28));
        step.setBorder(BorderFactory.createLineBorder(INK, 1, true));
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
