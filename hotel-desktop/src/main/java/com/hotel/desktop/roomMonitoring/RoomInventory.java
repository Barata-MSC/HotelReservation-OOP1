package com.hotel.desktop.roomMonitoring;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Shared in-memory sample room data for the dashboard and room monitor. */
public final class RoomInventory {

    private final List<Room> rooms = new ArrayList<>();
    private final List<Room> roomView = Collections.unmodifiableList(rooms);

    public RoomInventory() {
        addFloor(3, "AAAOOCM");
        addFloor(2, "AAAAAOOOOCCM");
        addFloor(1, "AAAAOOCX");
    }

    public List<Room> getRooms() {
        return roomView;
    }

    public int count(Status status) {
        Objects.requireNonNull(status, "status");
        int count = 0;
        for (Room room : rooms) {
            if (room.status == status) {
                count++;
            }
        }
        return count;
    }

    public void cycleStatus(int roomNumber) {
        for (Room room : rooms) {
            if (room.number == roomNumber) {
                room.status = room.status.next();
                return;
            }
        }
        throw new IllegalArgumentException("Unknown room number: " + roomNumber);
    }

    public void setStatus(int roomNumber, Status status) {
        Objects.requireNonNull(status, "status");
        for (Room room : rooms) {
            if (room.number == roomNumber) {
                room.status = status;
                return;
            }
        }
        throw new IllegalArgumentException("Unknown room number: " + roomNumber);
    }

    private void addFloor(int floor, String statuses) {
        for (int i = 0; i < statuses.length(); i++) {
            Status status = switch (statuses.charAt(i)) {
                case 'A' -> Status.AVAILABLE;
                case 'O' -> Status.OCCUPIED;
                case 'C' -> Status.DIRTY;
                case 'M' -> Status.UNDER_MAINTENANCE;
                case 'X' -> Status.OUT_OF_SERVICE;
                default -> throw new IllegalArgumentException(
                        "Unknown sample room status: " + statuses.charAt(i));
            };
            rooms.add(new Room(floor * 100 + i + 1, floor, status));
        }
    }

    public enum Status {
        AVAILABLE("Available"),
        OCCUPIED("Occupied"),
        DIRTY("Dirty"),
        UNDER_MAINTENANCE("Under Maintenance"),
        OUT_OF_SERVICE("Out of Service");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        private Status next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public static final class Room {
        private final int number;
        private final int floor;
        private Status status;

        private Room(int number, int floor, Status status) {
            this.number = number;
            this.floor = floor;
            this.status = status;
        }

        public int getNumber() {
            return number;
        }

        public int getFloor() {
            return floor;
        }

        public Status getStatus() {
            return status;
        }
    }
}
