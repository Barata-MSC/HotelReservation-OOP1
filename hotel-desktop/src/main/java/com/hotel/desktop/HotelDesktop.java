package com.hotel.desktop;

import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.UserRole;
import com.hotel.core.service.AuthenticationService;
import com.hotel.core.service.CustomerReservationService;
import com.hotel.core.service.ReservationService;
import com.hotel.core.service.RoomService;
import com.hotel.core.service.RoomStatusResetScheduler;
import com.hotel.desktop.auth.LoginScreen;
import com.hotel.desktop.auth.CustomerSignUpPanel;
import com.hotel.desktop.auth.WelcomeScreen;
import com.hotel.desktop.checkIn.CheckInScreen;
import com.hotel.desktop.checkIn.GuestStayManagementScreen;
import com.hotel.desktop.checkOut.CheckOutScreen;
import com.hotel.desktop.dashboard.DashboardScreen;
import com.hotel.desktop.customer.CustomerWorkspacePanel;
import com.hotel.desktop.houseKeepingStatus.HousekeepingScreen;
import com.hotel.desktop.nav.NavItem;
import com.hotel.desktop.nav.WorkspaceShell;
import com.hotel.desktop.roomManagement.RoomManagementScreen;
import com.hotel.desktop.roomMonitoring.RoomMonitoringDataSource;
import com.hotel.desktop.roomMonitoring.RoomMonitoringScreen;
import com.hotel.desktop.reservation.OnlineReservationScreen;
import com.hotel.desktop.reservation.ReservationCalendarScreen;
import com.hotel.desktop.reservation.ReservationManagementScreen;
import com.hotel.desktop.reservation.BookingHistoryScreen;
import com.hotel.desktop.reservation.WalkInReservationScreen;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.EnumSet;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Main window that hosts the authentication screens. */
public class HotelDesktop extends JFrame {

    private static final long serialVersionUID = 1L;
    private final CardLayout cardLayout;
    private final LoginScreen loginScreen;
    private final CustomerSignUpPanel customerSignUpPanel;
    private final WelcomeScreen welcomeScreen;
    private final DashboardScreen dashboardScreen;
    private final WalkInReservationScreen walkInReservationScreen;
    private final OnlineReservationScreen onlineReservationScreen;
    private final ReservationManagementScreen reservationManagementScreen;
    private final BookingHistoryScreen bookingHistoryScreen;
    private final CheckInScreen checkInScreen;
    private final CheckOutScreen checkOutScreen;
    private final GuestStayManagementScreen guestStayManagementScreen;
    private final HousekeepingScreen housekeepingScreen;
    private final RoomMonitoringScreen roomMonitoringScreen;
    private final RoomService roomService = new RoomService();
    private final RoomStatusResetScheduler roomStatusResetScheduler =
            new RoomStatusResetScheduler(roomService, this::refreshRoomStatusViews);
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final ReservationService reservationService = new ReservationService();
    private final CustomerReservationService customerReservationService = new CustomerReservationService();
    private final RoomMonitoringDataSource roomMonitoringDataSource =
            new RoomMonitoringDataSource(roomService);
    private final RoomManagementScreen roomManagementScreen;
    private final WorkspaceShell workspaceShell;
    private final CustomerWorkspacePanel customerWorkspacePanel;
    private AuthenticatedUser authenticatedUser;

    public HotelDesktop() {
        initComponents();
        setMinimumSize(new Dimension(1360, 800));
        pack();
        setLocationRelativeTo(null);

        cardLayout = (CardLayout) screenDeck.getLayout();
        loginScreen = new LoginScreen(authenticationService, this::onAuthenticated, this::showCustomerSignUp);
        customerSignUpPanel = new CustomerSignUpPanel(
                authenticationService, this::onAuthenticated, this::showLogin);
        customerWorkspacePanel = new CustomerWorkspacePanel(customerReservationService, this::logout);
        roomManagementScreen = new RoomManagementScreen(roomService, this::refreshReservationViews);
        walkInReservationScreen = new WalkInReservationScreen(
                reservationService, () -> authenticatedUser, this::refreshReservationViews);
        onlineReservationScreen = new OnlineReservationScreen(
                reservationService, roomService, () -> authenticatedUser, this::refreshReservationViews);
        ReservationCalendarScreen reservationCalendarScreen =
                new ReservationCalendarScreen(reservationService, () -> authenticatedUser);
        reservationManagementScreen = new ReservationManagementScreen(
                walkInReservationScreen, onlineReservationScreen, reservationCalendarScreen);
        bookingHistoryScreen = new BookingHistoryScreen(reservationService, () -> authenticatedUser);
        dashboardScreen = new DashboardScreen(
                () -> showWorkspace(NavItem.ROOM_MANAGEMENT),
                this::openWalkInReservations,
                this::openCheckIn,
                this::openCheckOut,
                roomMonitoringDataSource, reservationService, () -> authenticatedUser,
                this::updatePendingReservationBadge);
        checkInScreen = new CheckInScreen(
                reservationService, () -> authenticatedUser, this::refreshReservationViews);
        checkOutScreen = new CheckOutScreen(
                reservationService, () -> authenticatedUser, this::refreshReservationViews);
        guestStayManagementScreen = new GuestStayManagementScreen(checkInScreen, checkOutScreen);
        roomMonitoringScreen = new RoomMonitoringScreen(roomMonitoringDataSource, roomService);
        housekeepingScreen = new HousekeepingScreen(
                roomService, reservationService, () -> authenticatedUser, this::refreshReservationViews);
        welcomeScreen = new WelcomeScreen(this::showLogin);

        workspaceShell = new WorkspaceShell();
        workspaceShell.addScreen(NavItem.DASHBOARD, dashboardScreen);
        workspaceShell.addScreen(NavItem.ROOM_MANAGEMENT, roomManagementScreen);
        workspaceShell.addScreen(NavItem.ROOM_MONITORING, roomMonitoringScreen);
        workspaceShell.addScreen(NavItem.HOUSEKEEPING, housekeepingScreen);
        workspaceShell.addScreen(NavItem.RESERVATIONS, reservationManagementScreen);
        workspaceShell.addScreen(NavItem.BOOKING_HISTORY, bookingHistoryScreen);
        workspaceShell.addScreen(NavItem.GUEST_STAYS, guestStayManagementScreen);
        EnumSet<NavItem> availableScreens = EnumSet.of(
                NavItem.DASHBOARD, NavItem.ROOM_MANAGEMENT,
                NavItem.ROOM_MONITORING, NavItem.HOUSEKEEPING,
                NavItem.RESERVATIONS,
                NavItem.BOOKING_HISTORY,
                NavItem.GUEST_STAYS);
        workspaceShell.getSidebar().setOnNavigate(item -> {
            if (availableScreens.contains(item)) {
                showWorkspace(item);
            }
        });
        workspaceShell.getSidebar().setOnLogout(this::logout);

        screenDeck.add(welcomeScreen, "welcome");
        screenDeck.add(loginScreen, "login");
        screenDeck.add(customerSignUpPanel, "signup");
        screenDeck.add(workspaceShell, "shell");
        screenDeck.add(customerWorkspacePanel, "customer");
        showWelcome();
        roomStatusResetScheduler.start();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent event) {
                roomStatusResetScheduler.close();
            }
        });
    }

    private void refreshRoomStatusViews() {
        SwingUtilities.invokeLater(() -> {
            roomMonitoringDataSource.refresh();
            dashboardScreen.refreshRoomCounts();
            roomManagementScreen.refreshData();
            walkInReservationScreen.refreshAvailableRooms();
            housekeepingScreen.refreshCurrentRoom();
        });
    }

    private void updatePendingReservationBadge(int pendingCount) {
        if (workspaceShell == null) {
            return;
        }
        workspaceShell.getSidebar().setBadge(
                NavItem.RESERVATIONS, pendingCount > 0 ? Integer.toString(pendingCount) : null);
    }

    private void openWalkInReservations() {
        showWorkspace(NavItem.RESERVATIONS);
        reservationManagementScreen.showWalkIn();
    }

    private void openCheckIn() {
        showWorkspace(NavItem.GUEST_STAYS);
        guestStayManagementScreen.showCheckIn();
    }

    private void openCheckOut() {
        showWorkspace(NavItem.GUEST_STAYS);
        guestStayManagementScreen.showCheckOut();
    }

    private void showLogin() {
        loginScreen.clearCredentialsAndReset();
        cardLayout.show(screenDeck, "login");
        getRootPane().setDefaultButton(loginScreen.getDefaultButton());
    }

    private void showCustomerSignUp() {
        customerSignUpPanel.reset();
        cardLayout.show(screenDeck, "signup");
        getRootPane().setDefaultButton(customerSignUpPanel.getDefaultButton());
    }

    private void onAuthenticated(AuthenticatedUser user) {
        authenticatedUser = user;
        if (user.getRole() == UserRole.ADMIN) {
            workspaceShell.getSidebar().setStaffName(user.getFullName());
            workspaceShell.getSidebar().setStaffRole(user.getRole().getDatabaseValue());
            showWorkspace(NavItem.DASHBOARD);
        } else {
            customerWorkspacePanel.setCustomer(user);
            showCustomerWorkspace();
        }
    }

    private void showWorkspace(NavItem item) {
        if (authenticatedUser == null || !authenticatedUser.getRole().canAccessAdminWorkspace()) {
            if (authenticatedUser != null && authenticatedUser.getRole() == UserRole.CUSTOMER) {
                showCustomerWorkspace();
            } else {
                showWelcome();
            }
            return;
        }
        if (item == null) {
            showWelcome();
            return;
        }
        if (item == NavItem.DASHBOARD) {
            dashboardScreen.refreshRoomCounts();
        } else if (item == NavItem.ROOM_MONITORING) {
            roomMonitoringScreen.refreshData();
        } else if (item == NavItem.RESERVATIONS) {
            walkInReservationScreen.refreshAvailableRooms();
            reservationManagementScreen.refreshAll();
        } else if (item == NavItem.GUEST_STAYS) {
            guestStayManagementScreen.refreshAll();
        } else if (item == NavItem.BOOKING_HISTORY) {
            bookingHistoryScreen.refreshHistory();
        }
        cardLayout.show(screenDeck, "shell");
        workspaceShell.show(item);
        if (item == NavItem.DASHBOARD || item == NavItem.ROOM_MANAGEMENT) {
            getRootPane().setDefaultButton(workspaceShell.getSidebar().getLogoutButton());
        } else {
            getRootPane().setDefaultButton(null);
        }
    }

    private void showCustomerWorkspace() {
        if (authenticatedUser == null || authenticatedUser.getRole() != UserRole.CUSTOMER) {
            showWelcome();
            return;
        }
        cardLayout.show(screenDeck, "customer");
        getRootPane().setDefaultButton(null);
    }

    private void refreshReservationViews() {
        roomMonitoringDataSource.refresh();
        dashboardScreen.refreshRoomCounts();
        roomManagementScreen.refreshData();
        housekeepingScreen.refreshCurrentRoom();
        guestStayManagementScreen.refreshAll();
        walkInReservationScreen.refreshAvailableRooms();
        reservationManagementScreen.refreshAll();
        bookingHistoryScreen.refreshHistory();
    }

    private void logout() {
        authenticatedUser = null;
        customerWorkspacePanel.clearCustomer();
        workspaceShell.getSidebar().setStaffName("Staff");
        workspaceShell.getSidebar().setStaffRole("Admin");
        loginScreen.clearCredentialsAndReset();
        showWelcome();
    }

    private void showWelcome() {
        cardLayout.show(screenDeck, "welcome");
        getRootPane().setDefaultButton(welcomeScreen.getDefaultButton());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        screenDeck = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Hotel Reservation");
        setMinimumSize(new Dimension(1200, 760));

        screenDeck.setPreferredSize(new java.awt.Dimension(1280, 800));
        screenDeck.setLayout(new java.awt.CardLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(screenDeck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(screenDeck, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel screenDeck;
    // End of variables declaration//GEN-END:variables

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HotelDesktop().setVisible(true));
    }
}
