package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.Room;
import com.hotel.core.model.RoomMonitoringSnapshot;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService() {
        this(new RoomRepository());
    }

    public RoomService(RoomRepository roomRepository) {
        if (roomRepository == null) {
            throw new ValidationException("Room repository is required");
        }
        this.roomRepository = roomRepository;
    }

    public void addRoom(Room room) throws SQLException {
        validateRoom(room);
        if (roomRepository.existsByNumber(room.getRoomNumber())) {
            throw new ValidationException("Room number already exists: " + room.getRoomNumber());
        }
        roomRepository.add(room);
    }

    public void updateRoom(Room room) throws SQLException {
        validateRoom(room);
        if (isUnavailableStatus(room.getHousekeepingStatus())
                && roomRepository.hasCheckedInGuest(room.getRoomNumber())) {
            throw new ValidationException(
                    "A room with a checked-in guest cannot be set to "
                            + room.getHousekeepingStatus().getDatabaseValue());
        }
        roomRepository.update(room);
    }

    public Room findByNumber(int roomNumber) throws SQLException {
        return roomRepository.findByNumber(roomNumber);
    }

    public List<Room> search(Integer roomNumber, RoomType roomType) throws SQLException {
        return roomRepository.search(roomNumber, roomType);
    }

    public List<Room> findAll() throws SQLException {
        return roomRepository.findAll();
    }

    public RoomMonitoringSnapshot getMonitoringSnapshot() throws SQLException {
        return RoomMonitoringSnapshot.fromRooms(roomRepository.findAll());
    }

    public void setHousekeepingStatus(int roomNumber, HousekeepingStatus status) throws SQLException {
        if (status == null) {
            throw new ValidationException("Housekeeping status is required");
        }
        if (isUnavailableStatus(status) && roomRepository.hasCheckedInGuest(roomNumber)) {
            throw new ValidationException(
                    "A room with a checked-in guest cannot be set to " + status.getDatabaseValue());
        }
        roomRepository.setHousekeepingStatus(roomNumber, status);
    }

    public void setOccupied(int roomNumber, boolean occupied) throws SQLException {
        roomRepository.setOccupied(roomNumber, occupied);
    }

    private static void validateRoom(Room room) {
        if (room == null) {
            throw new ValidationException("Room is required");
        }
        if (room.getRoomNumber() < 1) {
            throw new ValidationException("Room number must be a positive whole number");
        }
        if (room.getFloor() < 1) {
            throw new ValidationException("Floor must be a positive whole number");
        }
        if (room.getRoomType() == null) {
            throw new ValidationException("Room type is required");
        }
        if (room.getCapacity() < 1) {
            throw new ValidationException("Capacity must be a whole number greater than or equal to 1");
        }
        if (room.getDescription() == null || room.getDescription().isBlank()) {
            throw new ValidationException("Description is required");
        }
        BigDecimal price = room.getPricePerDay();
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Price per day must be greater than zero");
        }
        if (room.getHousekeepingStatus() == null) {
            throw new ValidationException("Housekeeping status is required");
        }
    }

    private static boolean isUnavailableStatus(HousekeepingStatus status) {
        return status == HousekeepingStatus.UNDER_MAINTENANCE
                || status == HousekeepingStatus.OUT_OF_SERVICE;
    }
}
