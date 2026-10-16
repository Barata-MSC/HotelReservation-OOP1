package com.hotel.desktop.roomMonitoring;

import com.hotel.core.model.RoomMonitoringSnapshot;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.RoomMonitoringStatus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ButtonModel;

final class RoomMonitoringComponents {

    static final Color INK = new Color(20, 18, 26);
    static final Color SUN = new Color(255, 204, 0);
    static final Color GREEN = new Color(34, 197, 94);
    static final Color OOS_LINE = new Color(105, 105, 105);

    private RoomMonitoringComponents() {
    }

    static Color statusColor(RoomMonitoringStatus status) {
        return switch (status) {
            case AVAILABLE -> GREEN;
            case OCCUPIED, UNDER_MAINTENANCE -> INK;
            case DIRTY, OUT_OF_SERVICE -> Color.WHITE;
        };
    }

    static void smooth(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private static void drawStatusSymbol(
            Graphics2D graphics,
            RoomMonitoringStatus status,
            float x,
            float y,
            float size) {
        if (status == RoomMonitoringStatus.AVAILABLE) {
            graphics.setColor(INK);
            graphics.setStroke(new BasicStroke(Math.max(1.1f, size / 6f),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(new Line2D.Float(
                    x, y + size * 0.55f,
                    x + size * 0.35f, y + size * 0.9f));
            graphics.draw(new Line2D.Float(
                    x + size * 0.35f, y + size * 0.9f,
                    x + size, y + size * 0.1f));
        } else if (status == RoomMonitoringStatus.OCCUPIED) {
            graphics.setColor(SUN);
            graphics.setStroke(new BasicStroke(Math.max(1.1f, size / 6f)));
            graphics.drawOval(Math.round(x), Math.round(y), Math.round(size), Math.round(size));
        }
    }

    static final class RoundedPanel extends JPanel {

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

    static final class StatusMarker extends JPanel {

        private static final long serialVersionUID = 1L;
        private final RoomMonitoringStatus status;
        private final int markerWidth;
        private final int markerHeight;

        StatusMarker(RoomMonitoringStatus status, int width, int height) {
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
            if (status == RoomMonitoringStatus.DIRTY
                    || status == RoomMonitoringStatus.UNDER_MAINTENANCE) {
                g.clip(box);
                g.setColor(SUN);
                g.setStroke(new BasicStroke(3f));
                for (int x = -markerHeight; x < markerWidth + markerHeight; x += 7) {
                    if (status == RoomMonitoringStatus.DIRTY) {
                        g.drawLine(x, markerHeight, x + markerHeight, 0);
                    } else {
                        g.drawLine(x, 0, x + markerHeight, markerHeight);
                    }
                }
            }
            g.setClip(null);
            if (status == RoomMonitoringStatus.OUT_OF_SERVICE) {
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
            if (status == RoomMonitoringStatus.AVAILABLE || status == RoomMonitoringStatus.OCCUPIED) {
                float symbolSize = Math.max(5, Math.min(markerWidth, markerHeight) - 5);
                drawStatusSymbol(g, status,
                        (markerWidth - symbolSize) / 2f,
                        (markerHeight - symbolSize) / 2f,
                        symbolSize);
            }
            g.dispose();
        }
    }

    static final class RoomTile extends JButton {

        private static final long serialVersionUID = 1L;
        private final RoomMonitoringSnapshot.MonitoredRoom monitoredRoom;
        private final boolean selected;

        RoomTile(RoomMonitoringSnapshot.MonitoredRoom monitoredRoom, boolean selected, Runnable onSelect) {
            this.monitoredRoom = monitoredRoom;
            this.selected = selected;
            Room room = monitoredRoom.getRoom();
            setText(Integer.toString(room.getRoomNumber()));
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
            setToolTipText("Room " + room.getRoomNumber() + " — "
                    + monitoredRoom.getStatus().getDisplayName() + ", floor " + room.getFloor()
                    + ", " + room.getRoomType().getDatabaseValue());
            getAccessibleContext().setAccessibleDescription(getToolTipText());
            addActionListener(event -> onSelect.run());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            smooth(g);
            int w = getWidth();
            int h = getHeight();
            Shape tile = new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10);
            RoomMonitoringStatus status = monitoredRoom.getStatus();
            Color fill = statusColor(status);
            g.setColor(getModel().isPressed() ? fill.darker() : fill);
            g.fill(tile);
            if (status == RoomMonitoringStatus.DIRTY
                    || status == RoomMonitoringStatus.UNDER_MAINTENANCE) {
                g.clip(tile);
                g.setColor(SUN);
                g.setStroke(new BasicStroke(5f));
                for (int x = -h; x < w + h; x += 14) {
                    if (status == RoomMonitoringStatus.DIRTY) {
                        g.drawLine(x, h, x + h, 0);
                    } else {
                        g.drawLine(x, 0, x + h, h);
                    }
                }
            }
            g.setClip(null);
            g.setColor(status == RoomMonitoringStatus.OUT_OF_SERVICE ? OOS_LINE : INK);
            g.setStroke(status == RoomMonitoringStatus.OUT_OF_SERVICE
                    ? new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                            10f, new float[]{3f, 2f}, 0f)
                    : new BasicStroke(1.5f));
            g.draw(tile);
            if (status == RoomMonitoringStatus.OUT_OF_SERVICE) {
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Line2D.Float(7, h - 7, w - 7, 7));
            }
            if (selected) {
                g.setColor(SUN);
                g.setStroke(new BasicStroke(3f));
                g.draw(new RoundRectangle2D.Float(2.5f, 2.5f, w - 5, h - 5, 9, 9));
            }
            if (status == RoomMonitoringStatus.AVAILABLE || status == RoomMonitoringStatus.OCCUPIED) {
                drawStatusSymbol(g, status, w - 12, 4, 7);
            }

            FontMetrics metrics = g.getFontMetrics(getFont());
            String text = getText();
            if (status == RoomMonitoringStatus.DIRTY || status == RoomMonitoringStatus.UNDER_MAINTENANCE) {
                int chipW = metrics.stringWidth(text) + 12;
                int chipH = 20;
                RoundRectangle2D.Float chip = new RoundRectangle2D.Float(
                        (w - chipW) / 2f, (h - chipH) / 2f, chipW, chipH, 8, 8);
                g.setColor(status == RoomMonitoringStatus.DIRTY ? Color.WHITE : INK);
                g.fill(chip);
                g.setColor(INK);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(chip);
            }
            g.setFont(getFont());
            g.setColor(status == RoomMonitoringStatus.OCCUPIED
                    || status == RoomMonitoringStatus.UNDER_MAINTENANCE ? SUN : INK);
            g.drawString(text, (w - metrics.stringWidth(text)) / 2,
                    (h - metrics.getHeight()) / 2 + metrics.getAscent());
            g.dispose();
        }
    }

    static final class FilterButton extends JToggleButton {

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
            setPreferredSize(new Dimension(Math.max(58, text.length() * 7 + 24), 30));
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

    static final class FloorMarker extends JPanel {

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
