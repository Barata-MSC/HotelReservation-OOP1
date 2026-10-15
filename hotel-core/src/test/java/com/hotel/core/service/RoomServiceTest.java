package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.repository.RoomRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

    private static Room room(int roomNumber, int floor, int capacity, BigDecimal price) {
        return room(roomNumber, floor, capacity, price, HousekeepingStatus.CLEAN);
    }

    private static Room room(
            int roomNumber, int floor, int capacity, BigDecimal price, HousekeepingStatus housekeepingStatus) {
        return new Room(roomNumber, floor, RoomType.SINGLE, capacity, "Single room",
                price, false, housekeepingStatus);
    }

    private static final class StubRoomRepository extends RoomRepository {

        private boolean roomExists;
        private boolean checkedInGuest;

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
    }
}
