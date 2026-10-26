package com.hotel.desktop.nav;

import com.hotel.desktop.dashboard.DashboardScreen;

public enum NavItem {
    DASHBOARD("Dashboard", DashboardScreen.Glyph.HOME),
    ROOM_MANAGEMENT("Room management", DashboardScreen.Glyph.DOOR),
    ROOM_MONITORING("Room monitoring", DashboardScreen.Glyph.GRID),
    HOUSEKEEPING("Housekeeping", DashboardScreen.Glyph.SPARKLE),
    RESERVATIONS("Reservations", DashboardScreen.Glyph.PERSON_PLUS),
    GUEST_STAYS("Guest stays", DashboardScreen.Glyph.CHECK_IN),
    BOOKING_HISTORY("Booking history", DashboardScreen.Glyph.HISTORY);

    private final String label;
    private final DashboardScreen.Glyph glyph;

    NavItem(String label, DashboardScreen.Glyph glyph) {
        this.label = label;
        this.glyph = glyph;
    }

    public String getLabel() {
        return label;
    }

    public DashboardScreen.Glyph getGlyph() {
        return glyph;
    }
}
