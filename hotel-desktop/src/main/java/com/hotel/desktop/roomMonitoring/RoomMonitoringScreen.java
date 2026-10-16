package com.hotel.desktop.roomMonitoring;

import com.hotel.desktop.dashboard.DashboardScreen;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

/** Static room-monitoring preview with local filters and sample status changes. */
public final class RoomMonitoringScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color INK = new Color(20, 18, 26);
    private static final Color SUN = new Color(255, 204, 0);
    private static final Color CANVAS = new Color(255, 222, 62);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final Color OOS_LINE = new Color(105, 105, 105);

    private final RoomInventory roomInventory;
    private final Map<RoomInventory.Status, JLabel> countLabels = new EnumMap<>(RoomInventory.Status.class);
    private final JPanel roomRows = new JPanel();
    private RoomInventory.Status selectedFilter;

    public RoomMonitoringScreen(RoomInventory roomInventory) {
        this.roomInventory = Objects.requireNonNull(roomInventory, "roomInventory");
        setLayout(new BorderLayout());
        setBackground(CANVAS);
        setPreferredSize(new Dimension(1024, 800));
        add(createContent(), BorderLayout.CENTER);
        refreshCounts();
        rebuildRoomGrid();
    }

    public void refreshData() {
        refreshCounts();
        rebuildRoomGrid();
    }

    private JPanel createContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CANVAS);
        JPanel topLine = new JPanel();
        topLine.setBackground(SUN);
        topLine.setPreferredSize(new Dimension(1, 15));
        content.add(topLine, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 38, 40, 38));
        body.setLayout(new BorderLayout(0, 22));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(10, 66));
        JLabel title = new JLabel("Room monitoring");
        title.setFont(new Font("Georgia", Font.BOLD, 36));
        title.setForeground(INK);
        header.add(title, BorderLayout.NORTH);
        JLabel date = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)));
        date.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        date.setForeground(new Color(105, 91, 33));
        header.add(date, BorderLayout.CENTER);
        body.add(header, BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(0, 28));
        main.setOpaque(false);
        JPanel summary = new JPanel(new java.awt.GridLayout(1, RoomInventory.Status.values().length, 16, 0));
        summary.setOpaque(false);
        summary.setPreferredSize(new Dimension(10, 120));
        for (RoomInventory.Status status : RoomInventory.Status.values()) {
            summary.add(createCountCard(status));
        }
        main.add(summary, BorderLayout.NORTH);
        main.add(createRoomBoard(), BorderLayout.CENTER);
        body.add(main, BorderLayout.CENTER);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel createCountCard(RoomInventory.Status status) {
        RoundedPanel card = new RoundedPanel(4);
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 20, 4, 12));
        DashboardScreen.StatusBadge marker = new DashboardScreen.StatusBadge(statusBadgeKind(status));
        card.add(marker);

        JLabel name = new JLabel(status.getLabel());
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setForeground(INK);
        name.setBorder(new EmptyBorder(11, 0, 0, 0));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(name);

        JLabel count = new JLabel("0");
        count.setFont(new Font("Georgia", Font.BOLD, 38));
        count.setForeground(INK);
        count.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(count);
        countLabels.put(status, count);
        return card;
    }

    private static DashboardScreen.StatusBadge.Kind statusBadgeKind(RoomInventory.Status status) {
        return switch (status) {
            case AVAILABLE -> DashboardScreen.StatusBadge.Kind.AVAILABLE;
            case OCCUPIED -> DashboardScreen.StatusBadge.Kind.OCCUPIED;
            case DIRTY -> DashboardScreen.StatusBadge.Kind.CLEANING;
            case UNDER_MAINTENANCE -> DashboardScreen.StatusBadge.Kind.MAINTENANCE;
            case OUT_OF_SERVICE -> DashboardScreen.StatusBadge.Kind.OUT_OF_SERVICE;
        };
    }

    private JPanel createRoomBoard() {
        RoundedPanel board = new RoundedPanel(4);
        board.setLayout(new BorderLayout());
        board.setBorder(new EmptyBorder(12, 14, 10, 14));
        board.setPreferredSize(new Dimension(900, 420));
        board.setMinimumSize(new Dimension(500, 300));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new javax.swing.BoxLayout(header, javax.swing.BoxLayout.Y_AXIS));
        header.add(createFilters());
        header.add(javax.swing.Box.createVerticalStrut(7));
        header.add(createLegend());
        board.add(header, BorderLayout.NORTH);

        roomRows.setOpaque(false);
        roomRows.setLayout(new javax.swing.BoxLayout(roomRows, javax.swing.BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(roomRows,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        board.add(scroll, BorderLayout.CENTER);

        JLabel tip = new JLabel("Tip: click a room to change its status (Available → Occupied → Dirty → Under Maintenance → Out of Service).");
        tip.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tip.setForeground(new Color(70, 70, 70));
        tip.setBorder(new EmptyBorder(7, 7, 0, 0));
        board.add(tip, BorderLayout.SOUTH);
        return board;
    }

    private JPanel createFilters() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        filters.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        JToggleButton all = new FilterButton("All");
        all.setSelected(true);
        group.add(all);
        filters.add(all);
        all.addActionListener(event -> setFilter(null));
        for (RoomInventory.Status status : RoomInventory.Status.values()) {
            JToggleButton button = new FilterButton(status.getLabel());
            group.add(button);
            filters.add(button);
            button.addActionListener(event -> setFilter(status));
        }
        return filters;
    }

    private JPanel createLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        legend.setOpaque(false);
        for (RoomInventory.Status status : RoomInventory.Status.values()) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            item.setOpaque(false);
            item.setBorder(new EmptyBorder(0, 0, 0, 10));
            item.add(new StatusMarker(status, 12, 12));
            JLabel label = new JLabel(status.getLabel());
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            label.setForeground(INK);
            item.add(label);
            legend.add(item);
        }
        return legend;
    }

    private void setFilter(RoomInventory.Status status) {
        selectedFilter = status;
        rebuildRoomGrid();
    }

    private void rebuildRoomGrid() {
        roomRows.removeAll();
        for (int floor = 3; floor >= 1; floor--) {
            List<RoomInventory.Room> floorRooms = new java.util.ArrayList<>();
            for (RoomInventory.Room room : roomInventory.getRooms()) {
                if (room.getFloor() == floor && (selectedFilter == null || room.getStatus() == selectedFilter)) {
                    floorRooms.add(room);
                }
            }

            JPanel floorGroup = new JPanel();
            floorGroup.setOpaque(false);
            floorGroup.setLayout(new javax.swing.BoxLayout(floorGroup, javax.swing.BoxLayout.Y_AXIS));
            for (int start = 0; start < floorRooms.size(); start += 18) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 5));
                row.setOpaque(false);
                if (start == 0) {
                    FloorMarker floorMarker = new FloorMarker("F" + floor);
                    row.add(floorMarker);
                } else {
                    row.add(javax.swing.Box.createRigidArea(new Dimension(51, 43)));
                }
                int end = Math.min(start + 18, floorRooms.size());
                for (int i = start; i < end; i++) {
                    RoomInventory.Room room = floorRooms.get(i);
                    RoomTile tile = new RoomTile(room);
                    tile.addActionListener(event -> {
                        roomInventory.cycleStatus(room.getNumber());
                        refreshCounts();
                        rebuildRoomGrid();
                    });
                    row.add(tile);
                }
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
                floorGroup.add(row);
            }
            floorGroup.setMaximumSize(new Dimension(Integer.MAX_VALUE, floorGroup.getPreferredSize().height));
            roomRows.add(floorGroup);
        }
        roomRows.revalidate();
        roomRows.repaint();
    }

    private void refreshCounts() {
        for (RoomInventory.Status status : RoomInventory.Status.values()) {
            countLabels.get(status).setText(Integer.toString(roomInventory.count(status)));
        }
    }

    private static Color statusColor(RoomInventory.Status status) {
        return switch (status) {
            case AVAILABLE -> GREEN;
            case OCCUPIED, UNDER_MAINTENANCE -> INK;
            case DIRTY -> Color.WHITE;
            case OUT_OF_SERVICE -> Color.WHITE;
        };
    }

    private static void smooth(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static final class RoundedPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int radius;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            g.setColor(Color.WHITE);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.setColor(INK);
            g.setStroke(new BasicStroke(2f));
            g.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, radius, radius));
            g.dispose();
        }
    }

    private static final class StatusMarker extends JPanel {
        private static final long serialVersionUID = 1L;
        private final RoomInventory.Status status;
        private final int markerWidth;
        private final int markerHeight;

        StatusMarker(RoomInventory.Status status, int width, int height) {
            this.status = status;
            markerWidth = width;
            markerHeight = height;
            setOpaque(false);
            setPreferredSize(new Dimension(width, height));
            setMinimumSize(new Dimension(width, height));
            setMaximumSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            Shape box = new RoundRectangle2D.Float(1, 1, markerWidth - 2, markerHeight - 2, 3, 3);
            g.setColor(statusColor(status));
            g.fill(box);
            if (status == RoomInventory.Status.DIRTY || status == RoomInventory.Status.UNDER_MAINTENANCE) {
                g.clip(box);
                g.setColor(SUN);
                g.setStroke(new BasicStroke(3f));
                for (int x = -markerHeight; x < markerWidth + markerHeight; x += 7) {
                    g.drawLine(x, markerHeight, x + markerHeight, 0);
                }
            }
            if (status == RoomInventory.Status.OUT_OF_SERVICE) {
                g.setColor(OOS_LINE);
                g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        10f, new float[]{3f, 2f}, 0f));
                g.draw(box);
                g.setStroke(new BasicStroke(1.3f));
                g.draw(new Line2D.Float(markerWidth * 0.2f, markerHeight * 0.78f,
                        markerWidth * 0.8f, markerHeight * 0.22f));
            } else {
                g.setColor(INK);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(box);
            }
            g.dispose();
        }
    }

    private static final class RoomTile extends JButton {
        private static final long serialVersionUID = 1L;
        private final RoomInventory.Room room;

        RoomTile(RoomInventory.Room room) {
            this.room = room;
            setText(Integer.toString(room.getNumber()));
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(INK);
            setPreferredSize(new Dimension(44, 43));
            setMinimumSize(new Dimension(44, 43));
            setMaximumSize(new Dimension(44, 43));
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Room " + room.getNumber() + " — " + room.getStatus().getLabel()
                    + ". Click to change status.");
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            Shape tile = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10);
            Color fill = statusColor(room.getStatus());
            g.setColor(getModel().isPressed() ? fill.darker() : fill);
            g.fill(tile);
            if (room.getStatus() == RoomInventory.Status.DIRTY
                    || room.getStatus() == RoomInventory.Status.UNDER_MAINTENANCE) {
                g.clip(tile);
                g.setColor(SUN);
                g.setStroke(new BasicStroke(5f));
                for (int x = -h; x < w + h; x += 14) {
                    g.drawLine(x, h, x + h, 0);
                }
            }
            g.setClip(null);
            if (room.getStatus() == RoomInventory.Status.OUT_OF_SERVICE) {
                g.setColor(OOS_LINE);
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                        10f, new float[]{3f, 2f}, 0f));
                g.draw(tile);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Line2D.Float(7, h - 7, w - 7, 7));
            } else {
                g.setColor(INK);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(tile);
            }
            FontMetrics metrics = g.getFontMetrics(getFont());
            g.setFont(getFont());
            String text = getText();
            RoomInventory.Status status = room.getStatus();
            if (status == RoomInventory.Status.DIRTY || status == RoomInventory.Status.UNDER_MAINTENANCE) {
                // The stripes would hide the number, so it sits on a small plate.
                int chipW = metrics.stringWidth(text) + 12;
                int chipH = 20;
                RoundRectangle2D.Float chip = new RoundRectangle2D.Float(
                        (w - chipW) / 2f, (h - chipH) / 2f, chipW, chipH, 8, 8);
                g.setColor(status == RoomInventory.Status.DIRTY ? Color.WHITE : INK);
                g.fill(chip);
                g.setColor(INK);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(chip);
            }
            g.setColor(status == RoomInventory.Status.OCCUPIED
                    || status == RoomInventory.Status.UNDER_MAINTENANCE ? SUN : INK);
            g.drawString(text, (w - metrics.stringWidth(text)) / 2,
                    (h - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private static final class FilterButton extends JToggleButton {
        private static final long serialVersionUID = 1L;

        FilterButton(String text) {
            setText(text);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(INK);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            int width = Math.max(58, text.length() * 7 + 24);
            setPreferredSize(new Dimension(width, 30));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            ButtonModel model = getModel();
            boolean selected = model.isSelected();
            int w = getWidth();
            int h = getHeight();
            g.setColor(selected ? INK : model.isRollover() ? new Color(255, 243, 184) : Color.WHITE);
            g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.7f));
            g.drawRoundRect(1, 1, w - 2, h - 2, h - 2, h - 2);
            g.setFont(getFont());
            g.setColor(selected ? SUN : INK);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(getText(), (w - metrics.stringWidth(getText())) / 2,
                    (h - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    private static final class FloorMarker extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String floor;

        FloorMarker(String floor) {
            this.floor = floor;
            setOpaque(false);
            setPreferredSize(new Dimension(44, 43));
            setMinimumSize(new Dimension(44, 43));
            setMaximumSize(new Dimension(44, 43));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            g.setColor(INK);
            g.fillRoundRect(0, 1, getWidth(), getHeight() - 2, 12, 12);
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g.setColor(SUN);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(floor, (getWidth() - metrics.stringWidth(floor)) / 2,
                    (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

}
