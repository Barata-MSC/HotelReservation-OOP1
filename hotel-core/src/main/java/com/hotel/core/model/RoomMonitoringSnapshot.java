package com.hotel.core.model;

import com.hotel.core.model.enums.RoomMonitoringStatus;
import com.hotel.core.model.enums.RoomType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RoomMonitoringSnapshot {

    private final List<MonitoredRoom> rooms;
    private final Map<RoomMonitoringStatus, Integer> counts;
    private final Map<RoomType, RoomTypeAvailability> availabilityByRoomType;

    private RoomMonitoringSnapshot(
            List<MonitoredRoom> rooms,
            Map<RoomMonitoringStatus, Integer> counts,
            Map<RoomType, RoomTypeAvailability> availabilityByRoomType) {
        this.rooms = Collections.unmodifiableList(new ArrayList<>(rooms));
        this.counts = Collections.unmodifiableMap(new EnumMap<>(counts));
        this.availabilityByRoomType =
                Collections.unmodifiableMap(new EnumMap<>(availabilityByRoomType));
    }

    public static RoomMonitoringSnapshot fromRooms(List<Room> sourceRooms) {
        Objects.requireNonNull(sourceRooms, "sourceRooms");

        List<MonitoredRoom> monitoredRooms = new ArrayList<>();
        Map<RoomMonitoringStatus, Integer> statusCounts = new EnumMap<>(RoomMonitoringStatus.class);
        for (RoomMonitoringStatus status : RoomMonitoringStatus.values()) {
            statusCounts.put(status, 0);
        }

        Map<RoomType, int[]> typeCounts = new EnumMap<>(RoomType.class);
        for (RoomType type : RoomType.values()) {
            typeCounts.put(type, new int[2]);
        }

        for (Room room : sourceRooms) {
            Objects.requireNonNull(room, "sourceRooms contains null");
            RoomMonitoringStatus status = RoomMonitoringStatus.fromRoom(room);
            monitoredRooms.add(new MonitoredRoom(room, status));
            statusCounts.put(status, statusCounts.get(status) + 1);

            int[] counts = typeCounts.get(Objects.requireNonNull(room.getRoomType(), "room.roomType"));
            counts[1]++;
            if (status == RoomMonitoringStatus.AVAILABLE) {
                counts[0]++;
            }
        }

        Map<RoomType, RoomTypeAvailability> availability = new EnumMap<>(RoomType.class);
        for (RoomType type : RoomType.values()) {
            int[] counts = typeCounts.get(type);
            availability.put(type, new RoomTypeAvailability(counts[0], counts[1]));
        }

        return new RoomMonitoringSnapshot(monitoredRooms, statusCounts, availability);
    }

    public List<MonitoredRoom> getRooms() {
        return rooms;
    }

    public int getCount(RoomMonitoringStatus status) {
        return counts.get(Objects.requireNonNull(status, "status"));
    }

    public Map<RoomMonitoringStatus, Integer> getCounts() {
        return counts;
    }

    public Map<RoomType, RoomTypeAvailability> getAvailabilityByRoomType() {
        return availabilityByRoomType;
    }

    public int getTotalRoomCount() {
        return rooms.size();
    }

    public static final class MonitoredRoom {

        private final Room room;
        private final RoomMonitoringStatus status;

        private MonitoredRoom(Room room, RoomMonitoringStatus status) {
            this.room = room;
            this.status = status;
        }

        public Room getRoom() {
            return room;
        }

        public RoomMonitoringStatus getStatus() {
            return status;
        }
    }

    public static final class RoomTypeAvailability {

        private final int availableCount;
        private final int totalCount;

        private RoomTypeAvailability(int availableCount, int totalCount) {
            this.availableCount = availableCount;
            this.totalCount = totalCount;
        }

        public int getAvailableCount() {
            return availableCount;
        }

        public int getTotalCount() {
            return totalCount;
        }
    }
}
