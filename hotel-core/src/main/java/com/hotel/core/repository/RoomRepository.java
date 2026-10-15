package com.hotel.core.repository;

import com.hotel.core.config.DatabaseConnection;
import com.hotel.core.exception.RoomNotFoundException;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.RoomType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RoomRepository {

    private static final String SELECT_COLUMNS =
            "SELECT room_number, floor, room_type, capacity, description, "
                    + "price_per_day, is_occupied, housekeeping_status FROM rooms";

    public void add(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (room_number, floor, room_type, capacity, description, "
                + "price_per_day, is_occupied, housekeeping_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindRoom(statement, room);
            statement.executeUpdate();
        }
    }

    public void update(Room room) throws SQLException {
        findByNumber(room.getRoomNumber());
        String sql = "UPDATE rooms SET floor = ?, room_type = ?, capacity = ?, description = ?, "
                + "price_per_day = ?, is_occupied = ?, housekeeping_status = ? WHERE room_number = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, room.getFloor());
            statement.setString(2, room.getRoomType().getDatabaseValue());
            statement.setInt(3, room.getCapacity());
            statement.setString(4, room.getDescription());
            statement.setBigDecimal(5, room.getPricePerDay());
            statement.setBoolean(6, room.isOccupied());
            statement.setString(7, room.getHousekeepingStatus().getDatabaseValue());
            statement.setInt(8, room.getRoomNumber());
            statement.executeUpdate();
        }
    }

    public Room findByNumber(int roomNumber) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE room_number = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRoom(resultSet);
                }
            }
        }
        throw new RoomNotFoundException(roomNumber);
    }

    public boolean existsByNumber(int roomNumber) throws SQLException {
        String sql = "SELECT 1 FROM rooms WHERE room_number = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public List<Room> search(Integer roomNumber, RoomType roomType) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS);
        if (roomNumber != null && roomType != null) {
            sql.append(" WHERE room_number = ? OR room_type = ?");
        } else if (roomNumber != null) {
            sql.append(" WHERE room_number = ?");
        } else if (roomType != null) {
            sql.append(" WHERE room_type = ?");
        }

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            if (roomNumber != null) {
                statement.setInt(parameter++, roomNumber);
            }
            if (roomType != null) {
                statement.setString(parameter, roomType.getDatabaseValue());
            }
            return readRooms(statement);
        }
    }

    public List<Room> findAll() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_COLUMNS)) {
            return readRooms(statement);
        }
    }

    public void setHousekeepingStatus(int roomNumber, HousekeepingStatus status) throws SQLException {
        findByNumber(roomNumber);
        String sql = "UPDATE rooms SET housekeeping_status = ? WHERE room_number = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status.getDatabaseValue());
            statement.setInt(2, roomNumber);
            statement.executeUpdate();
        }
    }

    public void setOccupied(int roomNumber, boolean occupied) throws SQLException {
        findByNumber(roomNumber);
        String sql = "UPDATE rooms SET is_occupied = ? WHERE room_number = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, occupied);
            statement.setInt(2, roomNumber);
            statement.executeUpdate();
        }
    }

    public boolean hasCheckedInGuest(int roomNumber) throws SQLException {
        String sql = "SELECT 1 FROM reservations "
                + "WHERE room_number = ? AND status = 'Checked-In' LIMIT 1";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static void bindRoom(PreparedStatement statement, Room room) throws SQLException {
        statement.setInt(1, room.getRoomNumber());
        statement.setInt(2, room.getFloor());
        statement.setString(3, room.getRoomType().getDatabaseValue());
        statement.setInt(4, room.getCapacity());
        statement.setString(5, room.getDescription());
        statement.setBigDecimal(6, room.getPricePerDay());
        statement.setBoolean(7, room.isOccupied());
        statement.setString(8, room.getHousekeepingStatus().getDatabaseValue());
    }

    private static List<Room> readRooms(PreparedStatement statement) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rooms.add(mapRoom(resultSet));
            }
        }
        return rooms;
    }

    private static Room mapRoom(ResultSet resultSet) throws SQLException {
        return new Room(
                resultSet.getInt("room_number"),
                resultSet.getInt("floor"),
                RoomType.fromDatabaseValue(resultSet.getString("room_type")),
                resultSet.getInt("capacity"),
                resultSet.getString("description"),
                resultSet.getBigDecimal("price_per_day"),
                resultSet.getBoolean("is_occupied"),
                HousekeepingStatus.fromDatabaseValue(resultSet.getString("housekeeping_status")));
    }
}
