package com.hotel.desktop;

import com.hotel.core.model.StaffUser;
import com.hotel.core.service.AuthenticationService;
import com.hotel.core.service.ReservationService;
import com.hotel.core.service.RoomService;
import com.hotel.desktop.auth.LoginScreen;
import com.hotel.desktop.auth.WelcomeScreen;
import com.hotel.desktop.checkIn.CheckInScreen;
import com.hotel.desktop.checkOut.CheckOutScreen;
import com.hotel.desktop.dashboard.DashboardScreen;
import com.hotel.desktop.houseKeepingStatus.HousekeepingScreen;
import com.hotel.desktop.nav.NavItem;
import com.hotel.desktop.nav.WorkspaceShell;
import com.hotel.desktop.roomManagement.RoomManagementScreen;
import com.hotel.desktop.roomMonitoring.RoomMonitoringDataSource;
import com.hotel.desktop.roomMonitoring.RoomMonitoringScreen;
import com.hotel.desktop.reservation.OnlineReservationScreen;
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
    private final WelcomeScreen welcomeScreen;
    private final DashboardScreen dashboardScreen;
    private final WalkInReservationScreen walkInReservationScreen;
    private final OnlineReservationScreen onlineReservationScreen;
    private final CheckInScreen checkInScreen;
    private final CheckOutScreen checkOutScreen;
    private final HousekeepingScreen housekeepingScreen;
    private final RoomMonitoringScreen roomMonitoringScreen;
    private final RoomService roomService = new RoomService();
    private final AuthenticationService authenticationService = new AuthenticationService();
    private final ReservationService reservationService = new ReservationService();
    private final RoomMonitoringDataSource roomMonitoringDataSource =
            new RoomMonitoringDataSource(roomService);
    private final RoomManagementScreen roomManagementScreen;
    private final WorkspaceShell workspaceShell;
    private StaffUser authenticatedStaff;

    public HotelDesktop() {
        initComponents();
        setMinimumSize(new Dimension(1360, 800));
        pack();
        setLocationRelativeTo(null);

        cardLayout = (CardLayout) screenDeck.getLayout();
        loginScreen = new LoginScreen(authenticationService, this::onAuthenticated);
        roomManagementScreen = new RoomManagementScreen(roomService, roomMonitoringDataSource::refresh);
        dashboardScreen = new DashboardScreen(
                () -> showWorkspace(NavItem.ROOM_MANAGEMENT),
                () -> showWorkspace(NavItem.WALK_IN),
                () -> showWorkspace(NavItem.CHECK_IN),
                () -> showWorkspace(NavItem.CHECK_OUT),
                roomMonitoringDataSource);
        walkInReservationScreen = new WalkInReservationScreen(
                reservationService, this::refreshReservationViews);
        onlineReservationScreen = new OnlineReservationScreen();
        checkInScreen = new CheckInScreen(reservationService, this::refreshReservationViews);
        checkOutScreen = new CheckOutScreen(reservationService, this::refreshReservationViews);
        roomMonitoringScreen = new RoomMonitoringScreen(roomMonitoringDataSource);
        housekeepingScreen = new HousekeepingScreen(roomService, roomMonitoringDataSource::refresh);
        welcomeScreen = new WelcomeScreen(this::showLogin);

        workspaceShell = new WorkspaceShell();
        workspaceShell.addScreen(NavItem.DASHBOARD, dashboardScreen);
        workspaceShell.addScreen(NavItem.ROOM_MANAGEMENT, roomManagementScreen);
        workspaceShell.addScreen(NavItem.ROOM_MONITORING, roomMonitoringScreen);
        workspaceShell.addScreen(NavItem.HOUSEKEEPING, housekeepingScreen);
        workspaceShell.addScreen(NavItem.WALK_IN, walkInReservationScreen);
        workspaceShell.addScreen(NavItem.ONLINE_RESERVATIONS, onlineReservationScreen);
        workspaceShell.addScreen(NavItem.CHECK_IN, checkInScreen);
        workspaceShell.addScreen(NavItem.CHECK_OUT, checkOutScreen);
        EnumSet<NavItem> availableScreens = EnumSet.of(
                NavItem.DASHBOARD, NavItem.ROOM_MANAGEMENT,
                NavItem.ROOM_MONITORING, NavItem.HOUSEKEEPING,
                NavItem.WALK_IN, NavItem.ONLINE_RESERVATIONS,
                NavItem.CHECK_IN, NavItem.CHECK_OUT);
        workspaceShell.getSidebar().setOnNavigate(item -> {
            if (availableScreens.contains(item)) {
                showWorkspace(item);
            }
        });
        workspaceShell.getSidebar().setOnLogout(this::logout);

        screenDeck.add(welcomeScreen, "welcome");
        screenDeck.add(loginScreen, "login");
        screenDeck.add(workspaceShell, "shell");
        showWelcome();

    }

    private void showLogin() {
        loginScreen.clearCredentialsAndReset();
        cardLayout.show(screenDeck, "login");
        getRootPane().setDefaultButton(loginScreen.getDefaultButton());
    }

    private void onAuthenticated(StaffUser staffUser) {
        authenticatedStaff = staffUser;
        workspaceShell.getSidebar().setStaffName(staffUser.getFullName());
        workspaceShell.getSidebar().setStaffRole(staffUser.getRole());
        showWorkspace(NavItem.DASHBOARD);
    }

    private void showWorkspace(NavItem item) {
        if (authenticatedStaff == null) {
            showWelcome();
            return;
        }
        if (item == NavItem.DASHBOARD) {
            dashboardScreen.refreshRoomCounts();
        } else if (item == NavItem.ROOM_MONITORING) {
            roomMonitoringScreen.refreshData();
        } else if (item == NavItem.WALK_IN) {
            walkInReservationScreen.refreshAvailableRooms();
        } else if (item == NavItem.CHECK_IN) {
            checkInScreen.refreshArrivals();
        } else if (item == NavItem.CHECK_OUT) {
            checkOutScreen.refreshReservations();
        }
        cardLayout.show(screenDeck, "shell");
        workspaceShell.show(item);
        if (item == NavItem.DASHBOARD || item == NavItem.ROOM_MANAGEMENT) {
            getRootPane().setDefaultButton(workspaceShell.getSidebar().getLogoutButton());
        } else {
            getRootPane().setDefaultButton(null);
        }
    }

    private void refreshReservationViews() {
        roomMonitoringDataSource.refresh();
        roomMonitoringScreen.refreshData();
        checkInScreen.refreshArrivals();
        checkOutScreen.refreshReservations();
        walkInReservationScreen.refreshAvailableRooms();
    }

    private void logout() {
        authenticatedStaff = null;
        workspaceShell.getSidebar().setStaffName("Staff");
        workspaceShell.getSidebar().setStaffRole("Receptionist");
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
