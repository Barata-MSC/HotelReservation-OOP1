package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.RoomMonitoringStatus;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.repository.RoomRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomServiceTest {

    @Test
    void rejectsMissingRoom() {
        assertThrows(ValidationException.class, () -> new RoomService(new StubRoomRepository()).addRoom(null));
    }

    @Test
    void rejectsNonPositiveRoomNumberAndFloor() {
        assertThrows(ValidationException.class,
                () -> new RoomService(new StubRoomRepository()).addRoom(room(0, 1, 1, BigDecimal.TEN)));
        assertThrows(ValidationException.class,
                () -> new RoomService(new StubRoomRepository()).addRoom(room(101, 0, 1, BigDecimal.TEN)));
    }

    @Test
    void rejectsInvalidCapacity() {
        assertThrows(ValidationException.class,
                () -> new RoomService(new StubRoomRepository()).addRoom(room(101, 1, 0, BigDecimal.TEN)));
    }

    @Test
    void rejectsMissingTypeStatusAndPrice() {
        Room missingType = new Room(101, 1, null, 1, "Single room",
                BigDecimal.TEN, false, HousekeepingStatus.CLEAN);
        Room missingStatus = new Room(102, 1, RoomType.SINGLE, 1, "Single room",
                BigDecimal.TEN, false, null);
        Room missingPrice = new Room(103, 1, RoomType.SINGLE, 1, "Single room",
                null, false, HousekeepingStatus.CLEAN);
        RoomService service = new RoomService(new StubRoomRepository());

        assertThrows(ValidationException.class, () -> service.addRoom(missingType));
        assertThrows(ValidationException.class, () -> service.addRoom(missingStatus));
        assertThrows(ValidationException.class, () -> service.addRoom(missingPrice));
    }

    @Test
    void rejectsMissingDescriptionAndNonPositivePrice() {
        Room missingDescription = new Room(101, 1, RoomType.SINGLE, 1, "  ",
                BigDecimal.TEN, false, HousekeepingStatus.CLEAN);
        Room zeroPrice = room(102, 1, 1, BigDecimal.ZERO);
        Room negativePrice = room(103, 1, 1, BigDecimal.ONE.negate());

        RoomService service = new RoomService(new StubRoomRepository());
        assertThrows(ValidationException.class, () -> service.addRoom(missingDescription));
        assertThrows(ValidationException.class, () -> service.addRoom(zeroPrice));
        assertThrows(ValidationException.class, () -> service.addRoom(negativePrice));
    }

    @Test
    void rejectsDuplicateRoomNumber() {
        StubRoomRepository repository = new StubRoomRepository();
        repository.roomExists = true;

        assertThrows(ValidationException.class,
                () -> new RoomService(repository).addRoom(room(101, 1, 1, BigDecimal.TEN)));
    }

    @Test
    void acceptsValidRoom() {
        assertDoesNotThrow(
                () -> new RoomService(new StubRoomRepository()).addRoom(room(101, 1, 1, BigDecimal.TEN)));
    }

    @Test
    void preventsUnavailableStatusWhenGuestIsCheckedIn() throws Exception {
        StubRoomRepository repository = new StubRoomRepository();
        repository.checkedInGuest = true;
        RoomService service = new RoomService(repository);

        assertThrows(ValidationException.class,
                () -> service.setHousekeepingStatus(101, HousekeepingStatus.UNDER_MAINTENANCE));
        assertThrows(ValidationException.class,
                () -> service.setHousekeepingStatus(101, HousekeepingStatus.OUT_OF_SERVICE));
        assertThrows(ValidationException.class,
                () -> service.updateRoom(room(
                        101, 1, 1, BigDecimal.TEN, HousekeepingStatus.UNDER_MAINTENANCE)));
    }

    @Test
    void monitoringStatusUsesDocumentedPrecedence() {
        assertEquals(RoomMonitoringStatus.OUT_OF_SERVICE, RoomMonitoringStatus.fromRoom(
                room(101, 1, 1, BigDecimal.TEN, true, HousekeepingStatus.OUT_OF_SERVICE)));
        assertEquals(RoomMonitoringStatus.UNDER_MAINTENANCE, RoomMonitoringStatus.fromRoom(
                room(102, 1, 1, BigDecimal.TEN, true, HousekeepingStatus.UNDER_MAINTENANCE)));
        assertEquals(RoomMonitoringStatus.DIRTY, RoomMonitoringStatus.fromRoom(
                room(103, 1, 1, BigDecimal.TEN, true, HousekeepingStatus.DIRTY)));
        assertEquals(RoomMonitoringStatus.OCCUPIED, RoomMonitoringStatus.fromRoom(
                room(104, 1, 1, BigDecimal.TEN, true, HousekeepingStatus.CLEAN)));
        assertEquals(RoomMonitoringStatus.AVAILABLE, RoomMonitoringStatus.fromRoom(
                room(105, 1, 1, BigDecimal.TEN, false, HousekeepingStatus.CLEAN)));
    }

    @Test
    void monitoringSnapshotCountsStatusesAndAvailableRoomsByType() throws Exception {
        StubRoomRepository repository = new StubRoomRepository();
        repository.rooms.add(room(101, 1, 1, BigDecimal.TEN, false, HousekeepingStatus.CLEAN));
        repository.rooms.add(room(102, 1, 1, BigDecimal.TEN, true, HousekeepingStatus.CLEAN));
        repository.rooms.add(new Room(201, 2, RoomType.DOUBLE, 2, "Double room",
                BigDecimal.valueOf(20), false, HousekeepingStatus.DIRTY));
        repository.rooms.add(new Room(301, 3, RoomType.DELUXE, 2, "Deluxe room",
                BigDecimal.valueOf(30), false, HousekeepingStatus.OUT_OF_SERVICE));

        var snapshot = new RoomService(repository).getMonitoringSnapshot();

        assertEquals(4, snapshot.getTotalRoomCount());
        assertEquals(1, snapshot.getCount(RoomMonitoringStatus.AVAILABLE));
        assertEquals(1, snapshot.getCount(RoomMonitoringStatus.OCCUPIED));
        assertEquals(1, snapshot.getCount(RoomMonitoringStatus.DIRTY));
        assertEquals(0, snapshot.getCount(RoomMonitoringStatus.UNDER_MAINTENANCE));
        assertEquals(1, snapshot.getCount(RoomMonitoringStatus.OUT_OF_SERVICE));
        assertEquals(1, snapshot.getAvailabilityByRoomType().get(RoomType.SINGLE).getAvailableCount());
        assertEquals(2, snapshot.getAvailabilityByRoomType().get(RoomType.SINGLE).getTotalCount());
        assertEquals(0, snapshot.getAvailabilityByRoomType().get(RoomType.DOUBLE).getAvailableCount());
        assertEquals(1, snapshot.getAvailabilityByRoomType().get(RoomType.DOUBLE).getTotalCount());
        assertEquals(0, snapshot.getAvailabilityByRoomType().get(RoomType.DELUXE).getAvailableCount());
        assertEquals(1, snapshot.getAvailabilityByRoomType().get(RoomType.DELUXE).getTotalCount());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.getRooms().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.getCounts().put(RoomMonitoringStatus.AVAILABLE, 99));
    }

    @Test
    void monitoringSnapshotIncludesZeroCountsForEmptyInventory() throws Exception {
        var snapshot = new RoomService(new StubRoomRepository()).getMonitoringSnapshot();

        assertEquals(0, snapshot.getTotalRoomCount());
        for (RoomMonitoringStatus status : RoomMonitoringStatus.values()) {
            assertEquals(0, snapshot.getCount(status));
        }
        for (RoomType type : RoomType.values()) {
            assertEquals(0, snapshot.getAvailabilityByRoomType().get(type).getAvailableCount());
            assertEquals(0, snapshot.getAvailabilityByRoomType().get(type).getTotalCount());
        }
    }

    private static Room room(int roomNumber, int floor, int capacity, BigDecimal price) {
        return room(roomNumber, floor, capacity, price, HousekeepingStatus.CLEAN);
    }

    private static Room room(
            int roomNumber, int floor, int capacity, BigDecimal price, HousekeepingStatus housekeepingStatus) {
        return new Room(roomNumber, floor, RoomType.SINGLE, capacity, "Single room",
                price, false, housekeepingStatus);
    }

    private static Room room(
            int roomNumber,
            int floor,
            int capacity,
            BigDecimal price,
            boolean occupied,
            HousekeepingStatus housekeepingStatus) {
        return new Room(roomNumber, floor, RoomType.SINGLE, capacity, "Single room",
                price, occupied, housekeepingStatus);
    }

    private static final class StubRoomRepository extends RoomRepository {

        private boolean roomExists;
        private boolean checkedInGuest;
        private final List<Room> rooms = new ArrayList<>();

        @Override
        public boolean existsByNumber(int roomNumber) {
            return roomExists;
        }

        @Override
        public void add(Room room) {
        }

        @Override
        public void update(Room room) {
        }

        @Override
        public Room findByNumber(int roomNumber) {
            return room(101, 1, 1, BigDecimal.TEN);
        }

        @Override
        public boolean hasCheckedInGuest(int roomNumber) {
            return checkedInGuest;
        }

        @Override
        public void setHousekeepingStatus(int roomNumber, HousekeepingStatus status) {
        }

        @Override
        public List<Room> findAll() {
            return new ArrayList<>(rooms);
        }
    }
}
