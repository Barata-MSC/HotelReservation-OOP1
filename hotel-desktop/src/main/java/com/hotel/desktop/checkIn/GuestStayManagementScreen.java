package com.hotel.desktop.checkIn;

import com.hotel.desktop.checkOut.CheckOutScreen;
import com.hotel.desktop.ui.StaffUi;
import java.awt.BorderLayout;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/** Keeps arrival and departure actions together in one guest-stay workflow. */
public final class GuestStayManagementScreen extends JPanel {

    private static final long serialVersionUID = 1L;
    private final CheckInScreen checkInScreen;
    private final CheckOutScreen checkOutScreen;
    private final JTabbedPane tabs = new JTabbedPane();

    public GuestStayManagementScreen(CheckInScreen checkInScreen, CheckOutScreen checkOutScreen) {
        super(new BorderLayout());
        this.checkInScreen = Objects.requireNonNull(checkInScreen);
        this.checkOutScreen = Objects.requireNonNull(checkOutScreen);
        setBackground(StaffUi.CANVAS);
        tabs.setFont(StaffUi.bodyFont(14).deriveFont(java.awt.Font.BOLD));
        tabs.addTab("Arrivals · Check in", checkInScreen);
        tabs.addTab("In-house · Check out", checkOutScreen);
        tabs.addChangeListener(event -> refreshSelectedTab());
        add(tabs, BorderLayout.CENTER);
    }

    public void showCheckIn() {
        tabs.setSelectedIndex(0);
        checkInScreen.refreshArrivals();
    }

    public void showCheckOut() {
        tabs.setSelectedIndex(1);
        checkOutScreen.refreshReservations();
    }

    public void refreshAll() {
        checkInScreen.refreshArrivals();
        checkOutScreen.refreshReservations();
    }

    private void refreshSelectedTab() {
        if (tabs.getSelectedIndex() == 0) {
            checkInScreen.refreshArrivals();
        } else {
            checkOutScreen.refreshReservations();
        }
    }
}
