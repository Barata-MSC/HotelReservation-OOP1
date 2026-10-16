package com.hotel.core.model.enums;

import com.hotel.core.model.Room;

import java.util.Objects;

public enum RoomMonitoringStatus {
    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    DIRTY("Dirty"),
    UNDER_MAINTENANCE("Under Maintenance"),
    OUT_OF_SERVICE("Out of Service");

    private final String displayName;

    RoomMonitoringStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RoomMonitoringStatus fromRoom(Room room) {
        Objects.requireNonNull(room, "room");
        HousekeepingStatus housekeepingStatus =
                Objects.requireNonNull(room.getHousekeepingStatus(), "room.housekeepingStatus");

        if (housekeepingStatus == HousekeepingStatus.OUT_OF_SERVICE) {
            return OUT_OF_SERVICE;
        }
        if (housekeepingStatus == HousekeepingStatus.UNDER_MAINTENANCE) {
            return UNDER_MAINTENANCE;
        }
        if (housekeepingStatus == HousekeepingStatus.DIRTY) {
            return DIRTY;
        }
        return room.isOccupied() ? OCCUPIED : AVAILABLE;
    }
}
