package com.hotel.desktop.roomMonitoring;

import com.hotel.core.model.Room;
import com.hotel.core.model.RoomMonitoringSnapshot;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.service.RoomService;
import com.hotel.core.model.enums.RoomMonitoringStatus;
import com.hotel.core.model.enums.RoomType;
import com.hotel.desktop.roomMonitoring.RoomMonitoringComponents.FilterButton;
import com.hotel.desktop.roomMonitoring.RoomMonitoringComponents.FloorMarker;
import com.hotel.desktop.roomMonitoring.RoomMonitoringComponents.RoomTile;
import com.hotel.desktop.roomMonitoring.RoomMonitoringComponents.RoundedPanel;
import com.hotel.desktop.roomMonitoring.RoomMonitoringComponents.StatusMarker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.EmptyBorder;

import static com.hotel.desktop.roomMonitoring.RoomMonitoringComponents.INK;
import static com.hotel.desktop.roomMonitoring.RoomMonitoringComponents.SUN;

/** Read-only room status dashboard hosted inside the staff workspace. */
public final class RoomMonitoringScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color CANVAS = new Color(255, 222, 62);
    private static final Color MUTED = new Color(105, 91, 33);

    private final RoomMonitoringDataSource dataSource;
    private final RoomService roomService;
    private final JPanel roomRows = new JPanel();
    private final JPanel typeRows = new JPanel();
    private final Map<RoomMonitoringStatus, JLabel> countLabels =
            new EnumMap<>(RoomMonitoringStatus.class);
    private final Map<RoomMonitoringStatus, JPanel> attentionLists =
            new EnumMap<>(RoomMonitoringStatus.class);
    private final Map<RoomMonitoringStatus, JToggleButton> filterButtons =
            new EnumMap<>(RoomMonitoringStatus.class);
    private final JToggleButton allFilterButton = new FilterButton("All");
    private final JButton refreshButton = primaryButton("Refresh");
    private final JLabel lastUpdatedLabel = new JLabel("Last updated —");
    private final JLabel totalRoomsLabel = new JLabel("0 rooms");
    private final JLabel selectedRoomHint = bodyLabel("Select a room tile to see its details.");
    private final JLabel selectedRoomNumberValue = valueLabel();
    private final JLabel selectedFloor = valueLabel();
    private final JLabel selectedType = valueLabel();
    private final JLabel selectedCapacity = valueLabel();
    private final JLabel selectedPrice = valueLabel();
    private final JLabel selectedAvailability = valueLabel();
    private final JLabel selectedHousekeeping = valueLabel();
    private final JTextField searchField = new JTextField();
    private final JComboBox<String> roomTypeFilter = new JComboBox<>();
    private final JScrollPane roomScroll;
    private RoomMonitoringSnapshot snapshot;
    private RoomMonitoringStatus selectedFilter;
    private Integer selectedRoomNumber;
    private int tilesPerRow = -1;

    public RoomMonitoringScreen(RoomMonitoringDataSource dataSource, RoomService roomService) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.roomService = Objects.requireNonNull(roomService, "roomService");
        setLayout(new BorderLayout());
        setBackground(CANVAS);
        setPreferredSize(new Dimension(1024, 800));

        roomRows.setOpaque(false);
        roomRows.setLayout(new javax.swing.BoxLayout(roomRows, javax.swing.BoxLayout.Y_AXIS));
        roomScroll = new JScrollPane(
                roomRows,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        roomScroll.setOpaque(false);
        roomScroll.getViewport().setOpaque(false);
        roomScroll.setBorder(BorderFactory.createEmptyBorder());
        roomScroll.getVerticalScrollBar().setUnitIncrement(18);
        roomScroll.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                rebuildRoomGrid();
            }
        });

        add(createContent(), BorderLayout.CENTER);
        dataSource.addListener(this::displayDataState);
    }

    public void refreshData() {
        dataSource.refresh();
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CANVAS);
        JPanel topLine = new JPanel();
        topLine.setBackground(SUN);
        topLine.setPreferredSize(new Dimension(1, 12));
        content.add(topLine, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(12, 16, 14, 16));
        body.add(createHeader(), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);
        main.add(createSummaryRow(), BorderLayout.NORTH);
        main.add(createMainArea(), BorderLayout.CENTER);
        body.add(main, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new javax.swing.BoxLayout(titleBlock, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Room monitoring");
        title.setFont(new Font("Georgia", Font.BOLD, 30));
        title.setForeground(INK);
        JLabel date = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));
        date.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        date.setForeground(MUTED);
        titleBlock.add(title);
        titleBlock.add(date);
        header.add(titleBlock, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        actions.setOpaque(false);
        lastUpdatedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lastUpdatedLabel.setForeground(INK);
        totalRoomsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        totalRoomsLabel.setForeground(INK);
        refreshButton.addActionListener(event -> refreshData());
        actions.add(lastUpdatedLabel);
        actions.add(refreshButton);
        actions.add(totalRoomsLabel);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel createSummaryRow() {
        JPanel summary = new JPanel(new GridLayout(1, RoomMonitoringStatus.values().length, 8, 0));
        summary.setOpaque(false);
        for (RoomMonitoringStatus status : RoomMonitoringStatus.values()) {
            JButton card = createSummaryCard(status);
            summary.add(card);
        }
        return summary;
    }

    private JButton createSummaryCard(RoomMonitoringStatus status) {
        JButton card = new JButton();
        card.setLayout(new BorderLayout(4, 2));
        card.setBackground(Color.WHITE);
        card.setOpaque(true);
        card.setBorder(BorderFactory.createLineBorder(INK, 2));
        card.setFocusPainted(false);
        card.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        JLabel title = new JLabel(status.getDisplayName(), SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(INK);
        JLabel count = new JLabel("…", SwingConstants.CENTER);
        count.setFont(new Font("Georgia", Font.BOLD, 25));
        count.setForeground(INK);
        card.add(title, BorderLayout.NORTH);
        card.add(count, BorderLayout.CENTER);
        countLabels.put(status, count);
        card.getAccessibleContext().setAccessibleName(status.getDisplayName() + " rooms");
        card.addActionListener(event -> setFilter(status));
        card.setPreferredSize(new Dimension(120, 66));
        return card;
    }

    private JPanel createMainArea() {
        JPanel main = new JPanel(new BorderLayout(12, 0));
        main.setOpaque(false);
        main.add(createRoomBoard(), BorderLayout.CENTER);

        JPanel side = createSidePanel();
        side.setPreferredSize(new Dimension(280, 100));
        side.setMinimumSize(new Dimension(260, 100));
        JScrollPane sideScroll = new JScrollPane(
                side,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sideScroll.setPreferredSize(new Dimension(280, 100));
        sideScroll.setOpaque(false);
        sideScroll.getViewport().setOpaque(false);
        sideScroll.setBorder(BorderFactory.createEmptyBorder());
        sideScroll.getVerticalScrollBar().setUnitIncrement(16);
        main.add(sideScroll, BorderLayout.EAST);
        return main;
    }

    private JPanel createRoomBoard() {
        RoundedPanel board = new RoundedPanel(8);
        board.setLayout(new BorderLayout(0, 7));
        board.setBorder(new EmptyBorder(9, 10, 8, 10));
        board.setMinimumSize(new Dimension(360, 250));
        board.add(createBoardControls(), BorderLayout.NORTH);
        board.add(roomScroll, BorderLayout.CENTER);
        return board;
    }

    private JPanel createBoardControls() {
        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new javax.swing.BoxLayout(controls, javax.swing.BoxLayout.Y_AXIS));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filters.setOpaque(false);
        ButtonGroup filterGroup = new ButtonGroup();
        allFilterButton.setSelected(true);
        filterGroup.add(allFilterButton);
        allFilterButton.addActionListener(event -> setFilter(null));
        filters.add(allFilterButton);
        for (RoomMonitoringStatus status : RoomMonitoringStatus.values()) {
            JToggleButton button = new FilterButton(status.getDisplayName());
            filterGroup.add(button);
            filterButtons.put(status, button);
            button.addActionListener(event -> setFilter(status));
            filters.add(button);
        }
        controls.add(filters);

        JPanel selectors = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        selectors.setOpaque(false);
        JLabel searchLabel = bodyLabel("Room no.");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchField.setToolTipText("Filter by room number");
        searchField.setPreferredSize(new Dimension(100, 27));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                rebuildRoomGrid();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                rebuildRoomGrid();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                rebuildRoomGrid();
            }
        });
        selectors.add(searchLabel);
        selectors.add(searchField);
        JLabel typeLabel = bodyLabel("Type");
        roomTypeFilter.addItem("All room types");
        for (RoomType type : RoomType.values()) {
            roomTypeFilter.addItem(type.getDatabaseValue());
        }
        roomTypeFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roomTypeFilter.addActionListener(event -> rebuildRoomGrid());
        selectors.add(typeLabel);
        selectors.add(roomTypeFilter);
        controls.add(selectors);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        legend.setOpaque(false);
        for (RoomMonitoringStatus status : RoomMonitoringStatus.values()) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            item.setOpaque(false);
            item.setBorder(new EmptyBorder(0, 0, 0, 4));
            item.add(new StatusMarker(status, 12, 12));
            JLabel label = bodyLabel(status.getDisplayName());
            item.add(label);
            legend.add(item);
        }
        controls.add(legend);
        return controls;
    }

    private JPanel createSidePanel() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new javax.swing.BoxLayout(side, javax.swing.BoxLayout.Y_AXIS));
        side.add(createAvailableByTypeCard());
        side.add(javax.swing.Box.createVerticalStrut(9));
        side.add(createAttentionCard());
        side.add(javax.swing.Box.createVerticalStrut(9));
        side.add(createSelectedRoomCard());
        return side;
    }

    private JPanel createAvailableByTypeCard() {
        RoundedPanel card = sideCard();
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        card.add(cardTitle("Available by room type"));
        card.add(javax.swing.Box.createVerticalStrut(6));
        typeRows.setOpaque(false);
        typeRows.setLayout(new javax.swing.BoxLayout(typeRows, javax.swing.BoxLayout.Y_AXIS));
        for (RoomType type : RoomType.values()) {
            JPanel row = new JPanel(new BorderLayout(5, 0));
            row.setOpaque(false);
            JLabel name = bodyLabel(type.getDatabaseValue());
            JLabel count = bodyLabel("… / …");
            count.setFont(new Font("Segoe UI", Font.BOLD, 12));
            row.add(name, BorderLayout.WEST);
            row.add(count, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
            typeRows.add(row);
        }
        card.add(typeRows);
        return card;
    }

    private JPanel createAttentionCard() {
        RoundedPanel card = sideCard();
        card.setLayout(new BorderLayout(0, 5));
        card.add(cardTitle("Needs attention"), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        addAttentionTab(tabs, RoomMonitoringStatus.DIRTY, "Dirty");
        addAttentionTab(tabs, RoomMonitoringStatus.UNDER_MAINTENANCE, "Maintenance");
        addAttentionTab(tabs, RoomMonitoringStatus.OUT_OF_SERVICE, "Out of Service");
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        card.add(tabs, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(280, 185));
        return card;
    }

    private void addAttentionTab(JTabbedPane tabs, RoomMonitoringStatus status, String title) {
        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));
        attentionLists.put(status, list);
        JScrollPane scroll = new JScrollPane(
                list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        tabs.addTab(title, scroll);
    }

    private JPanel createSelectedRoomCard() {
        RoundedPanel card = sideCard();
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        card.add(cardTitle("Selected room"));
        selectedRoomHint.setBorder(new EmptyBorder(6, 0, 5, 0));
        card.add(selectedRoomHint);
        card.add(detailRow("Room no.", selectedRoomNumberValue));
        card.add(detailRow("Floor", selectedFloor));
        card.add(detailRow("Type", selectedType));
        card.add(detailRow("Capacity", selectedCapacity));
        card.add(detailRow("Price per day", selectedPrice));
        card.add(detailRow("Availability", selectedAvailability));
        card.add(detailRow("Housekeeping", selectedHousekeeping));
        return card;
    }

    private void displayDataState(RoomMonitoringDataSource.State state) {
        refreshButton.setEnabled(!state.isLoading());
        if (state.isLoading() && state.getLastUpdated() != null) {
            lastUpdatedLabel.setText("Refreshing · last updated "
                    + state.getLastUpdated().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else if (state.getLastUpdated() != null) {
            lastUpdatedLabel.setText("Last updated "
                    + state.getLastUpdated().format(DateTimeFormatter.ofPattern("HH:mm")));
        } else if (state.isLoading()) {
            lastUpdatedLabel.setText("Loading rooms…");
        } else if (state.getError() != null) {
            lastUpdatedLabel.setText("Not updated");
        }

        snapshot = state.getSnapshot();
        if (state.getError() != null && snapshot == null) {
            totalRoomsLabel.setText("Room data unavailable");
            showLoadError(state.getError());
            clearSnapshotDetails();
            return;
        }
        if (snapshot == null) {
            totalRoomsLabel.setText("Loading…");
            showLoading();
            return;
        }

        totalRoomsLabel.setText(snapshot.getTotalRoomCount()
                + (snapshot.getTotalRoomCount() == 1 ? " room" : " rooms"));
        for (RoomMonitoringStatus status : RoomMonitoringStatus.values()) {
            countLabels.get(status).setText(Integer.toString(snapshot.getCount(status)));
        }
        updateTypeCounts();
        updateAttentionLists();
        if (selectedRoomNumber != null && findRoom(selectedRoomNumber) == null) {
            selectedRoomNumber = null;
        }
        updateSelectedRoom();
        rebuildRoomGrid();
    }

    private void showLoading() {
        roomRows.removeAll();
        JLabel message = bodyLabel("Loading room data…");
        message.setBorder(new EmptyBorder(18, 8, 0, 0));
        roomRows.add(message);
        roomRows.revalidate();
        roomRows.repaint();
    }

    private void showLoadError(Throwable error) {
        roomRows.removeAll();
        JPanel message = new JPanel();
        message.setOpaque(false);
        message.setLayout(new javax.swing.BoxLayout(message, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = bodyLabel("Unable to load room data from the database.");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel detail = bodyLabel("Check the database connection and try again.");
        detail.setToolTipText(error.getMessage());
        JButton retry = primaryButton("Retry");
        retry.addActionListener(event -> refreshData());
        message.add(title);
        message.add(javax.swing.Box.createVerticalStrut(5));
        message.add(detail);
        message.add(javax.swing.Box.createVerticalStrut(9));
        message.add(retry);
        message.setBorder(new EmptyBorder(18, 8, 0, 0));
        roomRows.add(message);
        roomRows.revalidate();
        roomRows.repaint();
    }

    private void clearSnapshotDetails() {
        for (JLabel label : countLabels.values()) {
            label.setText("—");
        }
        typeRows.removeAll();
        for (JPanel list : attentionLists.values()) {
            list.removeAll();
            list.revalidate();
            list.repaint();
        }
        selectedRoomNumber = null;
        updateSelectedRoom();
        typeRows.revalidate();
        typeRows.repaint();
    }

    private void updateTypeCounts() {
        typeRows.removeAll();
        for (RoomType type : RoomType.values()) {
            RoomMonitoringSnapshot.RoomTypeAvailability availability =
                    snapshot.getAvailabilityByRoomType().get(type);
            JPanel row = new JPanel(new BorderLayout(5, 0));
            row.setOpaque(false);
            row.add(bodyLabel(type.getDatabaseValue()), BorderLayout.WEST);
            JLabel count = bodyLabel(availability.getAvailableCount() + " / " + availability.getTotalCount());
            count.setFont(new Font("Segoe UI", Font.BOLD, 12));
            row.add(count, BorderLayout.EAST);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 23));
            typeRows.add(row);
        }
        typeRows.revalidate();
        typeRows.repaint();
    }

    private void updateAttentionLists() {
        for (RoomMonitoringStatus status : List.of(
                RoomMonitoringStatus.DIRTY,
                RoomMonitoringStatus.UNDER_MAINTENANCE,
                RoomMonitoringStatus.OUT_OF_SERVICE)) {
            JPanel list = attentionLists.get(status);
            list.removeAll();
            List<RoomMonitoringSnapshot.MonitoredRoom> rooms = new ArrayList<>();
            for (RoomMonitoringSnapshot.MonitoredRoom room : snapshot.getRooms()) {
                if (room.getStatus() == status) {
                    rooms.add(room);
                }
            }
            rooms.sort(Comparator.comparingInt(room -> room.getRoom().getRoomNumber()));
            if (rooms.isEmpty()) {
                list.add(bodyLabel("None"));
            } else {
                for (RoomMonitoringSnapshot.MonitoredRoom monitoredRoom : rooms) {
                    Room room = monitoredRoom.getRoom();
                    JLabel label = bodyLabel("Room " + room.getRoomNumber() + " · F" + room.getFloor()
                            + " · " + room.getRoomType().getDatabaseValue());
                    label.setBorder(new EmptyBorder(3, 4, 3, 2));
                    list.add(label);
                }
            }
            list.revalidate();
            list.repaint();
        }
    }

    private void rebuildRoomGrid() {
        roomRows.removeAll();
        if (snapshot == null) {
            return;
        }

        List<RoomMonitoringSnapshot.MonitoredRoom> filteredRooms = new ArrayList<>();
        String query = searchField.getText().trim();
        String selectedTypeName = (String) roomTypeFilter.getSelectedItem();
        for (RoomMonitoringSnapshot.MonitoredRoom monitoredRoom : snapshot.getRooms()) {
            Room room = monitoredRoom.getRoom();
            if (selectedFilter != null && monitoredRoom.getStatus() != selectedFilter) {
                continue;
            }
            if (!query.isEmpty() && !Integer.toString(room.getRoomNumber()).contains(query)) {
                continue;
            }
            if (selectedTypeName != null && !"All room types".equals(selectedTypeName)
                    && !room.getRoomType().getDatabaseValue().equals(selectedTypeName)) {
                continue;
            }
            filteredRooms.add(monitoredRoom);
        }
        filteredRooms.sort(Comparator
                .comparingInt((RoomMonitoringSnapshot.MonitoredRoom room) -> room.getRoom().getFloor())
                .reversed()
                .thenComparingInt(room -> room.getRoom().getRoomNumber()));

        if (snapshot.getTotalRoomCount() == 0) {
            addBoardMessage("There are no rooms in the database.");
            return;
        }
        if (filteredRooms.isEmpty()) {
            addBoardMessage("No rooms match the selected filters.");
            return;
        }

        int currentTilesPerRow = calculateTilesPerRow();
        if (tilesPerRow != currentTilesPerRow) {
            tilesPerRow = currentTilesPerRow;
        }
        Set<Integer> floors = new TreeSet<>(Comparator.reverseOrder());
        for (RoomMonitoringSnapshot.MonitoredRoom room : filteredRooms) {
            floors.add(room.getRoom().getFloor());
        }
        for (int floor : floors) {
            List<RoomMonitoringSnapshot.MonitoredRoom> floorRooms = new ArrayList<>();
            for (RoomMonitoringSnapshot.MonitoredRoom room : filteredRooms) {
                if (room.getRoom().getFloor() == floor) {
                    floorRooms.add(room);
                }
            }
            JPanel floorGroup = new JPanel();
            floorGroup.setOpaque(false);
            floorGroup.setLayout(new javax.swing.BoxLayout(floorGroup, javax.swing.BoxLayout.Y_AXIS));
            for (int start = 0; start < floorRooms.size(); start += tilesPerRow) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 4));
                row.setOpaque(false);
                if (start == 0) {
                    row.add(new FloorMarker("F" + floor));
                } else {
                    row.add(javax.swing.Box.createRigidArea(new Dimension(44, 43)));
                }
                int end = Math.min(start + tilesPerRow, floorRooms.size());
                for (int index = start; index < end; index++) {
                    RoomMonitoringSnapshot.MonitoredRoom room = floorRooms.get(index);
                    boolean selected = Objects.equals(selectedRoomNumber, room.getRoom().getRoomNumber());
                    RoomTile tile = new RoomTile(room, selected,
                            () -> selectRoom(room.getRoom().getRoomNumber()));
                    tile.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent event) {
                            showRoomOverrideMenu(tile, room, event);
                        }

                        @Override
                        public void mouseReleased(MouseEvent event) {
                            showRoomOverrideMenu(tile, room, event);
                        }
                    });
                    row.add(tile);
                }
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                floorGroup.add(row);
            }
            floorGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, floorGroup.getPreferredSize().height));
            floorGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
            roomRows.add(floorGroup);
        }
        roomRows.revalidate();
        roomRows.repaint();
    }

    private void showRoomOverrideMenu(
            RoomTile tile, RoomMonitoringSnapshot.MonitoredRoom room, MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem available = new JMenuItem("Mark Available (Clean)");
        available.addActionListener(action -> updateRoomStatus(
                room.getRoom().getRoomNumber(), HousekeepingStatus.CLEAN));
        JMenuItem maintenance = new JMenuItem("Mark Under Maintenance");
        maintenance.addActionListener(action -> updateRoomStatus(
                room.getRoom().getRoomNumber(), HousekeepingStatus.UNDER_MAINTENANCE));
        menu.add(available);
        menu.add(maintenance);
        menu.show(tile, event.getX(), event.getY());
    }

    private void updateRoomStatus(int roomNumber, HousekeepingStatus status) {
        try {
            roomService.setHousekeepingStatus(roomNumber, status);
            dataSource.refresh();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Room status was not changed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int calculateTilesPerRow() {
        int viewportWidth = roomScroll.getViewport().getWidth();
        if (viewportWidth <= 0) {
            return 6;
        }
        return Math.max(1, (viewportWidth - 53) / 50);
    }

    private void addBoardMessage(String text) {
        JLabel message = bodyLabel(text);
        message.setBorder(new EmptyBorder(18, 8, 0, 0));
        roomRows.add(message);
        roomRows.revalidate();
        roomRows.repaint();
    }

    private void selectRoom(int roomNumber) {
        selectedRoomNumber = roomNumber;
        updateSelectedRoom();
        rebuildRoomGrid();
    }

    private void updateSelectedRoom() {
        RoomMonitoringSnapshot.MonitoredRoom monitoredRoom =
                selectedRoomNumber == null ? null : findRoom(selectedRoomNumber);
        boolean selected = monitoredRoom != null;
        selectedRoomHint.setVisible(!selected);
        selectedRoomNumberValue.setText(
                selected ? Integer.toString(monitoredRoom.getRoom().getRoomNumber()) : "—");
        selectedFloor.setText(selected ? Integer.toString(monitoredRoom.getRoom().getFloor()) : "—");
        selectedType.setText(selected ? monitoredRoom.getRoom().getRoomType().getDatabaseValue() : "—");
        selectedCapacity.setText(selected ? Integer.toString(monitoredRoom.getRoom().getCapacity()) : "—");
        selectedPrice.setText(selected ? formatPrice(monitoredRoom.getRoom().getPricePerDay()) : "—");
        selectedAvailability.setText(selected ? monitoredRoom.getStatus().getDisplayName() : "—");
        selectedHousekeeping.setText(selected
                ? monitoredRoom.getRoom().getHousekeepingStatus().getDatabaseValue()
                : "—");
    }

    private RoomMonitoringSnapshot.MonitoredRoom findRoom(int roomNumber) {
        if (snapshot == null) {
            return null;
        }
        for (RoomMonitoringSnapshot.MonitoredRoom room : snapshot.getRooms()) {
            if (room.getRoom().getRoomNumber() == roomNumber) {
                return room;
            }
        }
        return null;
    }

    private void setFilter(RoomMonitoringStatus status) {
        selectedFilter = status;
        allFilterButton.setSelected(status == null);
        for (Map.Entry<RoomMonitoringStatus, JToggleButton> entry : filterButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey() == status);
        }
        rebuildRoomGrid();
    }

    private static RoundedPanel sideCard() {
        RoundedPanel card = new RoundedPanel(7);
        card.setBorder(new EmptyBorder(9, 10, 9, 10));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private static JPanel detailRow(String name, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        JLabel label = bodyLabel(name);
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return row;
    }

    private static JLabel cardTitle(String title) {
        JLabel label = new JLabel(title);
        label.setFont(new Font("Georgia", Font.BOLD, 16));
        label.setForeground(INK);
        return label;
    }

    private static JLabel bodyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(INK);
        return label;
    }

    private static JLabel valueLabel() {
        JLabel label = bodyLabel("—");
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(SUN);
        button.setBackground(INK);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(INK, 2));
        button.setPreferredSize(new Dimension(82, 30));
        return button;
    }

    private static String formatPrice(BigDecimal price) {
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
        return "PHP " + formatter.format(price);
    }
}
