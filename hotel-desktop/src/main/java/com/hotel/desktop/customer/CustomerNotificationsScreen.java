package com.hotel.desktop.customer;

import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CustomerNotification;
import com.hotel.core.service.CustomerReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

public final class CustomerNotificationsScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final CustomerReservationService service;
    private final Runnable onUnreadCountChanged;
    private final List<CustomerNotification> notifications = new ArrayList<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Date", "Notification", "Reference", "Read"}, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final javax.swing.JLabel feedback = StaffUi.feedbackLabel();
    private final JButton markRead = StaffUi.secondaryButton("Mark as read");
    private AuthenticatedUser customer;

    public CustomerNotificationsScreen(
            CustomerReservationService service, Runnable onUnreadCountChanged) {
        this.service = java.util.Objects.requireNonNull(service);
        this.onUnreadCountChanged = java.util.Objects.requireNonNull(onUnreadCountChanged);
        JPanel body = CustomerUi.createPage(this, "Notifications", "Updates about your bookings and payments");
        JPanel card = CustomerUi.card();
        StaffUi.styleTable(table);
        table.setRowHeight(52);
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(0).setPreferredWidth(190);
        columns.getColumn(1).setPreferredWidth(590);
        columns.getColumn(2).setPreferredWidth(300);
        columns.getColumn(3).setPreferredWidth(110);
        DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(
                        source, value, selected, focus, row, column);
                CustomerNotification notification = notificationAtViewRow(row);
                cell.setFont(notification != null && !notification.read()
                        ? source.getFont().deriveFont(java.awt.Font.BOLD) : source.getFont());
                return cell;
            }
        };
        for (int column = 2; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(rowRenderer);
        }
        columns.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(
                        source, value, selected, focus, row, column);
                CustomerNotification notification = notificationAtViewRow(row);
                boolean unread = notification != null && !notification.read();
                cell.setFont(unread ? source.getFont().deriveFont(java.awt.Font.BOLD) : source.getFont());
                cell.setIcon(new EnvelopeIcon(unread));
                cell.setIconTextGap(8);
                cell.setToolTipText(unread ? "Unread notification" : "Read notification");
                return cell;
            }
        });
        columns.getColumn(1).setCellRenderer((source, value, selected, focus, row, column) -> {
            JTextArea message = new JTextArea(value == null ? "" : value.toString());
            message.setLineWrap(true);
            message.setWrapStyleWord(true);
            message.setFont(source.getFont());
            CustomerNotification notification = notificationAtViewRow(row);
            if (notification != null && !notification.read()) {
                message.setFont(source.getFont().deriveFont(java.awt.Font.BOLD));
            }
            message.setForeground(selected ? source.getSelectionForeground() : source.getForeground());
            message.setBackground(selected ? source.getSelectionBackground()
                    : (row % 2 == 0 ? Color.WHITE : new Color(250, 248, 238)));
            message.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 5, 10, 5));
            return message;
        });
        table.getSelectionModel().addListSelectionListener(event -> updateMarkRead());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && javax.swing.SwingUtilities.isLeftMouseButton(event)) {
                    int row = table.rowAtPoint(event.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        showNotificationDetails(notificationAtViewRow(row));
                    }
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        CustomerUi.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton retry = StaffUi.secondaryButton("Retry");
        retry.addActionListener(event -> refresh());
        markRead.addActionListener(event -> markSelectedRead());
        actions.add(markRead);
        actions.add(retry);
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(feedback, BorderLayout.CENTER);
        footer.add(actions, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        body.add(card, BorderLayout.CENTER);
    }

    public void setCustomer(AuthenticatedUser customer) {
        this.customer = customer;
        refresh();
    }

    public void refresh() {
        if (customer == null) {
            return;
        }
        model.setRowCount(0);
        StaffUi.showFeedback(feedback, "Loading notifications…", false);
        new SwingWorker<List<CustomerNotification>, Void>() {
            @Override
            protected List<CustomerNotification> doInBackground() throws SQLException {
                return service.findNotifications(customer);
            }

            @Override
            protected void done() {
                try {
                    notifications.clear();
                    notifications.addAll(get());
                    model.setRowCount(0);
                    for (CustomerNotification notification : notifications) {
                        model.addRow(new Object[]{
                                DATE_TIME.format(notification.createdAt()),
                                notification.message(),
                                notification.reservationReference() == null
                                        ? "—" : notification.reservationReference(),
                                notification.read() ? "Read" : "Unread"});
                    }
                    resizeMessageRows();
                    StaffUi.showFeedback(feedback, notifications.isEmpty()
                            ? "You have no notifications." : notifications.size() + " notification(s).", false);
                    updateMarkRead();
                    onUnreadCountChanged.run();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Loading notifications was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    showError("Unable to load notifications: " + exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void resizeMessageRows() {
        int messageColumn = 1;
        int width = table.getColumnModel().getColumn(messageColumn).getWidth() - 10;
        for (int row = 0; row < table.getRowCount(); row++) {
            Object value = table.getValueAt(row, messageColumn);
            JTextArea renderer = new JTextArea(value == null ? "" : value.toString());
            renderer.setLineWrap(true);
            renderer.setWrapStyleWord(true);
            renderer.setFont(table.getFont());
            renderer.setSize(Math.max(1, width), Short.MAX_VALUE);
            int height = Math.max(table.getRowHeight(), renderer.getPreferredSize().height + 4);
            table.setRowHeight(row, height);
        }
    }

    private CustomerNotification notificationAtViewRow(int viewRow) {
        if (viewRow < 0 || viewRow >= table.getRowCount()) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return modelRow < 0 || modelRow >= notifications.size() ? null : notifications.get(modelRow);
    }

    private void showNotificationDetails(CustomerNotification notification) {
        if (notification == null) {
            return;
        }
        JPanel details = new JPanel(new BorderLayout(0, 10));
        details.setPreferredSize(new java.awt.Dimension(440, 190));
        JLabel heading = new JLabel(formatType(notification.type()));
        heading.setFont(StaffUi.headingFont(20));
        heading.setForeground(StaffUi.INK);
        JTextArea message = new JTextArea(notification.message());
        message.setFont(StaffUi.BODY_FONT);
        message.setForeground(StaffUi.INK);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setEditable(false);
        message.setOpaque(false);
        JLabel metadata = new JLabel("Received " + DATE_TIME.format(notification.createdAt())
                + "  ·  " + (notification.read() ? "Read" : "Unread")
                + (notification.reservationReference() == null ? ""
                        : "  ·  Reservation " + notification.reservationReference()));
        metadata.setFont(StaffUi.bodyFont(12));
        metadata.setForeground(StaffUi.MUTED);
        details.add(heading, BorderLayout.NORTH);
        details.add(new JScrollPane(message), BorderLayout.CENTER);
        details.add(metadata, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, details, "Notification details", JOptionPane.INFORMATION_MESSAGE);
    }

    private static String formatType(String type) {
        if (type == null || type.isBlank()) {
            return "Notification";
        }
        String spaced = type.replaceAll("([a-z])([A-Z])", "$1 $2");
        return spaced.replace("Payment", "payment").replace("Reservation", "reservation");
    }

    private static final class EnvelopeIcon implements javax.swing.Icon {
        private final boolean unread;

        private EnvelopeIcon(boolean unread) {
            this.unread = unread;
        }

        @Override
        public int getIconWidth() {
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(unread ? StaffUi.INK : StaffUi.MUTED);
            g.drawRoundRect(x + 1, y + 2, 15, 10, 2, 2);
            g.drawLine(x + 2, y + 3, x + 9, y + 8);
            g.drawLine(x + 9, y + 8, x + 16, y + 3);
            if (unread) {
                g.fillOval(x + 13, y, 5, 5);
            }
            g.dispose();
        }
    }

    private void markSelectedRead() {
        CustomerNotification notification = selectedNotification();
        if (notification == null || notification.read()) {
            return;
        }
        markRead.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws SQLException {
                service.markNotificationRead(customer, notification.id());
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    refresh();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Updating the notification was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    showError("Unable to update notification: " + exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    private CustomerNotification selectedNotification() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(selected);
        return modelRow < 0 || modelRow >= notifications.size()
                ? null : notifications.get(modelRow);
    }

    private void updateMarkRead() {
        CustomerNotification selected = selectedNotification();
        markRead.setEnabled(selected != null && !selected.read());
    }

    private void showError(String message) {
        StaffUi.showFeedback(feedback, message, true);
    }
}
