package com.hotel.core.model;

import java.util.List;

public record AdminDashboardSnapshot(
        RoomMonitoringSnapshot rooms,
        int pendingOnlineReservations,
        int todaysArrivals,
        int todaysDepartures,
        List<AdminReservationDetails> recentReservations) {

    public AdminDashboardSnapshot {
        recentReservations = List.copyOf(recentReservations);
    }
}
