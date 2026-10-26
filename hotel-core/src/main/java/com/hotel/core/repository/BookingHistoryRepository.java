package com.hotel.core.repository;

import com.hotel.core.config.DatabaseConnection;
import com.hotel.core.model.BookingHistoryEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class BookingHistoryRepository {

    public List<BookingHistoryEntry> findBetween(LocalDate start, LocalDate endExclusive)
            throws SQLException {
        String sql = "SELECT reservation_id, reservation_reference, guest_name, room_number, "
                + "check_in, check_in_time, check_out, check_out_time, guest_count, "
                + "reservation_type, reservation_status, event_type, event_note, event_at "
                + "FROM booking_history_ledger WHERE event_at >= ? AND event_at < ? "
                + "ORDER BY event_at DESC, ledger_id DESC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(start.atStartOfDay()));
            statement.setTimestamp(2, Timestamp.valueOf(endExclusive.atStartOfDay()));
            try (ResultSet rows = statement.executeQuery()) {
                List<BookingHistoryEntry> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(new BookingHistoryEntry(
                            rows.getLong("reservation_id"),
                            rows.getString("reservation_reference"),
                            rows.getString("guest_name"),
                            rows.getInt("room_number"),
                            rows.getDate("check_in").toLocalDate(),
                            rows.getTime("check_in_time").toLocalTime(),
                            rows.getDate("check_out").toLocalDate(),
                            rows.getTime("check_out_time").toLocalTime(),
                            rows.getInt("guest_count"),
                            rows.getString("reservation_type"),
                            rows.getString("reservation_status"),
                            rows.getString("event_type"),
                            rows.getString("event_note"),
                            rows.getTimestamp("event_at").toLocalDateTime()));
                }
                return result;
            }
        }
    }

    static void insertSnapshot(Connection connection, long reservationId, String eventType, String note)
            throws SQLException {
        String sql = "INSERT INTO booking_history_ledger (reservation_id, reservation_reference, guest_name, "
                + "room_number, check_in, check_in_time, check_out, check_out_time, guest_count, "
                + "reservation_type, reservation_status, event_type, event_note, event_at, recorded_by) "
                + "SELECT r.id, r.reference, g.full_name, r.room_number, r.check_in, r.check_in_time, "
                + "r.check_out, r.check_out_time, r.guest_count, r.reservation_type, r.status, ?, ?, "
                + "CURRENT_TIMESTAMP, ? FROM reservations r JOIN guests g ON g.id = r.guest_id WHERE r.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            statement.setString(2, note);
            statement.setNull(3, java.sql.Types.INTEGER);
            statement.setLong(4, reservationId);
            statement.executeUpdate();
        }
    }

    static void insertSnapshot(
            Connection connection, long reservationId, String eventType, String note, int adminId)
            throws SQLException {
        String sql = "INSERT INTO booking_history_ledger (reservation_id, reservation_reference, guest_name, "
                + "room_number, check_in, check_in_time, check_out, check_out_time, guest_count, "
                + "reservation_type, reservation_status, event_type, event_note, event_at, recorded_by) "
                + "SELECT r.id, r.reference, g.full_name, r.room_number, r.check_in, r.check_in_time, "
                + "r.check_out, r.check_out_time, r.guest_count, r.reservation_type, r.status, ?, ?, "
                + "CURRENT_TIMESTAMP, ? FROM reservations r JOIN guests g ON g.id = r.guest_id WHERE r.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            statement.setString(2, note);
            statement.setInt(3, adminId);
            statement.setLong(4, reservationId);
            statement.executeUpdate();
        }
    }

    public void recordDispute(long reservationId, String reference, String note, int adminId)
            throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT id FROM reservations WHERE id = ? AND reference = ? FOR UPDATE")) {
                    lock.setLong(1, reservationId);
                    lock.setString(2, reference);
                    try (ResultSet result = lock.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Reservation was not found while recording the dispute.");
                        }
                    }
                }
                insertSnapshot(connection, reservationId, "Disputed", note, adminId);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        }
    }
}
