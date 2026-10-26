package com.hotel.desktop.customer;

import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.CustomerBookingRequest;
import com.hotel.core.model.CustomerReceipt;
import com.hotel.core.service.CustomerReservationService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.Map;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public final class CustomerWorkspacePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final CustomerReservationService service;
    private final Runnable onLogout;
    private final JPanel screens = new JPanel(new CardLayout());
    private final JLabel customerName = new JLabel("Customer");
    private final JButton notificationButton = StaffUi.secondaryButton("Notifications");
    private final Map<CustomerNavItem, JButton> navigationButtons = new EnumMap<>(CustomerNavItem.class);
    private final CustomerDashboardScreen dashboardScreen;
    private final CustomerBookingScreen bookingScreen;
    private final CustomerPaymentScreen paymentScreen;
    private final CustomerBookingsScreen bookingsScreen;
    private final CustomerNotificationsScreen notificationsScreen;
    private final CustomerReceiptScreen receiptScreen;
    private AuthenticatedUser customer;

    public CustomerWorkspacePanel(CustomerReservationService service, Runnable onLogout) {
        this.service = java.util.Objects.requireNonNull(service);
        this.onLogout = java.util.Objects.requireNonNull(onLogout);
        setLayout(new BorderLayout());
        setBackground(CustomerUi.CANVAS);

        dashboardScreen = new CustomerDashboardScreen(
                () -> showPage(CustomerNavItem.BOOK_ROOM, true));
        paymentScreen = new CustomerPaymentScreen(service, this::showReceipt);
        bookingScreen = new CustomerBookingScreen(service, this::continueFromBooking);
        bookingsScreen = new CustomerBookingsScreen(
                service, this::editBooking, this::payBalance,
                this::loadReceipt, this::refreshUnreadCount);
        notificationsScreen = new CustomerNotificationsScreen(service, this::refreshUnreadCount);
        receiptScreen = new CustomerReceiptScreen(
                () -> showPage(CustomerNavItem.MY_BOOKINGS, true));

        screens.add(dashboardScreen, CustomerNavItem.DASHBOARD.name());
        screens.add(bookingScreen, CustomerNavItem.BOOK_ROOM.name());
        screens.add(bookingsScreen, CustomerNavItem.MY_BOOKINGS.name());
        screens.add(notificationsScreen, CustomerNavItem.NOTIFICATIONS.name());
        screens.add(paymentScreen, CustomerNavItem.PAYMENT.name());
        screens.add(receiptScreen, CustomerNavItem.RECEIPT.name());
        add(buildSidebar(), BorderLayout.WEST);
        JScrollPane pageScroll = new JScrollPane(
                screens,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setBorder(null);
        pageScroll.setMinimumSize(new Dimension(0, 0));
        pageScroll.getVerticalScrollBar().setUnitIncrement(18);
        add(pageScroll, BorderLayout.CENTER);
        showPage(CustomerNavItem.DASHBOARD, false);
    }

    public void setCustomer(AuthenticatedUser customer) {
        if (customer == null || customer.getRole() != com.hotel.core.model.UserRole.CUSTOMER) {
            throw new IllegalArgumentException("A customer account is required for this workspace.");
        }
        this.customer = customer;
        customerName.setText("<html><b>" + escape(customer.getFullName())
                + "</b><br><font color='#C6CCD5'>Customer</font></html>");
        customerName.setForeground(java.awt.Color.WHITE);
        customerName.setToolTipText(customer.getFullName());
        paymentScreen.setCustomer(customer);
        bookingScreen.setCustomer(customer);
        bookingsScreen.setCustomer(customer);
        notificationsScreen.setCustomer(customer);
        refreshUnreadCount();
        showPage(CustomerNavItem.DASHBOARD, false);
    }

    public void clearCustomer() {
        customer = null;
        customerName.setText("Customer");
        customerName.setToolTipText(null);
        notificationButton.setText("Notifications");
        notificationButton.setToolTipText(null);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setBackground(new java.awt.Color(24, 27, 35));
        sidebar.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 3, CustomerUi.LINE),
                new EmptyBorder(10, 18, 10, 18)));
        sidebar.setPreferredSize(new Dimension(256, 1));
        JLabel brand = new JLabel("<html><b>OKADA</b><br>HOTEL</html>");
        brand.setFont(StaffUi.headingFont(28));
        brand.setForeground(java.awt.Color.WHITE);
        brand.setBorder(new EmptyBorder(0, 4, 0, 0));

        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 6));
        menu.setOpaque(false);
        menu.add(navButton("Dashboard", CustomerNavItem.DASHBOARD));
        menu.add(navButton("Book a room", CustomerNavItem.BOOK_ROOM));
        menu.add(navButton("My bookings", CustomerNavItem.MY_BOOKINGS));
        notificationButton.setHorizontalAlignment(JButton.LEFT);
        notificationButton.setPreferredSize(new Dimension(214, 44));
        notificationButton.setForeground(java.awt.Color.WHITE);
        notificationButton.setBackground(new java.awt.Color(38, 42, 52));
        notificationButton.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(65, 70, 82)),
                new EmptyBorder(9, 14, 9, 14)));
        notificationButton.setFocusPainted(false);
        notificationButton.addActionListener(event -> showPage(CustomerNavItem.NOTIFICATIONS, true));
        navigationButtons.put(CustomerNavItem.NOTIFICATIONS, notificationButton);
        menu.add(notificationButton);

        JPanel accountRow = new JPanel(new BorderLayout(8, 0));
        accountRow.setOpaque(false);
        accountRow.setBorder(new EmptyBorder(4, 0, 58, 0));
        customerName.setForeground(java.awt.Color.WHITE);
        customerName.setFont(StaffUi.bodyFont(12));
        customerName.setVerticalAlignment(javax.swing.SwingConstants.CENTER);
        customerName.setToolTipText("Customer account");
        accountRow.add(customerName, BorderLayout.CENTER);
        JButton logout = StaffUi.secondaryButton("Log out");
        logout.setFont(StaffUi.bodyFont(12).deriveFont(java.awt.Font.BOLD));
        logout.setPreferredSize(new Dimension(82, 38));
        logout.setMinimumSize(new Dimension(82, 38));
        logout.addActionListener(event -> onLogout.run());
        accountRow.add(logout, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(brand, BorderLayout.NORTH);
        top.add(menu, BorderLayout.CENTER);

        sidebar.add(top, BorderLayout.NORTH);
        sidebar.add(accountRow, BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton navButton(String text, CustomerNavItem item) {
        JButton button = new JButton(text);
        button.setFont(StaffUi.bodyFont(14).deriveFont(java.awt.Font.BOLD));
        button.setForeground(java.awt.Color.WHITE);
        button.setBackground(new java.awt.Color(38, 42, 52));
        button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(new java.awt.Color(65, 70, 82)),
                new EmptyBorder(9, 14, 9, 14)));
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(JButton.LEFT);
        button.setPreferredSize(new Dimension(214, 38));
        button.addActionListener(event -> showPage(item, true));
        navigationButtons.put(item, button);
        return button;
    }

    private void showPage(CustomerNavItem item, boolean refresh) {
        if (item == CustomerNavItem.BOOK_ROOM && refresh) {
            bookingScreen.startNewBooking();
        } else if (item == CustomerNavItem.MY_BOOKINGS && refresh) {
            bookingsScreen.refresh();
        } else if (item == CustomerNavItem.NOTIFICATIONS && refresh) {
            notificationsScreen.refresh();
        }
        ((CardLayout) screens.getLayout()).show(screens, item.name());
        updateNavigationSelection(item);
        if (refresh && item != CustomerNavItem.NOTIFICATIONS) {
            refreshUnreadCount();
        }
    }

    private void updateNavigationSelection(CustomerNavItem selectedItem) {
        navigationButtons.forEach((item, button) -> {
            boolean selected = item == selectedItem;
            button.setBackground(selected
                    ? CustomerUi.ACCENT : new java.awt.Color(38, 42, 52));
            button.setForeground(selected ? CustomerUi.INK : java.awt.Color.WHITE);
            button.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(selected
                            ? CustomerUi.ACCENT : new java.awt.Color(65, 70, 82)),
                    new EmptyBorder(9, 14, 9, 14)));
        });
    }

    private void continueFromBooking(String changeReference, CustomerBookingRequest request) {
        if (changeReference == null) {
            paymentScreen.prepareBooking(request);
            showPage(CustomerNavItem.PAYMENT, false);
            return;
        }
        new SwingWorker<CustomerReceipt, Void>() {
            @Override
            protected CustomerReceipt doInBackground() throws Exception {
                service.requestChange(customer, changeReference, request);
                return service.findReceipt(customer, changeReference);
            }

            @Override
            protected void done() {
                try {
                    showReceipt(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showChangeError("Saving the reservation change was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    showChangeError("Unable to change the reservation: "
                            + exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void editBooking(CustomerBooking booking) {
        bookingScreen.prepareChange(booking);
        showPage(CustomerNavItem.BOOK_ROOM, false);
    }

    private void payBalance(CustomerBooking booking) {
        paymentScreen.prepareBalancePayment(booking);
        showPage(CustomerNavItem.PAYMENT, false);
    }

    private void loadReceipt(CustomerBooking booking) {
        new SwingWorker<CustomerReceipt, Void>() {
            @Override
            protected CustomerReceipt doInBackground() throws SQLException {
                return service.findReceipt(customer, booking.reservation().reference());
            }

            @Override
            protected void done() {
                try {
                    showReceipt(get());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showChangeError("Loading the receipt was interrupted.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    showChangeError("Unable to load the receipt: " + exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void showReceipt(CustomerReceipt receipt) {
        receiptScreen.setReceipt(receipt);
        bookingsScreen.refresh();
        refreshUnreadCount();
        showPage(CustomerNavItem.RECEIPT, false);
    }

    private void refreshUnreadCount() {
        if (customer == null) {
            return;
        }
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws SQLException {
                return service.countUnreadNotifications(customer);
            }

            @Override
            protected void done() {
                try {
                    int unreadCount = get();
                    notificationButton.setText("Notifications  (" + unreadCount + ")");
                    notificationButton.setToolTipText(unreadCount
                            + " unread notification(s)");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    notificationButton.setToolTipText("Unable to refresh unread notifications.");
                } catch (java.util.concurrent.ExecutionException exception) {
                    notificationButton.setToolTipText(
                            "Unable to load unread notifications: " + exception.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void showChangeError(String message) {
        javax.swing.JOptionPane.showMessageDialog(
                this, message, "Reservation update", javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
