package com.hotel.core.repository;

import com.hotel.core.config.DatabaseConnection;
import com.hotel.core.exception.RoomNotFoundException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.CheckoutReceipt;
import com.hotel.core.model.AdminPaymentRecord;
import com.hotel.core.model.AdminReservationData;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.CustomerBookingData;
import com.hotel.core.model.CustomerBookingRequest;
import com.hotel.core.model.CustomerNotification;
import com.hotel.core.model.CustomerPaymentSubmission;
import com.hotel.core.model.WalkInReceipt;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.StayExtensionQuote;
import com.hotel.core.model.enums.CustomerPaymentKind;
import com.hotel.core.model.enums.CustomerPaymentStatus;
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
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReservationRepository {

    private static final String RESERVATION_COLUMNS =
            "SELECT r.id, r.reference, r.guest_id, g.full_name, g.contact_number, g.email, "
                    + "g.classification, g.discount_id_type, g.discount_id_number, "
                    + "g.discount_id_proof_path, g.discount_verified, "
                    + "r.room_number, rm.room_type, r.room_rate, r.check_in, r.check_out, r.nights, "
                    + "r.guest_count, r.reservation_type, r.status "
                    + "FROM reservations r JOIN guests g ON g.id = r.guest_id "
                    + "JOIN rooms rm ON rm.room_number = r.room_number";
    private static final String CUSTOMER_BOOKING_COLUMNS =
            "SELECT r.id, r.reference, r.guest_id, g.full_name, g.contact_number, g.email, "
                    + "g.classification, g.discount_id_type, g.discount_id_number, "
                    + "g.discount_id_proof_path, g.discount_verified, "
                    + "r.room_number, rm.room_type, r.room_rate, r.check_in, r.check_out, r.nights, "
                    + "r.guest_count, r.reservation_type, r.status, r.check_in_time, r.check_out_time, "
                    + "r.payment_status, "
                    + "(SELECT COALESCE(SUM(p.amount), 0) FROM payments p WHERE p.reservation_id = r.id) "
                    + "AS amount_submitted, "
                    + "(SELECT GROUP_CONCAT(DISTINCT p.payment_method ORDER BY p.payment_method "
                    + "SEPARATOR ', ') FROM payments p WHERE p.reservation_id = r.id) AS payment_methods "
                    + "FROM reservations r JOIN guests g ON g.id = r.guest_id "
                    + "JOIN rooms rm ON rm.room_number = r.room_number";

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int guestCount)
            throws SQLException {
        return findAvailableRooms(checkIn.atTime(15, 0), checkOut.atTime(11, 0), guestCount);
    }

    public List<Room> findAvailableRooms(LocalDateTime checkIn, LocalDateTime checkOut, int guestCount)
            throws SQLException {
        String sql = "SELECT rm.room_number, rm.floor, rm.room_type, rm.capacity, rm.description, "
                + "rm.price_per_day, rm.is_occupied, rm.housekeeping_status FROM rooms rm "
                + "WHERE rm.capacity >= ? AND rm.housekeeping_status = 'Clean' "
                + "AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.room_number = rm.room_number "
                + "AND r.status IN ('Pending', 'Confirmed', 'Checked-In') "
                + "AND TIMESTAMP(r.check_in, r.check_in_time) < ? "
                + "AND TIMESTAMP(r.check_out, r.check_out_time) > ?) ORDER BY rm.room_number";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, guestCount);
            statement.setTimestamp(2, Timestamp.valueOf(checkOut.plusHours(1)));
            statement.setTimestamp(3, Timestamp.valueOf(checkIn.minusHours(1)));
            return readRooms(statement);
        }
    }

    private static void insertNotificationForReservation(
            Connection connection, long reservationId, String type, String message) throws SQLException {
        String sql = "INSERT INTO notifications "
                + "(customer_user_id, reservation_id, notification_type, message) "
                + "SELECT customer_user_id, id, ?, ? FROM reservations "
                + "WHERE id = ? AND customer_user_id IS NOT NULL";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, type);
            statement.setString(2, message);
            statement.setLong(3, reservationId);
            statement.executeUpdate();
        }
    }

    public List<Room> findAvailableRooms(
            LocalDate checkIn, LocalDate checkOut, int guestCount, RoomType roomType) throws SQLException {
        return findAvailableRooms(checkIn, checkOut, guestCount, roomType, null);
    }

    public List<Room> findAvailableRooms(
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount,
            RoomType roomType,
            String excludedReference) throws SQLException {
        return findAvailableRooms(checkIn.atTime(15, 0), checkOut.atTime(11, 0),
                guestCount, roomType, excludedReference);
    }

    public List<Room> findAvailableRooms(
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            int guestCount,
            RoomType roomType,
            String excludedReference) throws SQLException {
        if (roomType == null) {
            throw new ValidationException("Room type is required.");
        }
        String sql = "SELECT rm.room_number, rm.floor, rm.room_type, rm.capacity, rm.description, "
                + "rm.price_per_day, rm.is_occupied, rm.housekeeping_status FROM rooms rm "
                + "WHERE rm.capacity >= ? AND rm.room_type = ? "
                + "AND rm.housekeeping_status = 'Clean' "
                + "AND NOT EXISTS (SELECT 1 FROM reservations r WHERE r.room_number = rm.room_number "
                + "AND r.status IN ('Pending', 'Confirmed', 'Checked-In') "
                + "AND TIMESTAMP(r.check_in, r.check_in_time) < ? "
                + "AND TIMESTAMP(r.check_out, r.check_out_time) > ?";
        if (excludedReference != null) {
            sql += " AND r.reference <> ?";
        }
        sql += ") ORDER BY rm.room_number";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, guestCount);
            statement.setString(2, roomType.getDatabaseValue());
            statement.setTimestamp(3, Timestamp.valueOf(checkOut.plusHours(1)));
            statement.setTimestamp(4, Timestamp.valueOf(checkIn.minusHours(1)));
            if (excludedReference != null) {
                statement.setString(5, excludedReference);
            }
            return readRooms(statement);
        }
    }

    public CustomerBooking createCustomerOnline(
            int customerUserId,
            CustomerBookingRequest request,
            BigDecimal expectedRoomRate,
            BigDecimal total,
            BigDecimal submittedAmount,
            CustomerPaymentSubmission payment,
            String proofPath) throws SQLException {
        BigDecimal minimumDownpayment = total.multiply(new BigDecimal("0.50"))
                .setScale(2, RoundingMode.HALF_UP);
        if (submittedAmount == null || submittedAmount.compareTo(minimumDownpayment) < 0) {
            throw new ValidationException("The minimum online downpayment is PHP "
                    + minimumDownpayment.toPlainString() + ".");
        }
        if (submittedAmount.compareTo(total) > 0) {
            throw new ValidationException("Payment cannot exceed the booking total.");
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Room room = lockRoom(connection, request.roomNumber());
                ensureCustomerRoomBookable(connection, room, request, null, expectedRoomRate);
                Guest guest = request.guest();
                long guestId = insertGuest(connection, guest);
                String reference = newReference("ONL-");
                int nights = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(
                        request.checkIn().toLocalDate(), request.checkOut().toLocalDate()));
                String sql = "INSERT INTO reservations (reference, guest_id, customer_user_id, room_number, "
                        + "room_rate, check_in, check_in_time, check_out, check_out_time, nights, guest_count, "
                        + "reservation_type, status, payment_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Online', 'Pending', 'Partially Paid')";
                long reservationId;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, reference);
                    statement.setLong(2, guestId);
                    statement.setInt(3, customerUserId);
                    statement.setInt(4, request.roomNumber());
                    statement.setBigDecimal(5, room.getPricePerDay());
                    statement.setDate(6, Date.valueOf(request.checkIn().toLocalDate()));
                    statement.setTime(7, Time.valueOf(request.checkIn().toLocalTime()));
                    statement.setDate(8, Date.valueOf(request.checkOut().toLocalDate()));
                    statement.setTime(9, Time.valueOf(request.checkOut().toLocalTime()));
                    statement.setInt(10, nights);
                    statement.setInt(11, request.guestCount());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Database did not return a reservation id.");
                        }
                        reservationId = keys.getLong(1);
                    }
                }
                insertCustomerPayment(connection, reservationId, CustomerPaymentKind.DOWNPAYMENT,
                        payment, submittedAmount, proofPath);
                connection.commit();
                Reservation reservation = new Reservation(
                        reservationId, reference, guestWithId(guest, guestId), request.roomNumber(),
                        room.getRoomType().getDatabaseValue(), room.getPricePerDay(),
                        request.checkIn().toLocalDate(), request.checkOut().toLocalDate(), nights,
                        request.guestCount(), ReservationType.ONLINE, ReservationStatus.PENDING);
                return new CustomerBooking(
                        reservation, request.checkIn().toLocalTime(), request.checkOut().toLocalTime(),
                        CustomerPaymentStatus.PARTIALLY_PAID, total, submittedAmount,
                        total.subtract(submittedAmount), payment.method().getDatabaseValue());
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public void updateCustomerPending(
            int customerUserId,
            String reference,
            CustomerBookingRequest request,
            BigDecimal expectedRoomRate,
            BigDecimal updatedTotal) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation current = lockCustomerReservation(connection, customerUserId, reference);
                if (current.status() != ReservationStatus.PENDING) {
                    throw new ValidationException("Only pending reservations can be changed.");
                }
                Room room = lockRoom(connection, request.roomNumber());
                ensureCustomerRoomBookable(connection, room, request, reference, expectedRoomRate);
                if (sumSubmittedPayments(connection, current.id()).compareTo(updatedTotal) > 0) {
                    throw new ValidationException(
                            "The updated booking total cannot be less than payments already submitted.");
                }
                updateGuest(connection, current.guest().id(), request.guest());
                int nights = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(
                        request.checkIn().toLocalDate(), request.checkOut().toLocalDate()));
                String sql = "UPDATE reservations SET room_number = ?, room_rate = ?, check_in = ?, "
                        + "check_in_time = ?, check_out = ?, check_out_time = ?, nights = ?, guest_count = ? "
                        + "WHERE id = ? AND customer_user_id = ? AND status = 'Pending'";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, request.roomNumber());
                    statement.setBigDecimal(2, room.getPricePerDay());
                    statement.setDate(3, Date.valueOf(request.checkIn().toLocalDate()));
                    statement.setTime(4, Time.valueOf(request.checkIn().toLocalTime()));
                    statement.setDate(5, Date.valueOf(request.checkOut().toLocalDate()));
                    statement.setTime(6, Time.valueOf(request.checkOut().toLocalTime()));
                    statement.setInt(7, nights);
                    statement.setInt(8, request.guestCount());
                    statement.setLong(9, current.id());
                    statement.setInt(10, customerUserId);
                    if (statement.executeUpdate() != 1) {
                        throw new ValidationException("Only pending reservations can be changed.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public void cancelCustomerPending(int customerUserId, String reference) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockCustomerReservation(connection, customerUserId, reference);
                if (reservation.status() != ReservationStatus.PENDING) {
                    throw new ValidationException("Only pending reservations can be cancelled.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE reservations SET status = 'Cancelled' "
                                + "WHERE id = ? AND customer_user_id = ? AND status = 'Pending'")) {
                    statement.setLong(1, reservation.id());
                    statement.setInt(2, customerUserId);
                    if (statement.executeUpdate() != 1) {
                        throw new ValidationException("Only pending reservations can be cancelled.");
                    }
                }
                BookingHistoryRepository.insertSnapshot(connection, reservation.id(), "Cancelled",
                        "Reservation cancelled by the customer.");
                insertNotification(connection, customerUserId, reservation.id(),
                        "ReservationCancelled", "Your reservation " + reference + " was cancelled.");
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public void submitCustomerPayment(
            int customerUserId,
            String reference,
            CustomerPaymentKind kind,
            BigDecimal amount,
            BigDecimal bookingTotal,
            CustomerPaymentSubmission payment,
            String proofPath) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockCustomerReservation(connection, customerUserId, reference);
                if (reservation.status() == ReservationStatus.CANCELLED
                        || reservation.status() == ReservationStatus.CHECKED_OUT) {
                    throw new ValidationException("Payments cannot be submitted for this reservation.");
                }
                BigDecimal submitted = sumSubmittedPayments(connection, reservation.id());
                if (amount == null || amount.signum() <= 0
                        || submitted.add(amount).compareTo(bookingTotal) > 0) {
                    throw new ValidationException("Payment exceeds the remaining booking balance.");
                }
                BigDecimal remaining = bookingTotal.subtract(submitted).max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);
                if (kind == CustomerPaymentKind.BALANCE && amount.compareTo(remaining) != 0) {
                    throw new ValidationException("Pay the full remaining balance of PHP "
                            + remaining.toPlainString() + ".");
                }
                insertCustomerPayment(connection, reservation.id(), kind, payment, amount, proofPath);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public List<CustomerBookingData> findCustomerBookings(int customerUserId) throws SQLException {
        String sql = CUSTOMER_BOOKING_COLUMNS
                + " WHERE r.customer_user_id = ? ORDER BY r.created_at DESC, r.id DESC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CustomerBookingData> bookings = new ArrayList<>();
                while (resultSet.next()) {
                    bookings.add(mapCustomerBookingData(resultSet));
                }
                return bookings;
            }
        }
    }

    public CustomerBookingData findCustomerBooking(int customerUserId, String reference) throws SQLException {
        String sql = CUSTOMER_BOOKING_COLUMNS + " WHERE r.customer_user_id = ? AND r.reference = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerUserId);
            statement.setString(2, reference);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapCustomerBookingData(resultSet) : null;
            }
        }
    }

    public List<CustomerNotification> findCustomerNotifications(int customerUserId) throws SQLException {
        String sql = "SELECT n.id, n.notification_type, n.message, n.created_at, n.read_at, r.reference "
                + "FROM notifications n LEFT JOIN reservations r ON r.id = n.reservation_id "
                + "WHERE n.customer_user_id = ? ORDER BY n.created_at DESC, n.id DESC";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CustomerNotification> notifications = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp readAt = resultSet.getTimestamp("read_at");
                    notifications.add(new CustomerNotification(
                            resultSet.getLong("id"),
                            resultSet.getString("notification_type"),
                            resultSet.getString("message"),
                            resultSet.getTimestamp("created_at").toLocalDateTime(),
                            readAt != null,
                            resultSet.getString("reference")));
                }
                return notifications;
            }
        }
    }

    public int countUnreadCustomerNotifications(int customerUserId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM notifications WHERE customer_user_id = ? AND read_at IS NULL")) {
            statement.setInt(1, customerUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

            public List<AdminReservationData> findAdminReservations() throws SQLException {
                String sql = CUSTOMER_BOOKING_COLUMNS
                        + " ORDER BY r.created_at DESC, r.id DESC";
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql);
                     ResultSet resultSet = statement.executeQuery()) {
                    List<AdminReservationData> reservations = new ArrayList<>();
                    while (resultSet.next()) {
                        CustomerBookingData booking = mapCustomerBookingData(resultSet);
                        List<AdminPaymentRecord> payments = findAdminPayments(connection, booking.reservation().id());
                        reservations.add(new AdminReservationData(booking, payments));
                    }
                    return reservations;
                }
            }

            public List<AdminReservationData> findAdminCalendarReservations(
                    LocalDate monthStart, LocalDate monthEnd) throws SQLException {
                String sql = CUSTOMER_BOOKING_COLUMNS
                        + " WHERE r.check_in <= ? AND r.check_out >= ? "
                        + "ORDER BY r.check_in, r.check_in_time, r.room_number";
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setDate(1, Date.valueOf(monthEnd));
                    statement.setDate(2, Date.valueOf(monthStart));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<AdminReservationData> reservations = new ArrayList<>();
                        while (resultSet.next()) {
                            CustomerBookingData booking = mapCustomerBookingData(resultSet);
                            List<AdminPaymentRecord> payments = findAdminPayments(connection, booking.reservation().id());
                            reservations.add(new AdminReservationData(booking, payments));
                        }
                        return reservations;
                    }
                }
            }

            public List<AdminReservationData> findRecentAdminReservations(int limit) throws SQLException {
                int boundedLimit = Math.max(1, Math.min(limit, 50));
                String sql = CUSTOMER_BOOKING_COLUMNS
                        + " ORDER BY r.created_at DESC, r.id DESC LIMIT ?";
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, boundedLimit);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<AdminReservationData> reservations = new ArrayList<>();
                        while (resultSet.next()) {
                            CustomerBookingData booking = mapCustomerBookingData(resultSet);
                            List<AdminPaymentRecord> payments = findAdminPayments(connection, booking.reservation().id());
                            reservations.add(new AdminReservationData(booking, payments));
                        }
                        return reservations;
                    }
                }
            }

            public int countPendingOnlineReservations() throws SQLException {
                return countReservations(
                        "SELECT COUNT(*) FROM reservations WHERE reservation_type = 'Online' AND status = 'Pending'");
            }

            public int countArrivals(LocalDate date) throws SQLException {
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT COUNT(*) FROM reservations WHERE check_in = ? "
                                     + "AND status IN ('Confirmed', 'Checked-In')")) {
                    statement.setDate(1, Date.valueOf(date));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        return resultSet.getInt(1);
                    }
                }
            }

            public int countDepartures(LocalDate date) throws SQLException {
                try (Connection connection = DatabaseConnection.getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "SELECT COUNT(*) FROM reservations WHERE check_out = ? AND status = 'Checked-In'")) {
                    statement.setDate(1, Date.valueOf(date));
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        return resultSet.getInt(1);
                    }
                }
            }

            public void verifyOnlinePayment(
                    String reference, long paymentId, int verifiedBy, BigDecimal totalDue) throws SQLException {
                try (Connection connection = DatabaseConnection.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        Reservation reservation = lockReservation(connection, reference);
                        if (reservation.type() != ReservationType.ONLINE) {
                            throw new ValidationException("Only online reservation payments can be verified here.");
                        }
                        String paymentType;
                        BigDecimal paymentAmount;
                        try (PreparedStatement statement = connection.prepareStatement(
                                "SELECT payment_type, amount FROM payments "
                                        + "WHERE id = ? AND reservation_id = ? AND is_verified = FALSE FOR UPDATE")) {
                            statement.setLong(1, paymentId);
                            statement.setLong(2, reservation.id());
                            try (ResultSet resultSet = statement.executeQuery()) {
                                if (!resultSet.next()) {
                                    throw new ValidationException("This payment is already verified or no longer exists.");
                                }
                                paymentType = resultSet.getString(1);
                                paymentAmount = resultSet.getBigDecimal(2);
                            }
                        }
                        try (PreparedStatement statement = connection.prepareStatement(
                                "UPDATE payments SET is_verified = TRUE, verified_by = ?, "
                                        + "verified_at = CURRENT_TIMESTAMP WHERE id = ? AND reservation_id = ? "
                                        + "AND is_verified = FALSE")) {
                            statement.setInt(1, verifiedBy);
                            statement.setLong(2, paymentId);
                            statement.setLong(3, reservation.id());
                            if (statement.executeUpdate() != 1) {
                                throw new ValidationException("This payment could not be verified.");
                            }
                        }
                        updatePaymentStatus(connection, reservation.id(), totalDue);
                        BookingHistoryRepository.insertSnapshot(connection, reservation.id(), "PaymentVerified",
                                paymentType + " payment verified: PHP " + paymentAmount.toPlainString() + ".",
                                verifiedBy);
                        insertNotificationForReservation(
                                connection, reservation.id(), "PaymentVerified",
                                "Your " + paymentType.toLowerCase(java.util.Locale.ROOT)
                                        + " payment for reservation " + reference + " has been verified.");
                        connection.commit();
                    } catch (SQLException | RuntimeException exception) {
                        rollback(connection, exception);
                        throw exception;
                    }
                }
            }

            public void verifyDiscountId(String reference, BigDecimal totalDue) throws SQLException {
                try (Connection connection = DatabaseConnection.getConnection()) {
                    connection.setAutoCommit(false);
                    try {
                        Reservation reservation = lockReservation(connection, reference);
                        if (reservation.type() != ReservationType.ONLINE
                                || reservation.guest().classification() != GuestClassification.DISCOUNTED
                                || reservation.guest().discountIdProofPath() == null
                                || reservation.guest().discountIdProofPath().isBlank()) {
                            throw new ValidationException("A discounted online booking with an ID proof is required.");
                        }
                        try (PreparedStatement statement = connection.prepareStatement(
                                "UPDATE guests SET discount_verified = TRUE WHERE id = ?")) {
                            statement.setLong(1, reservation.guest().id());
                            if (statement.executeUpdate() != 1) {
                                throw new SQLException("Discount ID verification could not be saved.");
                            }
                        }
                        updatePaymentStatus(connection, reservation.id(), totalDue);
                        connection.commit();
                    } catch (SQLException | RuntimeException exception) {
                        rollback(connection, exception);
                        throw exception;
                    }
                }
            }

    public void markCustomerNotificationRead(int customerUserId, long notificationId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE notifications SET read_at = CURRENT_TIMESTAMP "
                             + "WHERE customer_user_id = ? AND id = ? AND read_at IS NULL")) {
            statement.setInt(1, customerUserId);
            statement.setLong(2, notificationId);
            statement.executeUpdate();
        }
    }

    private static void ensureCustomerRoomBookable(
            Connection connection,
            Room room,
            CustomerBookingRequest request,
            String excludedReference,
            BigDecimal expectedRoomRate) throws SQLException {
        if (room.getRoomType() != request.roomType()) {
            throw new ValidationException("The selected room no longer matches the requested room type.");
        }
        if (room.getPricePerDay().compareTo(expectedRoomRate) != 0) {
            throw new ValidationException("The room rate changed. Refresh availability and try again.");
        }
        ensureBookable(connection, room, request.roomNumber(), request.guestCount(),
                request.checkIn(), request.checkOut(), excludedReference);
    }

    private static Reservation lockCustomerReservation(
            Connection connection, int customerUserId, String reference) throws SQLException {
        String sql = RESERVATION_COLUMNS
                + " WHERE r.customer_user_id = ? AND r.reference = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, customerUserId);
            statement.setString(2, reference);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ValidationException("Reservation not found.");
                }
                return mapReservation(resultSet);
            }
        }
    }

    private static BigDecimal sumSubmittedPayments(Connection connection, long reservationId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE reservation_id = ?")) {
            statement.setLong(1, reservationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBigDecimal(1);
            }
        }
    }

    private static void insertCustomerPayment(
            Connection connection,
            long reservationId,
            CustomerPaymentKind kind,
            CustomerPaymentSubmission payment,
            BigDecimal amount,
            String proofPath) throws SQLException {
        String sql = "INSERT INTO payments (reservation_id, payment_type, payment_method, amount, "
                + "reference_details, proof_file_path, is_verified) VALUES (?, ?, ?, ?, ?, ?, FALSE)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            statement.setString(2, kind.getDatabaseValue());
            statement.setString(3, payment.method().getDatabaseValue());
            statement.setBigDecimal(4, amount);
            statement.setString(5, payment.referenceDetails().trim());
            statement.setString(6, proofPath);
            statement.executeUpdate();
        }
    }

    private static void updateGuest(Connection connection, long guestId, Guest guest) throws SQLException {
        String sql = "UPDATE guests SET full_name = ?, contact_number = ?, email = ?, classification = ?, "
                + "discount_id_type = ?, discount_id_number = ?, discount_id_proof_path = ?, "
                + "discount_verified = FALSE WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, guest.fullName());
            statement.setString(2, guest.contactNumber());
            statement.setString(3, guest.email());
            statement.setString(4, guest.classification().getDatabaseValue());
            statement.setString(5, guest.discountIdType());
            statement.setString(6, guest.discountIdNumber());
            statement.setString(7, guest.discountIdProofPath());
            statement.setLong(8, guestId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Guest details were not updated.");
            }
        }
    }

    private static void insertNotification(
            Connection connection, int customerUserId, long reservationId, String type, String message)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO notifications (customer_user_id, reservation_id, notification_type, message) "
                        + "VALUES (?, ?, ?, ?)")) {
            statement.setInt(1, customerUserId);
            statement.setLong(2, reservationId);
            statement.setString(3, type);
            statement.setString(4, message);
            statement.executeUpdate();
        }
    }

    private static CustomerBookingData mapCustomerBookingData(ResultSet resultSet) throws SQLException {
        return new CustomerBookingData(
                mapReservation(resultSet),
                resultSet.getTime("check_in_time").toLocalTime(),
                resultSet.getTime("check_out_time").toLocalTime(),
                CustomerPaymentStatus.fromDatabaseValue(resultSet.getString("payment_status")),
                resultSet.getBigDecimal("amount_submitted"),
                resultSet.getString("payment_methods"));
    }

    private static List<AdminPaymentRecord> findAdminPayments(Connection connection, long reservationId)
            throws SQLException {
        String sql = "SELECT id, payment_type, payment_method, amount, reference_details, "
                + "proof_file_path, is_verified, submitted_at FROM payments "
                + "WHERE reservation_id = ? ORDER BY submitted_at, id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reservationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AdminPaymentRecord> payments = new ArrayList<>();
                while (resultSet.next()) {
                    payments.add(new AdminPaymentRecord(
                            resultSet.getLong("id"),
                            resultSet.getString("payment_type"),
                            resultSet.getString("payment_method"),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getString("reference_details"),
                            resultSet.getString("proof_file_path"),
                            resultSet.getBoolean("is_verified"),
                            resultSet.getTimestamp("submitted_at").toLocalDateTime()));
                }
                return payments;
            }
        }
    }

    private static int countReservations(String sql) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static void updatePaymentStatus(
            Connection connection, long reservationId, BigDecimal totalDue) throws SQLException {
        BigDecimal submitted;
        BigDecimal verified;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COALESCE(SUM(amount), 0), "
                        + "COALESCE(SUM(CASE WHEN is_verified = TRUE THEN amount ELSE 0 END), 0) "
                        + "FROM payments WHERE reservation_id = ?")) {
            statement.setLong(1, reservationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                submitted = resultSet.getBigDecimal(1);
                verified = resultSet.getBigDecimal(2);
            }
        }
        CustomerPaymentStatus status = verified.compareTo(totalDue) >= 0
                ? CustomerPaymentStatus.FULLY_PAID
                : submitted.signum() > 0
                        ? CustomerPaymentStatus.PARTIALLY_PAID : CustomerPaymentStatus.UNPAID;
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE reservations SET payment_status = ? WHERE id = ?")) {
            statement.setString(1, status.getDatabaseValue());
            statement.setLong(2, reservationId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Reservation payment status was not updated.");
            }
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
        return create(guest, roomNumber, checkIn.atTime(15, 0), checkOut.atTime(11, 0),
                guestCount, type, initialStatus);
    }

    public WalkInReceipt createWalkInPaid(
            Guest guest, int roomNumber, LocalDateTime checkIn, LocalDateTime checkOut,
            int guestCount, BigDecimal cashTendered, CheckoutBill bill, int adminId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Room room = lockRoom(connection, roomNumber);
                if (room.getPricePerDay().compareTo(bill.roomRate()) != 0) {
                    throw new ValidationException("Room rate changed. Refresh availability and try again.");
                }
                ensureBookable(connection, room, roomNumber, guestCount, checkIn, checkOut, null);
                if (cashTendered.compareTo(bill.total()) < 0) {
                    throw new ValidationException("Cash tendered is less than the walk-in total.");
                }
                long guestId = insertGuest(connection, guest);
                String reference = newReference("RES-");
                int nights = bill.nights();
                String sql = "INSERT INTO reservations (reference, guest_id, room_number, room_rate, "
                        + "check_in, check_in_time, check_out, check_out_time, nights, guest_count, "
                        + "reservation_type, status, payment_status) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Walk-in', 'Confirmed', 'Fully Paid')";
                long reservationId;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, reference);
                    statement.setLong(2, guestId);
                    statement.setInt(3, roomNumber);
                    statement.setBigDecimal(4, room.getPricePerDay());
                    statement.setDate(5, Date.valueOf(checkIn.toLocalDate()));
                    statement.setTime(6, Time.valueOf(checkIn.toLocalTime()));
                    statement.setDate(7, Date.valueOf(checkOut.toLocalDate()));
                    statement.setTime(8, Time.valueOf(checkOut.toLocalTime()));
                    statement.setInt(9, nights);
                    statement.setInt(10, guestCount);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Database did not return a reservation id.");
                        reservationId = keys.getLong(1);
                    }
                }
                BigDecimal change = cashTendered.subtract(bill.total()).setScale(2, RoundingMode.HALF_UP);
                String receiptReference = newReference("RCT-");
                LocalDateTime paidAt = LocalDateTime.now();
                try (PreparedStatement payment = connection.prepareStatement(
                        "INSERT INTO payments (reservation_id, payment_type, payment_method, amount, "
                                + "is_verified, verified_by, verified_at) "
                                + "VALUES (?, 'Cash', 'Cash', ?, TRUE, ?, CURRENT_TIMESTAMP)")) {
                    payment.setLong(1, reservationId);
                    payment.setBigDecimal(2, bill.total());
                    payment.setInt(3, adminId);
                    payment.executeUpdate();
                }
                try (PreparedStatement transaction = connection.prepareStatement(
                        "INSERT INTO transactions (reservation_id, receipt_reference, room_rate, nights, "
                                + "room_cost, discount_amount, tax_amount, total_amount, cash_tendered, "
                                + "change_amount, payment_status, paid_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Paid', ?)")) {
                    transaction.setLong(1, reservationId);
                    transaction.setString(2, receiptReference);
                    transaction.setBigDecimal(3, bill.roomRate());
                    transaction.setInt(4, bill.nights());
                    transaction.setBigDecimal(5, bill.roomCost());
                    transaction.setBigDecimal(6, bill.discountAmount());
                    transaction.setBigDecimal(7, bill.taxAmount());
                    transaction.setBigDecimal(8, bill.total());
                    transaction.setBigDecimal(9, cashTendered);
                    transaction.setBigDecimal(10, change);
                    transaction.setTimestamp(11, Timestamp.valueOf(paidAt));
                    transaction.executeUpdate();
                }
                BookingHistoryRepository.insertSnapshot(connection, reservationId,
                        "PaymentReceived", "Walk-in payment received: PHP " + bill.total()
                                + " by Cash; receipt " + receiptReference + ".", adminId);
                Reservation reservation = new Reservation(reservationId, reference,
                        guestWithId(guest, guestId), roomNumber,
                        room.getRoomType().getDatabaseValue(), room.getPricePerDay(),
                        checkIn.toLocalDate(), checkOut.toLocalDate(), nights, guestCount,
                        ReservationType.WALK_IN, ReservationStatus.CONFIRMED);
                connection.commit();
                return new WalkInReceipt(reservation, bill, receiptReference, cashTendered, change, paidAt);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public Reservation create(
            Guest guest,
            int roomNumber,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
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
                int nights = Math.max(1, Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(
                        checkIn.toLocalDate(), checkOut.toLocalDate())));
                String sql = "INSERT INTO reservations (reference, guest_id, room_number, room_rate, "
                        + "check_in, check_in_time, check_out, check_out_time, nights, guest_count, "
                        + "reservation_type, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                long reservationId;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, reference);
                    statement.setLong(2, guestId);
                    statement.setInt(3, roomNumber);
                    statement.setBigDecimal(4, roomRate);
                    statement.setDate(5, Date.valueOf(checkIn.toLocalDate()));
                    statement.setTime(6, Time.valueOf(checkIn.toLocalTime()));
                    statement.setDate(7, Date.valueOf(checkOut.toLocalDate()));
                    statement.setTime(8, Time.valueOf(checkOut.toLocalTime()));
                    statement.setInt(9, nights);
                    statement.setInt(10, guestCount);
                    statement.setString(11, type.getDatabaseValue());
                    statement.setString(12, initialStatus.getDatabaseValue());
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
                        room.getRoomType().getDatabaseValue(), roomRate,
                        checkIn.toLocalDate(), checkOut.toLocalDate(),
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

    public List<Reservation> findUpcomingReservationsForRoom(int roomNumber) throws SQLException {
        return findReservations("WHERE r.room_number = ? AND r.status IN ('Pending', 'Confirmed') "
                + "AND r.check_out >= CURRENT_DATE ORDER BY r.check_in, r.id", roomNumber);
    }

    public List<Reservation> findArrivals(LocalDate today) throws SQLException {
        return findReservations("WHERE r.status = 'Confirmed' "
                + "AND TIMESTAMP(r.check_in, r.check_in_time) <= CURRENT_TIMESTAMP "
                + "AND TIMESTAMP(r.check_out, r.check_out_time) > CURRENT_TIMESTAMP "
                + "AND rm.housekeeping_status = 'Clean' ORDER BY r.check_in, r.reference");
    }

    public List<Reservation> findCheckedIn() throws SQLException {
        return findReservations("WHERE r.status = 'Checked-In' ORDER BY r.check_out, r.reference");
    }

    public Reservation findByReference(String reference) throws SQLException {
        List<Reservation> reservations = findReservations("WHERE r.reference = ?", reference);
        return reservations.isEmpty() ? null : reservations.get(0);
    }

    public CustomerPaymentStatus findPaymentStatus(String reference) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT payment_status FROM reservations WHERE reference = ?")) {
            statement.setString(1, reference);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ValidationException("Reservation not found: " + reference);
                }
                return CustomerPaymentStatus.fromDatabaseValue(resultSet.getString(1));
            }
        }
    }

    public Reservation reviewOnline(
            String reference, boolean confirm, BigDecimal minimumVerifiedDownpayment) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.type() != ReservationType.ONLINE
                        || reservation.status() != ReservationStatus.PENDING) {
                    throw new ValidationException("Only pending online reservations can be reviewed.");
                }
                if (confirm) {
                    BigDecimal verifiedDownpayment;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "SELECT COALESCE(SUM(amount), 0) FROM payments "
                                    + "WHERE reservation_id = ? AND payment_type = 'Downpayment' "
                                    + "AND is_verified = TRUE")) {
                        statement.setLong(1, reservation.id());
                        try (ResultSet resultSet = statement.executeQuery()) {
                            resultSet.next();
                            verifiedDownpayment = resultSet.getBigDecimal(1);
                        }
                    }
                    if (minimumVerifiedDownpayment == null
                            || verifiedDownpayment.compareTo(minimumVerifiedDownpayment) < 0) {
                        throw new ValidationException("Cannot approve this booking until at least PHP "
                                + (minimumVerifiedDownpayment == null ? "0.00"
                                        : minimumVerifiedDownpayment.setScale(2, RoundingMode.HALF_UP).toPlainString())
                                + " of downpayment has been verified.");
                    }
                    Room room = lockRoom(connection, reservation.roomNumber());
                    LocalDateTime[] stayTimes = findStayTimes(connection, reservation.id());
                    ensureBookable(connection, room, reservation.roomNumber(), reservation.guestCount(),
                            stayTimes[0], stayTimes[1], reference);
                }
                ReservationStatus status = confirm
                        ? ReservationStatus.CONFIRMED : ReservationStatus.CANCELLED;
                updateReservationStatus(connection, reservation.id(), status);
                insertNotificationForReservation(
                        connection, reservation.id(),
                        confirm ? "ReservationApproved" : "ReservationRejected",
                        confirm
                                ? "Your reservation " + reference + " has been approved."
                                : "Your reservation " + reference + " was rejected.");
                connection.commit();
                return withStatus(reservation, status);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public Reservation checkIn(String reference, LocalDate today) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.status() != ReservationStatus.CONFIRMED) {
                    throw new ValidationException("Only confirmed reservations can be checked in.");
                }
                if (findPaymentStatus(connection, reservation.id()) != CustomerPaymentStatus.FULLY_PAID) {
                    throw new ValidationException("Collect and verify the full balance before check-in.");
                }
                LocalDateTime[] stayTimes = findStayTimes(connection, reservation.id());
                if (LocalDateTime.now().isBefore(stayTimes[0])) {
                    throw new ValidationException("The reservation check-in date has not arrived.");
                }
                if (!LocalDateTime.now().isBefore(stayTimes[1])) {
                    throw new ValidationException("The reservation check-out date has passed.");
                }
                Room room = lockRoom(connection, reservation.roomNumber());
                if (hasActiveCheckedInReservation(connection, reservation.roomNumber())) {
                    throw new ValidationException("The assigned room is already occupied.");
                }
                if (room.getHousekeepingStatus() != HousekeepingStatus.CLEAN) {
                    throw new ValidationException("The assigned room is not clean and ready.");
                }
                if (room.getCapacity() < reservation.guestCount()) {
                    throw new ValidationException("The assigned room does not have enough guest capacity.");
                }
                LocalDateTime cleaningEnds = findConflictEnd(connection, reservation.roomNumber(),
                        stayTimes[0], stayTimes[1], reference);
                if (cleaningEnds != null) {
                    throw new ValidationException("The assigned room is unavailable; cleaning will conclude at "
                            + cleaningEnds.toString().replace('T', ' ') + ".");
                }
                updateReservationStatus(connection, reservation.id(), ReservationStatus.CHECKED_IN);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET is_occupied = TRUE WHERE room_number = ?")) {
                    statement.setInt(1, reservation.roomNumber());
                    if (statement.executeUpdate() != 1) {
                        throw new RoomNotFoundException(reservation.roomNumber());
                    }
                }
                connection.commit();
                return withStatus(reservation, ReservationStatus.CHECKED_IN);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public Reservation collectBalanceAndCheckIn(
            String reference, BigDecimal amount, BigDecimal totalDue, String method, int adminId)
            throws SQLException {
        if (amount == null || totalDue == null || method == null) {
            throw new ValidationException("Payment amount and method are required.");
        }
        if (!List.of("Cash", "GCash", "Bank Transfer", "Other").contains(method)) {
            throw new ValidationException("Choose Cash, GCash, Bank Transfer, or Card / Other.");
        }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.type() != ReservationType.ONLINE
                        || reservation.status() != ReservationStatus.CONFIRMED) {
                    throw new ValidationException("Only confirmed online reservations can collect a check-in balance.");
                }
                LocalDateTime[] stay = findStayTimes(connection, reservation.id());
                if (LocalDateTime.now().isBefore(stay[0]) || !LocalDateTime.now().isBefore(stay[1])) {
                    throw new ValidationException("The reservation is outside its check-in time window.");
                }
                BigDecimal submitted;
                BigDecimal verified;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COALESCE(SUM(amount),0), "
                                + "COALESCE(SUM(CASE WHEN is_verified THEN amount ELSE 0 END),0), "
                                + "COALESCE(SUM(CASE WHEN is_verified THEN 0 ELSE 1 END),0) "
                                + "FROM payments WHERE reservation_id = ?")) {
                    statement.setLong(1, reservation.id());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        resultSet.next();
                        submitted = resultSet.getBigDecimal(1);
                        verified = resultSet.getBigDecimal(2);
                        if (resultSet.getInt(3) != 0) {
                            throw new ValidationException("Verify all submitted online payments before collecting the balance.");
                        }
                    }
                }
                if (submitted.compareTo(verified) != 0) {
                    throw new ValidationException("Submitted payments must be verified before check-in.");
                }
                BigDecimal balance = totalDue.subtract(verified).max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);
                if (balance.signum() > 0 && amount.compareTo(balance) != 0) {
                    throw new ValidationException("Collect the exact remaining balance of PHP "
                            + balance.toPlainString() + ".");
                }
                Room room = lockRoom(connection, reservation.roomNumber());
                ensureCheckInRoomReady(connection, room, reservation, stay, reference);
                if (balance.signum() > 0) {
                    try (PreparedStatement payment = connection.prepareStatement(
                            "INSERT INTO payments (reservation_id, payment_type, payment_method, amount, "
                                    + "is_verified, verified_by, verified_at) "
                                    + "VALUES (?, 'Balance', ?, ?, TRUE, ?, CURRENT_TIMESTAMP)")) {
                        payment.setLong(1, reservation.id());
                        payment.setString(2, method);
                        payment.setBigDecimal(3, balance);
                        payment.setInt(4, adminId);
                        payment.executeUpdate();
                    }
                    updatePaymentStatus(connection, reservation.id(), totalDue);
                } else if (findPaymentStatus(connection, reservation.id()) != CustomerPaymentStatus.FULLY_PAID) {
                    throw new ValidationException("The reservation must be fully paid before check-in.");
                }
                updateReservationStatus(connection, reservation.id(), ReservationStatus.CHECKED_IN);
                setRoomOccupied(connection, reservation.roomNumber());
                if (balance.signum() > 0) {
                    BookingHistoryRepository.insertSnapshot(connection, reservation.id(), "PaymentReceived",
                            "Check-in balance collected: PHP " + balance.toPlainString() + " via " + method + ".",
                            adminId);
                }
                connection.commit();
                return withStatus(reservation, ReservationStatus.CHECKED_IN);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private static void ensureCheckInRoomReady(
            Connection connection, Room room, Reservation reservation,
            LocalDateTime[] stay, String reference) throws SQLException {
        if (hasActiveCheckedInReservation(connection, reservation.roomNumber())) {
            throw new ValidationException("The assigned room is already occupied.");
        }
        if (room.getHousekeepingStatus() != HousekeepingStatus.CLEAN) {
            throw new ValidationException("The assigned room is not clean and ready.");
        }
        if (room.getCapacity() < reservation.guestCount()) {
            throw new ValidationException("The assigned room does not have enough guest capacity.");
        }
        LocalDateTime cleaningEnds = findConflictEnd(connection, reservation.roomNumber(),
                stay[0], stay[1], reference);
        if (cleaningEnds != null) {
            throw new ValidationException("The assigned room is unavailable; cleaning will conclude at "
                    + cleaningEnds.toString().replace('T', ' ') + ".");
        }
    }

    private static void setRoomOccupied(Connection connection, int roomNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE rooms SET is_occupied = TRUE WHERE room_number = ?")) {
            statement.setInt(1, roomNumber);
            if (statement.executeUpdate() != 1) throw new RoomNotFoundException(roomNumber);
        }
    }

    public Reservation completeCheckOut(String reference) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.status() != ReservationStatus.CHECKED_IN) {
                    throw new ValidationException("Only checked-in reservations can be checked out.");
                }
                LocalDateTime[] stay = findStayTimes(connection, reservation.id());
                if (LocalDateTime.now().isBefore(stay[1])) {
                    throw new ValidationException("Check-out is available from "
                            + stay[1].toString().replace('T', ' ') + ".");
                }
                lockRoom(connection, reservation.roomNumber());
                updateReservationStatus(connection, reservation.id(), ReservationStatus.CHECKED_OUT,
                        "Guest checked out. Room marked Dirty for housekeeping.");
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET is_occupied = FALSE, housekeeping_status = 'Dirty', "
                                + "dirty_since = CURRENT_TIMESTAMP WHERE room_number = ?")) {
                    statement.setInt(1, reservation.roomNumber());
                    if (statement.executeUpdate() != 1) throw new RoomNotFoundException(reservation.roomNumber());
                }
                insertNotificationForReservation(connection, reservation.id(), "GuestCheckedOut",
                        "Your stay for reservation " + reference + " has been checked out.");
                connection.commit();
                return withStatus(reservation, ReservationStatus.CHECKED_OUT);
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public Reservation extendStay(
            StayExtensionQuote quote, String paymentMethod, int adminId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, quote.reference());
                if (reservation.status() != ReservationStatus.CHECKED_IN) {
                    throw new ValidationException("Only checked-in guests can extend their stay.");
                }
                LocalDateTime[] stay = findStayTimes(connection, reservation.id());
                if (!stay[1].equals(quote.currentCheckout())
                        || !quote.proposedCheckout().isAfter(stay[1])) {
                    throw new ValidationException("The reservation changed. Refresh the guest and try again.");
                }
                if (paymentMethod == null
                        || !List.of("Cash", "GCash", "Bank Transfer", "Other").contains(paymentMethod)) {
                    throw new ValidationException("Choose a valid extension payment method.");
                }

                Room room = lockRoom(connection, reservation.roomNumber());
                LocalDateTime conflictEnds = findConflictEnd(
                        connection, reservation.roomNumber(), stay[1], quote.proposedCheckout(),
                        reservation.reference());
                if (conflictEnds != null) {
                    throw new ValidationException("The extension conflicts with another reservation; "
                            + "room cleaning will conclude at "
                            + conflictEnds.toString().replace('T', ' ') + ".");
                }

                BigDecimal currentTotal = quote.currentTotal().setScale(2, RoundingMode.HALF_UP);
                BigDecimal proposedTotal = quote.proposedTotal().setScale(2, RoundingMode.HALF_UP);
                BigDecimal extensionCharge = proposedTotal.subtract(currentTotal)
                        .setScale(2, RoundingMode.HALF_UP);
                if (extensionCharge.signum() < 0
                        || extensionCharge.compareTo(quote.extensionCharge()) != 0) {
                    throw new ValidationException("The extension charge changed. Recalculate and try again.");
                }
                BigDecimal verifiedPayments;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT COALESCE(SUM(CASE WHEN is_verified THEN amount ELSE 0 END),0), "
                                + "COALESCE(SUM(CASE WHEN is_verified THEN 0 ELSE 1 END),0) "
                                + "FROM payments WHERE reservation_id = ?")) {
                    statement.setLong(1, reservation.id());
                    try (ResultSet result = statement.executeQuery()) {
                        result.next();
                        verifiedPayments = result.getBigDecimal(1);
                        if (result.getInt(2) > 0) {
                            throw new ValidationException("Resolve submitted payments before extending the stay.");
                        }
                    }
                }
                if (verifiedPayments.compareTo(currentTotal) < 0) {
                    throw new ValidationException("Settle the current reservation balance before extending.");
                }
                if (extensionCharge.signum() > 0) {
                    try (PreparedStatement payment = connection.prepareStatement(
                            "INSERT INTO payments (reservation_id, payment_type, payment_method, amount, "
                                    + "is_verified, verified_by, verified_at) "
                                    + "VALUES (?, 'Balance', ?, ?, TRUE, ?, CURRENT_TIMESTAMP)")) {
                        payment.setLong(1, reservation.id());
                        payment.setString(2, paymentMethod);
                        payment.setBigDecimal(3, extensionCharge);
                        payment.setInt(4, adminId);
                        payment.executeUpdate();
                    }
                }
                int proposedNights = quote.proposedNights();
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE reservations SET check_out = ?, check_out_time = ?, nights = ? WHERE id = ?")) {
                    update.setDate(1, Date.valueOf(quote.proposedCheckout().toLocalDate()));
                    update.setTime(2, Time.valueOf(quote.proposedCheckout().toLocalTime()));
                    update.setInt(3, proposedNights);
                    update.setLong(4, reservation.id());
                    if (update.executeUpdate() != 1) throw new SQLException("The stay extension was not saved.");
                }
                if (extensionCharge.signum() > 0) {
                    updatePaymentStatus(connection, reservation.id(), proposedTotal);
                }
                BookingHistoryRepository.insertSnapshot(connection, reservation.id(), "StayExtended",
                        "Checkout extended to " + quote.proposedCheckout().toString().replace('T', ' ')
                                + "; additional charge PHP " + extensionCharge.toPlainString() + ".", adminId);
                if (extensionCharge.signum() > 0) {
                    BookingHistoryRepository.insertSnapshot(connection, reservation.id(), "PaymentReceived",
                            "Stay extension payment received: PHP " + extensionCharge.toPlainString()
                                    + " via " + paymentMethod + ".", adminId);
                }
                insertNotificationForReservation(connection, reservation.id(), "StayExtended",
                        "Your reservation " + reservation.reference() + " has been extended until "
                                + quote.proposedCheckout().toString().replace('T', ' ') + ".");
                connection.commit();
                return new Reservation(reservation.id(), reservation.reference(), reservation.guest(),
                        reservation.roomNumber(), reservation.roomType(), reservation.roomRate(),
                        reservation.checkIn(), quote.proposedCheckout().toLocalDate(), proposedNights,
                        reservation.guestCount(), reservation.type(), reservation.status());
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
                updateReservationStatus(connection, reservation.id(), ReservationStatus.CHECKED_OUT,
                        "Online guest checked out. Room marked Dirty for housekeeping.");
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE reservations SET payment_status = 'Fully Paid' WHERE id = ?")) {
                    statement.setLong(1, reservation.id());
                    statement.executeUpdate();
                }
                insertNotificationForReservation(
                        connection, reservation.id(), "GuestCheckedOut",
                        "Your stay for reservation " + reference + " has been checked out.");
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET is_occupied = FALSE, housekeeping_status = 'Dirty', "
                                + "dirty_since = CURRENT_TIMESTAMP "
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

    public void forceCheckOut(String reference, String reason) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.status() != ReservationStatus.CHECKED_IN) {
                    throw new ValidationException("Only checked-in reservations can be force checked out.");
                }
                lockRoom(connection, reservation.roomNumber());
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE reservations SET status = 'Checked-Out', force_checkout_reason = ? "
                                + "WHERE id = ? AND status = 'Checked-In'")) {
                    statement.setString(1, reason);
                    statement.setLong(2, reservation.id());
                    if (statement.executeUpdate() != 1) {
                        throw new ValidationException("The reservation is no longer checked in.");
                    }
                }
                BookingHistoryRepository.insertSnapshot(connection, reservation.id(), "Checked-Out",
                        "Forced check-out. Reason: " + reason);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET is_occupied = FALSE, housekeeping_status = 'Dirty', "
                                + "dirty_since = CURRENT_TIMESTAMP WHERE room_number = ?")) {
                    statement.setInt(1, reservation.roomNumber());
                    if (statement.executeUpdate() != 1) {
                        throw new RoomNotFoundException(reservation.roomNumber());
                    }
                }
                insertNotificationForReservation(connection, reservation.id(), "GuestCheckedOut",
                        "Your stay was checked out by hotel staff. Reason: " + reason);
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public void checkOutOnline(String reference) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Reservation reservation = lockReservation(connection, reference);
                if (reservation.type() != ReservationType.ONLINE
                        || reservation.status() != ReservationStatus.CHECKED_IN) {
                    throw new ValidationException("Only checked-in online reservations can be checked out here.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT payment_status FROM reservations WHERE id = ? FOR UPDATE")) {
                    statement.setLong(1, reservation.id());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()
                                || CustomerPaymentStatus.fromDatabaseValue(resultSet.getString(1))
                                        != CustomerPaymentStatus.FULLY_PAID) {
                            throw new ValidationException(
                                    "The online reservation balance must be fully paid and verified first.");
                        }
                    }
                }
                lockRoom(connection, reservation.roomNumber());
                updateReservationStatus(connection, reservation.id(), ReservationStatus.CHECKED_OUT);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET is_occupied = FALSE, housekeeping_status = 'Dirty', "
                                + "dirty_since = CURRENT_TIMESTAMP WHERE room_number = ?")) {
                    statement.setInt(1, reservation.roomNumber());
                    if (statement.executeUpdate() != 1) {
                        throw new RoomNotFoundException(reservation.roomNumber());
                    }
                }
                insertNotificationForReservation(connection, reservation.id(), "GuestCheckedOut",
                        "Your stay for reservation " + reference + " has been checked out.");
                connection.commit();
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
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            String excludedReference) throws SQLException {
        if (room.getCapacity() < guestCount) {
            throw new ValidationException("Room " + roomNumber + " does not have enough guest capacity.");
        }
        if (room.getHousekeepingStatus() != HousekeepingStatus.CLEAN) {
            throw new ValidationException("Room " + roomNumber + " is not clean and available.");
        }
        LocalDateTime cleaningEnds = findConflictEnd(connection, roomNumber, checkIn, checkOut, excludedReference);
        if (cleaningEnds != null) {
            throw new ValidationException("Room " + roomNumber + " is unavailable; cleaning will conclude at "
                    + cleaningEnds.toString().replace('T', ' ') + ".");
        }
    }

    private static LocalDateTime findConflictEnd(
            Connection connection,
            int roomNumber,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            String excludedReference) throws SQLException {
        String sql = "SELECT TIMESTAMP(check_out, check_out_time) FROM reservations WHERE room_number = ? "
                + "AND status IN ('Pending', 'Confirmed', 'Checked-In') "
                + "AND TIMESTAMP(check_in, check_in_time) < ? "
                + "AND TIMESTAMP(check_out, check_out_time) > ? ";
        if (excludedReference != null) {
            sql += " AND reference <> ?";
        }
        sql += " ORDER BY TIMESTAMP(check_out, check_out_time) LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            statement.setTimestamp(2, Timestamp.valueOf(checkOut.plusHours(1)));
            statement.setTimestamp(3, Timestamp.valueOf(checkIn.minusHours(1)));
            if (excludedReference != null) {
                statement.setString(4, excludedReference);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getTimestamp(1).toLocalDateTime().plusHours(1) : null;
            }
        }
    }

    private static CustomerPaymentStatus findPaymentStatus(Connection connection, long reservationId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT payment_status FROM reservations WHERE id = ?")) {
            statement.setLong(1, reservationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) throw new ValidationException("Reservation not found.");
                return CustomerPaymentStatus.fromDatabaseValue(resultSet.getString(1));
            }
        }
    }

    private static LocalDateTime[] findStayTimes(Connection connection, long reservationId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT TIMESTAMP(check_in, check_in_time), TIMESTAMP(check_out, check_out_time) "
                        + "FROM reservations WHERE id = ?")) {
            statement.setLong(1, reservationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new ValidationException("Reservation not found.");
                }
                return new LocalDateTime[]{resultSet.getTimestamp(1).toLocalDateTime(),
                        resultSet.getTimestamp(2).toLocalDateTime()};
            }
        }
    }

    private static boolean hasActiveCheckedInReservation(Connection connection, int roomNumber)
            throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status = 'Checked-In' "
                + "LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static long insertGuest(Connection connection, Guest guest) throws SQLException {
        String sql = "INSERT INTO guests (full_name, contact_number, email, classification, "
                + "discount_id_type, discount_id_number, discount_id_proof_path, discount_verified) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, guest.fullName());
            statement.setString(2, guest.contactNumber());
            statement.setString(3, guest.email());
            statement.setString(4, guest.classification().getDatabaseValue());
            statement.setString(5, guest.discountIdType());
            statement.setString(6, guest.discountIdNumber());
            statement.setString(7, guest.discountIdProofPath());
            statement.setBoolean(8, guest.discountVerified());
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
        updateReservationStatus(connection, reservationId, status,
                "Reservation status changed to " + status.getDatabaseValue() + ".");
    }

    private static void updateReservationStatus(
            Connection connection, long reservationId, ReservationStatus status, String auditNote)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE reservations SET status = ? WHERE id = ?")) {
            statement.setString(1, status.getDatabaseValue());
            statement.setLong(2, reservationId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Reservation status was not updated.");
            }
        }
        if (status == ReservationStatus.CHECKED_IN
                || status == ReservationStatus.CHECKED_OUT
                || status == ReservationStatus.CANCELLED) {
            BookingHistoryRepository.insertSnapshot(connection, reservationId,
                    status.getDatabaseValue(), auditNote);
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
                resultSet.getString("discount_id_proof_path"));
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

    private static Guest guestWithId(Guest guest, long id) {
        return new Guest(id, guest.fullName(), guest.contactNumber(), guest.email(),
                guest.classification(), guest.discountIdType(), guest.discountIdNumber(),
                guest.discountVerified());
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
