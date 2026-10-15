package com.hotel.desktop.nav;

import com.hotel.desktop.dashboard.DashboardScreen;

public enum NavItem {
    DASHBOARD("Dashboard", DashboardScreen.Glyph.HOME),
    ROOM_MANAGEMENT("Room management", DashboardScreen.Glyph.DOOR),
    ROOM_MONITORING("Room monitoring", DashboardScreen.Glyph.GRID),
    HOUSEKEEPING("Housekeeping", DashboardScreen.Glyph.SPARKLE),
    WALK_IN("Walk-in reservation", DashboardScreen.Glyph.PERSON_PLUS),
    ONLINE_RESERVATIONS("Online reservations", DashboardScreen.Glyph.GLOBE),
    CHECK_IN("Check in", DashboardScreen.Glyph.CHECK_IN),
    CHECK_OUT("Check out", DashboardScreen.Glyph.CHECK_OUT);

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
