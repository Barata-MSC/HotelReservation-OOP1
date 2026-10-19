package com.hotel.core.repository;

import com.hotel.core.config.DatabaseConnection;
import com.hotel.core.exception.RoomNotFoundException;
import com.hotel.core.exception.RoomNotReadyException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.CheckoutReceipt;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.ReservationType;
import com.hotel.core.model.enums.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReservationRepository {

    private static final String RESERVATION_COLUMNS =
            "SELECT r.id, r.reference, r.guest_id, g.full_name, g.contact_number, g.email, "
                    + "g.classification, g.discount_id_type, g.discount_id_number, g.discount_verified, "
                    + "g.discount_id_expiry, g.discount_name_matches, "
                    + "r.room_number, rm.room_type, r.room_rate, r.check_in, r.check_out, r.nights, "
                    + "r.guest_count, r.reservation_type, r.status "
                    + "FROM reservations r JOIN guests g ON g.id = r.guest_id "
                    + "JOIN rooms rm ON rm.room_number = r.room_number";

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int guestCount)
            throws SQLException {
        String sql = "SELECT rm.room_number, rm.floor, rm.room_type, rm.capacity, rm.description, "
                + "rm.price_per_day, rm.is_occupied, rm.housekeeping_status FROM rooms rm "
                + "WHERE rm.capacity >= ? AND rm.is_occupied = FALSE "
                + "AND rm.housekeeping_status = 'Clean' "
                + "AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.room_number = rm.room_number "
                + "AND r.status IN ('Pending', 'Confirmed', 'Checked-In') "
                + "AND r.check_in < ? AND r.check_out > ?) ORDER BY rm.room_number";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, guestCount);
            statement.setDate(2, Date.valueOf(checkOut));
            statement.setDate(3, Date.valueOf(checkIn));
            return readRooms(statement);
        }
    }

    public Reservation create(
            Guest guest,
            int roomNumber,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount,
            ReservationType type,
            ReservationStatus initialStatus) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Room room = lockRoom(connection, roomNumber);
                ensureBookable(connection, room, roomNumber, guestCount, checkIn, checkOut, null);
                long guestId = insertGuest(connection, guest);
                BigDecimal roomRate = room.getPricePerDay();
                String reference = newReference("RES-");
                int nights = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut));
                String sql = "INSERT INTO reservations (reference, guest_id, room_number, room_rate, "
                        + "check_in, check_out, nights, guest_count, reservation_type, status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                long reservationId;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, reference);
                    statement.setLong(2, guestId);
                    statement.setInt(3, roomNumber);
                    statement.setBigDecimal(4, roomRate);
                    statement.setDate(5, Date.valueOf(checkIn));
                    statement.setDate(6, Date.valueOf(checkOut));
                    statement.setInt(7, nights);
                    statement.setInt(8, guestCount);
                    statement.setString(9, type.getDatabaseValue());
                    statement.setString(10, initialStatus.getDatabaseValue());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Database did not return a reservation id.");
                        }
                        reservationId = keys.getLong(1);
                    }
                }
                connection.commit();
                return new Reservation(
                        reservationId, reference, guestWithId(guest, guestId), roomNumber,
                        room.getRoomType().getDatabaseValue(), roomRate, checkIn, checkOut,
                        nights, guestCount, type, initialStatus);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public List<Reservation> findPendingOnline() throws SQLException {
        return findReservations("WHERE r.reservation_type = 'Online' AND r.status = 'Pending' "
                + "ORDER BY r.created_at, r.id");
    }

    public List<Reservation> findArrivals(LocalDate today) throws SQLException {
        return findReservations("WHERE r.status = 'Confirmed' AND r.check_in <= ? "
                + "AND r.check_out > ? ORDER BY r.check_in, r.reference", today, today);
    }

    public List<Reservation> findForCheckIn(String query) throws SQLException {
        return findReservations("WHERE r.reference = ? OR g.full_name LIKE ? "
                + "ORDER BY CASE r.status WHEN 'Pending' THEN 0 WHEN 'Confirmed' THEN 1 "
                + "WHEN 'Checked-In' THEN 2 ELSE 3 END, r.check_in DESC, r.reference",
                query, "%" + query + "%");
    }

    public List<Reservation> findAffectedByHousekeepingChange(int roomNumber, LocalDate today)
            throws SQLException {
        return findReservations("WHERE r.room_number = ? AND r.status IN ('Pending', 'Confirmed') "
                + "AND r.check_out > ? ORDER BY r.check_in, r.reference", roomNumber, today);
    }

    public List<Room> findReplacementRooms(String reference) throws SQLException {
        Reservation reservation = findByReference(reference);
        if (reservation == null) {
            throw new ValidationException("Reservation not found: " + reference);
        }
        String sql = "SELECT rm.room_number, rm.floor, rm.room_type, rm.capacity, rm.description, "
                + "rm.price_per_day, rm.is_occupied, rm.housekeeping_status FROM rooms rm "
                + "WHERE rm.room_number <> ? AND rm.capacity >= ? AND rm.is_occupied = FALSE "
                + "AND rm.housekeeping_status = 'Clean' "
                + "AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.room_number = rm.room_number "
                + "AND r.status IN ('Pending', 'Confirmed', 'Checked-In') "
                + "AND r.check_in < ? AND r.check_out > ?) ORDER BY rm.room_number";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, reservation.roomNumber());
            statement.setInt(2, reservation.guestCount());
            statement.setDate(3, Date.valueOf(reservation.checkOut()));
            statement.setDate(4, Date.valueOf(reservation.checkIn()));
            return readRooms(statement);
        }
    }

    public List<Reservation> findCheckedIn() throws SQLException {
        return findReservations("WHERE r.status = 'Checked-In' ORDER BY r.check_out, r.reference");
    }

    public Reservation findByReference(String reference) throws SQLException {
        List<Reservation> reservations = findReservations("WHERE r.reference = ?", reference);
        return reservations.isEmpty() ? null : reservations.get(0);
    }

    public Reservation reviewOnline(String reference, boolean confirm) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.type() != ReservationType.ONLINE
                        || reservation.status() != ReservationStatus.PENDING) {
                    throw new ValidationException("Only pending online reservations can be reviewed.");
                }
                if (confirm) {
                    Room room = lockRoom(connection, reservation.roomNumber());
                    ensureBookable(connection, room, reservation.roomNumber(), reservation.guestCount(),
                            reservation.checkIn(), reservation.checkOut(), reference);
                }
                ReservationStatus status = confirm
                        ? ReservationStatus.CONFIRMED : ReservationStatus.CANCELLED;
                updateReservationStatus(connection, reservation.id(), status);
                connection.commit();
                return withStatus(reservation, status);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public Reservation checkIn(
            String reference,
            LocalDate today,
            Integer replacementRoomNumber,
            boolean discountVerifiedAtCheckIn) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.status() != ReservationStatus.CONFIRMED
                        && reservation.status() != ReservationStatus.PENDING) {
                    throw new ValidationException(
                            "Reservation status is " + reservation.status().getDatabaseValue()
                                    + "; only Pending or Confirmed reservations can be checked in.");
                }
                if (today.isBefore(reservation.checkIn())) {
                    throw new ValidationException("The reservation check-in date has not arrived.");
                }
                if (!today.isBefore(reservation.checkOut())) {
                    throw new ValidationException("The reservation check-out date has passed.");
                }
                int roomNumber = replacementRoomNumber == null
                        ? reservation.roomNumber() : replacementRoomNumber;
                Room room = lockRoom(connection, roomNumber);
                try {
                    ensureBookable(connection, room, roomNumber, reservation.guestCount(),
                            reservation.checkIn(), reservation.checkOut(), reference);
                } catch (ValidationException exception) {
                    if (replacementRoomNumber == null) {
                        throw new RoomNotReadyException(exception.getMessage());
                    }
                    throw exception;
                }
                if (reservation.status() == ReservationStatus.PENDING) {
                    updateReservationStatus(connection, reservation.id(), ReservationStatus.CONFIRMED);
                }
                if (roomNumber != reservation.roomNumber()) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE reservations SET room_number = ?, room_rate = ? WHERE id = ?")) {
                        statement.setInt(1, roomNumber);
                        statement.setBigDecimal(2, room.getPricePerDay());
                        statement.setLong(3, reservation.id());
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Reservation room was not updated.");
                        }
                    }
                }
                Guest guest = reservation.guest();
                if (guest.classification() == GuestClassification.DISCOUNTED
                        && (!discountVerifiedAtCheckIn || !guest.discountVerified()
                        || !guest.discountNameMatches() || guest.discountIdExpiry() == null
                        || guest.discountIdExpiry().isBefore(today)
                        || guest.discountIdType() == null || guest.discountIdType().isBlank()
                        || guest.discountIdNumber() == null || guest.discountIdNumber().isBlank())) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "UPDATE guests SET classification = 'Regular', discount_id_type = NULL, "
                                    + "discount_id_number = NULL, discount_verified = FALSE, "
                                    + "discount_id_expiry = NULL, discount_name_matches = FALSE "
                                    + "WHERE id = ?")) {
                        statement.setLong(1, guest.id());
                        if (statement.executeUpdate() != 1) {
                            throw new SQLException("Guest classification was not updated.");
                        }
                    }
                    guest = regularGuest(guest);
                }
                updateReservationStatus(connection, reservation.id(), ReservationStatus.CHECKED_IN);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET is_occupied = TRUE WHERE room_number = ?")) {
                    statement.setInt(1, roomNumber);
                    if (statement.executeUpdate() != 1) {
                        throw new RoomNotFoundException(reservation.roomNumber());
                    }
                }
                connection.commit();
                return withStatus(reservation, ReservationStatus.CHECKED_IN, roomNumber, room, guest);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public CheckoutReceipt checkOut(
            String reference,
            BigDecimal cashTendered,
            CheckoutBill bill) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.status() != ReservationStatus.CHECKED_IN) {
                    throw new ValidationException("Only checked-in reservations can be checked out.");
                }
                if (cashTendered.compareTo(bill.total()) < 0) {
                    throw new ValidationException("Cash tendered is less than the amount due.");
                }
                Room room = lockRoom(connection, reservation.roomNumber());
                BigDecimal change = cashTendered.subtract(bill.total()).setScale(2, RoundingMode.HALF_UP);
                String receiptReference = newReference("RCT-");
                LocalDateTime paidAt = LocalDateTime.now();
                String sql = "INSERT INTO transactions (reservation_id, receipt_reference, room_rate, nights, "
                        + "room_cost, discount_amount, tax_amount, total_amount, cash_tendered, "
                        + "change_amount, payment_status, paid_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Paid', ?)";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, reservation.id());
                    statement.setString(2, receiptReference);
                    statement.setBigDecimal(3, bill.roomRate());
                    statement.setInt(4, bill.nights());
                    statement.setBigDecimal(5, bill.roomCost());
                    statement.setBigDecimal(6, bill.discountAmount());
                    statement.setBigDecimal(7, bill.taxAmount());
                    statement.setBigDecimal(8, bill.total());
                    statement.setBigDecimal(9, cashTendered);
                    statement.setBigDecimal(10, change);
                    statement.setTimestamp(11, Timestamp.valueOf(paidAt));
                    statement.executeUpdate();
                }
                updateReservationStatus(connection, reservation.id(), ReservationStatus.CHECKED_OUT);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET is_occupied = FALSE, housekeeping_status = 'Dirty' "
                                + "WHERE room_number = ?")) {
                    statement.setInt(1, reservation.roomNumber());
                    if (statement.executeUpdate() != 1) {
                        throw new RoomNotFoundException(reservation.roomNumber());
                    }
                }
                connection.commit();
                return new CheckoutReceipt(receiptReference, reservation, bill,
                        cashTendered, change, paidAt);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private List<Reservation> findReservations(String suffix, Object... parameters) throws SQLException {
        String sql = RESERVATION_COLUMNS + " " + suffix;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (resultSet.next()) {
                    reservations.add(mapReservation(resultSet));
                }
                return reservations;
            }
        }
    }

    private static Reservation lockReservation(Connection connection, String reference) throws SQLException {
        String sql = RESERVATION_COLUMNS + " WHERE r.reference = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reference);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ValidationException("Reservation not found: " + reference);
                }
                return mapReservation(resultSet);
            }
        }
    }

    private static Room lockRoom(Connection connection, int roomNumber) throws SQLException {
        String sql = "SELECT room_number, floor, room_type, capacity, description, price_per_day, "
                + "is_occupied, housekeeping_status FROM rooms WHERE room_number = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RoomNotFoundException(roomNumber);
                }
                return mapRoom(resultSet);
            }
        }
    }

    private static void ensureBookable(
            Connection connection,
            Room room,
            int roomNumber,
            int guestCount,
            LocalDate checkIn,
            LocalDate checkOut,
            String excludedReference) throws SQLException {
        if (room.getCapacity() < guestCount) {
            throw new ValidationException("Room " + roomNumber + " does not have enough guest capacity.");
        }
        if (room.isOccupied()) {
            throw new ValidationException("Room " + roomNumber + " is already occupied.");
        }
        if (room.getHousekeepingStatus() != HousekeepingStatus.CLEAN) {
            throw new ValidationException("Room " + roomNumber + " is not clean and available.");
        }
        if (hasOverlappingReservation(connection, roomNumber, checkIn, checkOut, excludedReference)) {
            throw new ValidationException("Room " + roomNumber + " is already reserved for those dates.");
        }
    }

    private static boolean hasOverlappingReservation(
            Connection connection,
            int roomNumber,
            LocalDate checkIn,
            LocalDate checkOut,
            String excludedReference) throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? "
                + "AND status IN ('Pending', 'Confirmed', 'Checked-In') "
                + "AND check_in < ? AND check_out > ?";
        if (excludedReference != null) {
            sql += " AND reference <> ?";
        }
        sql += " LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            statement.setDate(2, Date.valueOf(checkOut));
            statement.setDate(3, Date.valueOf(checkIn));
            if (excludedReference != null) {
                statement.setString(4, excludedReference);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static long insertGuest(Connection connection, Guest guest) throws SQLException {
        String sql = "INSERT INTO guests (full_name, contact_number, email, classification, "
                + "discount_id_type, discount_id_number, discount_verified, discount_id_expiry, "
                + "discount_name_matches) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, guest.fullName());
            statement.setString(2, guest.contactNumber());
            statement.setString(3, guest.email());
            statement.setString(4, guest.classification().getDatabaseValue());
            statement.setString(5, guest.discountIdType());
            statement.setString(6, guest.discountIdNumber());
            statement.setBoolean(7, guest.discountVerified());
            if (guest.discountIdExpiry() == null) {
                statement.setNull(8, java.sql.Types.DATE);
            } else {
                statement.setDate(8, Date.valueOf(guest.discountIdExpiry()));
            }
            statement.setBoolean(9, guest.discountNameMatches());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Database did not return a guest id.");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void updateReservationStatus(
            Connection connection, long reservationId, ReservationStatus status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?")) {
            statement.setString(1, status.getDatabaseValue());
            statement.setLong(2, reservationId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Reservation status was not updated.");
            }
        }
    }

    private static Reservation mapReservation(ResultSet resultSet) throws SQLException {
        Guest guest = new Guest(
                resultSet.getLong("guest_id"),
                resultSet.getString("full_name"),
                resultSet.getString("contact_number"),
                resultSet.getString("email"),
                GuestClassification.fromDatabaseValue(resultSet.getString("classification")),
                resultSet.getString("discount_id_type"),
                resultSet.getString("discount_id_number"),
                resultSet.getBoolean("discount_verified"),
                resultSet.getDate("discount_id_expiry") == null
                        ? null : resultSet.getDate("discount_id_expiry").toLocalDate(),
                resultSet.getBoolean("discount_name_matches"));
        return new Reservation(
                resultSet.getLong("id"),
                resultSet.getString("reference"),
                guest,
                resultSet.getInt("room_number"),
                resultSet.getString("room_type"),
                resultSet.getBigDecimal("room_rate"),
                resultSet.getDate("check_in").toLocalDate(),
                resultSet.getDate("check_out").toLocalDate(),
                resultSet.getInt("nights"),
                resultSet.getInt("guest_count"),
                ReservationType.fromDatabaseValue(resultSet.getString("reservation_type")),
                ReservationStatus.fromDatabaseValue(resultSet.getString("status")));
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

    private static List<Room> readRooms(PreparedStatement statement) throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                rooms.add(mapRoom(resultSet));
            }
        }
        return rooms;
    }

    private static void bindParameters(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int index = 0; index < parameters.length; index++) {
            Object parameter = parameters[index];
            if (parameter instanceof LocalDate date) {
                statement.setDate(index + 1, Date.valueOf(date));
            } else {
                statement.setObject(index + 1, parameter);
            }
        }
    }

    private static Reservation withStatus(Reservation reservation, ReservationStatus status) {
        return new Reservation(
                reservation.id(), reservation.reference(), reservation.guest(),
                reservation.roomNumber(), reservation.roomType(), reservation.roomRate(),
                reservation.checkIn(), reservation.checkOut(), reservation.nights(),
                reservation.guestCount(), reservation.type(), status);
    }

    private static Reservation withStatus(
            Reservation reservation,
            ReservationStatus status,
            int roomNumber,
            Room room,
            Guest guest) {
        return new Reservation(
                reservation.id(), reservation.reference(), guest, roomNumber,
                room.getRoomType().getDatabaseValue(), room.getPricePerDay(),
                reservation.checkIn(), reservation.checkOut(), reservation.nights(),
                reservation.guestCount(), reservation.type(), status);
    }

    private static Guest guestWithId(Guest guest, long id) {
        return new Guest(id, guest.fullName(), guest.contactNumber(), guest.email(),
                guest.classification(), guest.discountIdType(), guest.discountIdNumber(),
                guest.discountVerified(), guest.discountIdExpiry(), guest.discountNameMatches());
    }

    private static Guest regularGuest(Guest guest) {
        return new Guest(guest.id(), guest.fullName(), guest.contactNumber(), guest.email(),
                GuestClassification.REGULAR, null, null, false, null, false);
    }

    private static String newReference(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase(java.util.Locale.ROOT);
    }

    private static void rollback(Connection connection, Exception failure) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
