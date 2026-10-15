package com.hotel.core.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(int roomNumber) {
        super("Room not found: " + roomNumber);
    }
}
