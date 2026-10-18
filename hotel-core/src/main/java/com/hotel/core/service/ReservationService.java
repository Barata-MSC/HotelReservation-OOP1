package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.CheckoutReceipt;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.ReservationType;
import com.hotel.core.repository.ReservationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReservationService {

    private static final BigDecimal REGULAR_TAX_RATE = new BigDecimal("0.12");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    private final ReservationRepository reservationRepository;

    public ReservationService() {
        this(new ReservationRepository());
    }

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = Objects.requireNonNull(
                reservationRepository, "Reservation repository is required");
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int guestCount)
            throws SQLException {
        validateStay(checkIn, checkOut);
        validateGuestCount(guestCount);
        return reservationRepository.findAvailableRooms(checkIn, checkOut, guestCount);
    }

    public Reservation createWalkIn(
            Guest guest, int roomNumber, LocalDate checkIn, LocalDate checkOut, int guestCount)
            throws SQLException {
        validateGuest(guest);
        validateRoomNumber(roomNumber);
        validateGuestCount(guestCount);
        validateStay(checkIn, checkOut);
        if (!LocalDate.now().equals(checkIn)) {
            throw new ValidationException("A walk-in reservation must start today.");
        }
        return reservationRepository.create(
                guest, roomNumber, checkIn, checkOut, guestCount,
                ReservationType.WALK_IN, ReservationStatus.CONFIRMED);
    }

    public Reservation createOnline(
            Guest guest, int roomNumber, LocalDate checkIn, LocalDate checkOut, int guestCount)
            throws SQLException {
        validateGuest(guest);
        validateRoomNumber(roomNumber);
        validateGuestCount(guestCount);
        validateStay(checkIn, checkOut);
        if (checkIn.isBefore(LocalDate.now())) {
            throw new ValidationException("Online reservations cannot start in the past.");
        }
        return reservationRepository.create(
                guest, roomNumber, checkIn, checkOut, guestCount,
                ReservationType.ONLINE, ReservationStatus.PENDING);
    }

    public List<Reservation> findPendingOnlineReservations() throws SQLException {
        return reservationRepository.findPendingOnline();
    }

    public Reservation reviewOnlineReservation(String reference, boolean confirm) throws SQLException {
        validateReference(reference);
        return reservationRepository.reviewOnline(reference.trim(), confirm);
    }

    public List<Reservation> findArrivals(LocalDate today) throws SQLException {
        Objects.requireNonNull(today, "Today is required");
        return reservationRepository.findArrivals(today);
    }

    public List<Reservation> findCheckedInReservations() throws SQLException {
        return reservationRepository.findCheckedIn();
    }

    public Reservation findByReference(String reference) throws SQLException {
        validateReference(reference);
        return reservationRepository.findByReference(reference.trim());
    }

    public Reservation checkIn(String reference, LocalDate today) throws SQLException {
        validateReference(reference);
        Objects.requireNonNull(today, "Today is required");
        return reservationRepository.checkIn(reference.trim(), today);
    }

    public CheckoutBill calculateCheckoutBill(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation is required");
        long calculatedNights = ChronoUnit.DAYS.between(
                reservation.checkIn(), reservation.checkOut());
        if (calculatedNights <= 0 || calculatedNights > Integer.MAX_VALUE
                || calculatedNights != reservation.nights()) {
            throw new ValidationException("Reservation dates or stored night count are invalid.");
        }
        if (reservation.roomRate() == null || reservation.roomRate().signum() <= 0) {
            throw new ValidationException("Reservation room rate must be greater than zero.");
        }

        boolean discounted = reservation.guest().classification() == GuestClassification.DISCOUNTED
                && reservation.guest().discountVerified();
        BigDecimal discountRate = discounted ? DISCOUNT_RATE : BigDecimal.ZERO;
        BigDecimal taxRate = discounted ? BigDecimal.ZERO : REGULAR_TAX_RATE;
        BigDecimal roomCost = reservation.roomRate()
                .multiply(BigDecimal.valueOf(calculatedNights)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = roomCost.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = roomCost.subtract(discountAmount);
        BigDecimal taxAmount = taxableAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        return new CheckoutBill(
                Math.toIntExact(calculatedNights), reservation.roomRate(), roomCost,
                discountRate, discountAmount, taxRate, taxAmount, total);
    }

    public CheckoutReceipt checkOut(String reference, BigDecimal cashTendered, LocalDate today)
            throws SQLException {
        validateReference(reference);
        Objects.requireNonNull(today, "Today is required");
        if (cashTendered == null || cashTendered.signum() <= 0) {
            throw new ValidationException("Cash tendered must be a positive amount with at most two decimals.");
        }
        BigDecimal normalizedCash;
        try {
            normalizedCash = cashTendered.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException("Cash tendered must be a positive amount with at most two decimals.");
        }
        Reservation reservation = reservationRepository.findByReference(reference.trim());
        if (reservation == null) {
            throw new ValidationException("Reservation not found: " + reference.trim());
        }
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("Only checked-in reservations can be checked out.");
        }
        if (today.isBefore(reservation.checkOut())) {
            throw new ValidationException("The reservation check-out date has not arrived.");
        }
        CheckoutBill bill = calculateCheckoutBill(reservation);
        if (normalizedCash.compareTo(bill.total()) < 0) {
            throw new ValidationException("Cash tendered is less than the amount due.");
        }
        return reservationRepository.checkOut(reference.trim(), normalizedCash, bill);
    }

    private static void validateGuest(Guest guest) {
        if (guest == null) {
            throw new ValidationException("Guest details are required.");
        }
        requireText(guest.fullName(), "Guest name");
        requireText(guest.contactNumber(), "Contact number");
        String email = requireText(guest.email(), "Email");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ValidationException("Enter a valid guest email address.");
        }
        if (guest.classification() == null) {
            throw new ValidationException("Guest classification is required.");
        }
        if (guest.classification() == GuestClassification.DISCOUNTED) {
            requireText(guest.discountIdType(), "Discount ID type");
            requireText(guest.discountIdNumber(), "Discount ID number");
            if (!guest.discountVerified()) {
                throw new ValidationException("Verify the discount ID before applying a discount.");
            }
        } else if (guest.discountVerified()) {
            throw new ValidationException("Regular guests cannot be marked as discount-verified.");
        }
    }

    private static void validateStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new ValidationException("Check-in and check-out dates are required.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out date must be after check-in date.");
        }
        try {
            Math.toIntExact(ChronoUnit.DAYS.between(checkIn, checkOut));
        } catch (ArithmeticException exception) {
            throw new ValidationException("The stay duration is too long.");
        }
    }

    private static void validateGuestCount(int guestCount) {
        if (guestCount < 1) {
            throw new ValidationException("At least one guest is required.");
        }
    }

    private static void validateRoomNumber(int roomNumber) {
        if (roomNumber < 1) {
            throw new ValidationException("Room number must be a positive whole number.");
        }
    }

    private static void validateReference(String reference) {
        if (reference == null || reference.isBlank()) {
            throw new ValidationException("Reservation reference is required.");
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " is required.");
        }
        return value.trim();
    }
}
