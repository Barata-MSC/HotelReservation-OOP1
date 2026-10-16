package com.hotel.desktop.roomManagement;

import com.hotel.core.exception.RoomNotFoundException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.service.RoomService;

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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/** Room management workflow preview hosted inside the staff application's screen deck. */
public class RoomManagementScreen extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color INK = new Color(20, 18, 26);
    private static final Color SUN = new Color(255, 204, 0);
    private static final Color CANVAS = new Color(255, 222, 62);
    private static final Color MUTED = new Color(105, 91, 33);
    private static final Color LINE = new Color(236, 230, 196);

    private final JPanel workArea = new JPanel(new BorderLayout(0, 16));
    private final RoomService roomService;
    private final Runnable onRoomChanged;
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> statusFilter = new JComboBox<>(
            new String[]{"All statuses", "Available", "Occupied", "Dirty", "Under Maintenance", "Out of Service"});
    private final JTable roomTable = new JTable();
    private final JLabel roomCount = new JLabel("0 rooms");
    private final DefaultTableModel roomModel;
    private final TableRowSorter<DefaultTableModel> roomSorter;
    private final JLabel feedback = new JLabel("Room data is loaded from the database.");

    public RoomManagementScreen() {
        this(new RoomService(), () -> { });
    }

    public RoomManagementScreen(RoomService roomService, Runnable onRoomChanged) {
        this.roomService = Objects.requireNonNull(roomService, "roomService");
        this.onRoomChanged = Objects.requireNonNull(onRoomChanged, "onRoomChanged");
        setLayout(new BorderLayout());
        setBackground(CANVAS);
        setPreferredSize(new Dimension(1024, 800));

        roomModel = new DefaultTableModel(
                new Object[0][7],
                new String[]{"Room", "Floor", "Type", "Capacity", "Nightly rate", "Status", "Housekeeping"}) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        roomTable.setModel(roomModel);
        roomSorter = new TableRowSorter<>(roomModel);
        roomTable.setRowSorter(roomSorter);
        add(buildContent(), BorderLayout.CENTER);
        styleTable();
        installFilters();
        showInventory();
        loadRooms();
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CANVAS);
        JPanel topStrip = new JPanel();
        topStrip.setBackground(SUN);
        topStrip.setPreferredSize(new Dimension(10, 15));
        content.add(topStrip, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(14, 38, 40, 38));
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Room management");
        title.setFont(new Font("Georgia", Font.BOLD, 36));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Add, update, search, and display hotel rooms");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(MUTED);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(3));
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.CENTER);
        body.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(buildActionSelector(), BorderLayout.NORTH);
        workArea.setOpaque(false);
        center.add(workArea, BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildActionSelector() {
        JPanel actions = new JPanel(new GridBagLayout());
        actions.setOpaque(false);
        actions.setPreferredSize(new Dimension(10, 84));
        addActionCard(actions, 0, "Add room", "Enter details for a new room", () -> showForm(false));
        addActionCard(actions, 1, "Update room", "Select a room and edit its details", () -> {
            int selected = roomTable.getSelectedRow();
            if (selected < 0) {
                feedback.setText("Select a room in the table before choosing Update room.");
                showInventory();
                return;
            }
            showForm(true);
        });
        addActionCard(actions, 2, "Search / display", "Find and review rooms in the inventory", this::loadRooms);
        return actions;
    }

    private void addActionCard(JPanel parent, int column, String label, String description, Runnable action) {
        JButton button = new JButton("<html><b>" + label + "</b><br><font size='3'>" + description + "</font></html>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setForeground(INK);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(11, 16, 11, 12)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> action.run());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, column == 0 ? 0 : 8, 0, column == 2 ? 0 : 8);
        parent.add(button, constraints);
    }

    private JPanel buildInventory() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        panel.add(buildSearchBar(), BorderLayout.NORTH);
        panel.add(buildRoomTableCard(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSearchBar() {
        JPanel search = new JPanel(new BorderLayout(12, 0));
        search.setOpaque(false);
        JPanel inputWrap = new JPanel(new BorderLayout(10, 0));
        inputWrap.setBackground(Color.WHITE);
        inputWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(7, 13, 7, 9)));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        searchField.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        searchField.setToolTipText("Search by room number, floor, or room type");
        inputWrap.add(searchField, BorderLayout.CENTER);
        JButton searchButton = primaryButton("Search");
        searchButton.addActionListener(event -> applyFilters());
        inputWrap.add(searchButton, BorderLayout.EAST);
        search.add(inputWrap, BorderLayout.CENTER);

        statusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusFilter.setBackground(Color.WHITE);
        statusFilter.setPreferredSize(new Dimension(185, 44));
        statusFilter.addActionListener(event -> applyFilters());
        search.add(statusFilter, BorderLayout.EAST);
        return search;
    }

    private JPanel buildRoomTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(18, 20, 14, 20)));
        JPanel tableHeading = new JPanel(new BorderLayout());
        tableHeading.setOpaque(false);
        JLabel heading = new JLabel("Room inventory");
        heading.setFont(new Font("Georgia", Font.BOLD, 21));
        heading.setForeground(INK);
        tableHeading.add(heading, BorderLayout.WEST);
        JLabel sampleTag = new JLabel("Database inventory");
        sampleTag.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sampleTag.setForeground(MUTED);
        tableHeading.add(sampleTag, BorderLayout.EAST);
        tableHeading.setBorder(BorderFactory.createEmptyBorder(0, 2, 15, 2));
        card.add(tableHeading, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(roomTable);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, INK));
        scroll.getViewport().setBackground(Color.WHITE);
        card.add(scroll, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 2));
        feedback.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        feedback.setForeground(MUTED);
        footer.add(feedback, BorderLayout.WEST);
        roomCount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        roomCount.setForeground(INK);
        footer.add(roomCount, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildRoomForm(boolean update) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        JPanel headingCard = new JPanel(new BorderLayout());
        headingCard.setBackground(Color.WHITE);
        headingCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(16, 20, 16, 20)));
        JLabel title = new JLabel(update ? "Update room" : "Add room");
        title.setFont(new Font("Georgia", Font.BOLD, 21));
        title.setForeground(INK);
        headingCard.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel("Changes are saved to the database");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        hint.setForeground(MUTED);
        headingCard.add(hint, BorderLayout.EAST);
        panel.add(headingCard, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(24, 28, 24, 28)));
        String[] labels = {"Room number", "Floor", "Room type", "Capacity", "Nightly rate", "Room status", "Housekeeping"};
        JTextField roomNumber = new JTextField(18);
        JTextField floor = new JTextField(18);
        JComboBox<String> type = new JComboBox<>(new String[]{"Single", "Double", "Deluxe"});
        JTextField capacity = new JTextField(18);
        JTextField rate = new JTextField(18);
        JComboBox<String> status = new JComboBox<>(new String[]{"Available", "Occupied", "Dirty", "Under Maintenance", "Out of Service"});
        JComboBox<String> housekeeping = new JComboBox<>(new String[]{"Clean", "Dirty"});
        Component[] fields = {roomNumber, floor, type, capacity, rate, status, housekeeping};
        if (update) {
            int row = roomTable.convertRowIndexToModel(roomTable.getSelectedRow());
            roomNumber.setText(String.valueOf(roomTable.getModel().getValueAt(row, 0)));
            floor.setText(String.valueOf(roomTable.getModel().getValueAt(row, 1)));
            type.setSelectedItem(roomTable.getModel().getValueAt(row, 2));
            capacity.setText(String.valueOf(roomTable.getModel().getValueAt(row, 3)));
            rate.setText(String.valueOf(roomTable.getModel().getValueAt(row, 4)).replace("PHP ", "").replace(",", ""));
            status.setSelectedItem(roomTable.getModel().getValueAt(row, 5));
            housekeeping.setSelectedItem(roomTable.getModel().getValueAt(row, 6));
            roomNumber.setEditable(false);
        }
        for (int row = 0; row < labels.length; row++) {
            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = row;
            labelConstraints.anchor = GridBagConstraints.WEST;
            labelConstraints.insets = new Insets(0, 0, 14, 24);
            JLabel label = new JLabel(labels[row]);
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            label.setForeground(INK);
            form.add(label, labelConstraints);
            GridBagConstraints fieldConstraints = new GridBagConstraints();
            fieldConstraints.gridx = 1;
            fieldConstraints.gridy = row;
            fieldConstraints.weightx = 1;
            fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
            fieldConstraints.insets = new Insets(0, 0, 14, 0);
            styleField(fields[row]);
            form.add(fields[row], fieldConstraints);
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = labels.length;
        filler.gridwidth = 2;
        filler.weighty = 1;
        form.add(Box.createVerticalGlue(), filler);
        panel.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        JButton cancel = secondaryButton("Back to room list");
        cancel.addActionListener(event -> showInventory());
        JButton save = primaryButton(update ? "Save changes" : "Add room");
        save.addActionListener(event -> {
            saveRoom(update, roomNumber, floor, type, capacity, rate, status, housekeeping);
        });
        footer.add(cancel);
        footer.add(save);
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private void styleField(Component component) {
        component.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        component.setPreferredSize(new Dimension(320, 40));
        if (component instanceof JTextField field) {
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(135, 130, 145), 1, true),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        }
    }

    private void styleTable() {
        roomTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roomTable.setForeground(INK);
        roomTable.setRowHeight(48);
        roomTable.setFillsViewportHeight(true);
        roomTable.setSelectionBackground(new Color(255, 243, 184));
        roomTable.setSelectionForeground(INK);
        roomTable.setGridColor(LINE);
        roomTable.setShowVerticalLines(false);
        roomTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        roomTable.getTableHeader().setForeground(INK);
        roomTable.getTableHeader().setBackground(new Color(250, 248, 238));
        roomTable.getTableHeader().setPreferredSize(new Dimension(10, 40));
        roomTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    }

    private void installFilters() {
        searchField.addActionListener(event -> loadRooms());
    }

    private void applyFilters() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String status = String.valueOf(statusFilter.getSelectedItem());
        roomSorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean matchesQuery = query.isEmpty();
                for (int column = 0; !matchesQuery && column < entry.getValueCount(); column++) {
                    Object value = entry.getValue(column);
                    matchesQuery = value != null
                            && value.toString().toLowerCase(Locale.ROOT).contains(query);
                }
                boolean matchesStatus = "All statuses".equals(status) || status.equals(entry.getStringValue(5));
                return matchesQuery && matchesStatus;
            }
        });
    }

    private void loadRooms() {
        String query = searchField.getText().trim();
        try {
            List<Room> rooms;
            if (query.isEmpty()) {
                rooms = roomService.findAll();
            } else {
                Integer roomNumber = parseRoomNumber(query);
                RoomType roomType = parseRoomType(query);
                if (roomNumber != null) {
                    rooms = roomService.search(roomNumber, null);
                    if (rooms.isEmpty()) {
                        rooms = roomService.findAll();
                    }
                } else if (roomType != null) {
                    rooms = roomService.search(null, roomType);
                } else {
                    rooms = roomService.findAll();
                }
            }

            roomModel.setRowCount(0);
            for (Room room : rooms) {
                roomModel.addRow(new Object[]{
                    room.getRoomNumber(),
                    room.getFloor(),
                    room.getRoomType().getDatabaseValue(),
                    room.getCapacity(),
                    formatPrice(room.getPricePerDay()),
                    roomStatus(room),
                    room.getHousekeepingStatus().getDatabaseValue()
                });
            }
            roomCount.setText(rooms.size() + (rooms.size() == 1 ? " room" : " rooms"));
            feedback.setText("Room data is loaded from the database.");
            applyFilters();
        } catch (SQLException e) {
            feedback.setText("Unable to load room data.");
            showDatabaseError(e);
        }
        showInventory();
    }

    private void saveRoom(
            boolean update,
            JTextField roomNumberField,
            JTextField floorField,
            JComboBox<String> typeField,
            JTextField capacityField,
            JTextField rateField,
            JComboBox<String> statusField,
            JComboBox<String> housekeepingField) {
        try {
            int roomNumber = Integer.parseInt(roomNumberField.getText().trim());
            int floor = Integer.parseInt(floorField.getText().trim());
            int capacity = Integer.parseInt(capacityField.getText().trim());
            RoomType roomType = parseRoomType(String.valueOf(typeField.getSelectedItem()));
            BigDecimal price = new BigDecimal(rateField.getText().trim().replace(",", ""));
            String selectedStatus = String.valueOf(statusField.getSelectedItem());
            HousekeepingStatus housekeepingStatus = parseHousekeepingStatus(
                    selectedStatus, String.valueOf(housekeepingField.getSelectedItem()));
            String description = update
                    ? roomService.findByNumber(roomNumber).getDescription()
                    : roomType.getDatabaseValue() + " room";
            Room room = new Room(
                    roomNumber,
                    floor,
                    roomType,
                    capacity,
                    description,
                    price,
                    "Occupied".equals(selectedStatus),
                    housekeepingStatus);

            if (update) {
                roomService.updateRoom(room);
                feedback.setText("Room updated.");
            } else {
                roomService.addRoom(room);
                feedback.setText("Room added.");
            }
            onRoomChanged.run();
            loadRooms();
        } catch (NumberFormatException e) {
            showValidationError("Enter whole numbers for room number, floor, and capacity, and a valid price.");
        } catch (ValidationException e) {
            showValidationError(e.getMessage());
        } catch (RoomNotFoundException e) {
            showValidationError(e.getMessage());
        } catch (SQLException e) {
            feedback.setText("Unable to save room data.");
            showDatabaseError(e);
        }
    }

    private static Integer parseRoomNumber(String query) {
        try {
            return Integer.valueOf(query);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static RoomType parseRoomType(String value) {
        for (RoomType roomType : RoomType.values()) {
            if (roomType.getDatabaseValue().equalsIgnoreCase(value.trim())) {
                return roomType;
            }
        }
        return null;
    }

    private static HousekeepingStatus parseHousekeepingStatus(String roomStatus, String housekeeping) {
        return switch (roomStatus) {
            case "Dirty" -> HousekeepingStatus.DIRTY;
            case "Under Maintenance" -> HousekeepingStatus.UNDER_MAINTENANCE;
            case "Out of Service" -> HousekeepingStatus.OUT_OF_SERVICE;
            default -> HousekeepingStatus.valueOf(housekeeping.toUpperCase(Locale.ROOT));
        };
    }

    private static String roomStatus(Room room) {
        if (room.getHousekeepingStatus() == HousekeepingStatus.UNDER_MAINTENANCE
                || room.getHousekeepingStatus() == HousekeepingStatus.OUT_OF_SERVICE) {
            return room.getHousekeepingStatus().getDatabaseValue();
        }
        if (room.getHousekeepingStatus() == HousekeepingStatus.DIRTY) {
            return HousekeepingStatus.DIRTY.getDatabaseValue();
        }
        return room.isOccupied() ? "Occupied" : "Available";
    }

    private static String formatPrice(BigDecimal price) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(2);
        return "PHP " + formatter.format(price);
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid room details", JOptionPane.WARNING_MESSAGE);
    }

    private void showDatabaseError(SQLException error) {
        String sqlState = error.getSQLState();
        String detail = error.getMessage();
        boolean connectionFailure = (sqlState != null && sqlState.startsWith("08"))
                || (detail != null && (detail.startsWith("Missing classpath configuration")
                || detail.startsWith("Unable to load database configuration")
                || detail.startsWith("Missing required database property")));
        String message = connectionFailure
                ? "Unable to connect to the hotel database."
                : "The database could not complete the room operation.";
        JOptionPane.showMessageDialog(
                this,
                message + "\n" + error.getMessage(),
                connectionFailure ? "Database unavailable" : "Database error",
                JOptionPane.ERROR_MESSAGE);
    }

    private void showInventory() {
        workArea.removeAll();
        workArea.add(buildInventory(), BorderLayout.CENTER);
        workArea.revalidate();
        workArea.repaint();
    }

    private void showForm(boolean update) {
        workArea.removeAll();
        workArea.add(buildRoomForm(update), BorderLayout.CENTER);
        workArea.revalidate();
        workArea.repaint();
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(SUN);
        button.setBackground(INK);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(INK);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INK, 2, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

}
