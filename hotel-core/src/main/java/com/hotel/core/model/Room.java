package com.hotel.core.model;

import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.RoomType;

import java.math.BigDecimal;

public final class Room {

    private final int roomNumber;
    private final int floor;
    private final RoomType roomType;
    private final int capacity;
    private final String description;
    private final BigDecimal pricePerDay;
    private final boolean occupied;
    private final HousekeepingStatus housekeepingStatus;

    public Room(
            int roomNumber,
            int floor,
            RoomType roomType,
            int capacity,
            String description,
            BigDecimal pricePerDay,
            boolean occupied,
            HousekeepingStatus housekeepingStatus) {
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.roomType = roomType;
        this.capacity = capacity;
        this.description = description;
        this.pricePerDay = pricePerDay;
        this.occupied = occupied;
        this.housekeepingStatus = housekeepingStatus;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public int getFloor() {
        return floor;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPricePerDay() {
        return pricePerDay;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public HousekeepingStatus getHousekeepingStatus() {
        return housekeepingStatus;
    }
}
