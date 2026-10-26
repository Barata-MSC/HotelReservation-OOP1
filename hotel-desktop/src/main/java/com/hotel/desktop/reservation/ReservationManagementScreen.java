package com.hotel.desktop.reservation;

import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.service.ReservationService;
import com.hotel.core.service.RoomService;
import com.hotel.desktop.ui.StaffUi;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public final class ReservationManagementScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private final WalkInReservationScreen walkInScreen;
    private final OnlineReservationScreen onlineScreen;
    private final ReservationCalendarScreen calendarScreen;
    private final JTabbedPane tabs = new JTabbedPane();

    public ReservationManagementScreen(
            WalkInReservationScreen walkInScreen,
            OnlineReservationScreen onlineScreen,
            ReservationCalendarScreen calendarScreen) {
        this.walkInScreen = Objects.requireNonNull(walkInScreen);
        this.onlineScreen = Objects.requireNonNull(onlineScreen);
        this.calendarScreen = Objects.requireNonNull(calendarScreen);
        setLayout(new BorderLayout());
        setBackground(StaffUi.CANVAS);
        tabs.setFont(StaffUi.bodyFont(14).deriveFont(java.awt.Font.BOLD));
        tabs.addTab("Walk-in", walkInScreen);
        tabs.addTab("Online review", onlineScreen);
        tabs.addTab("Calendar", calendarScreen);
        tabs.addChangeListener(event -> refreshSelectedTab());
        add(tabs, BorderLayout.CENTER);
    }

    public void showWalkIn() {
        tabs.setSelectedIndex(0);
        walkInScreen.refreshAvailableRooms();
    }

    public void refreshAll() {
        onlineScreen.refreshReservations();
        calendarScreen.refreshMonth();
    }

    private void refreshSelectedTab() {
        switch (tabs.getSelectedIndex()) {
            case 0 -> walkInScreen.refreshAvailableRooms();
            case 1 -> onlineScreen.refreshReservations();
            case 2 -> calendarScreen.refreshMonth();
            default -> {
            }
        }
    }
}
