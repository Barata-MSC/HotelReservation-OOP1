package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.CheckoutReceipt;
import com.hotel.core.model.AdminDashboardSnapshot;
import com.hotel.core.model.AdminPaymentRecord;
import com.hotel.core.model.AdminReservationData;
import com.hotel.core.model.AdminReservationDetails;
import com.hotel.core.model.BookingHistoryEntry;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.CustomerBookingData;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.StayExtensionQuote;
import com.hotel.core.model.WalkInReceipt;
import com.hotel.core.model.RoomMonitoringSnapshot;
import com.hotel.core.model.enums.CustomerPaymentStatus;
import com.hotel.core.model.enums.BookingHistoryPeriod;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.ReservationType;
import com.hotel.core.repository.ReservationRepository;
import com.hotel.core.repository.BookingHistoryRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReservationService {

    private static final BigDecimal REGULAR_TAX_RATE = new BigDecimal("0.12");
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    private final ReservationRepository reservationRepository;
    private final BookingHistoryRepository bookingHistoryRepository = new BookingHistoryRepository();
    private final RoomService roomService;

    public ReservationService() {
        this(new ReservationRepository(), new RoomService());
    }

    public ReservationService(ReservationRepository reservationRepository) {
        this(reservationRepository, new RoomService());
    }

    public ReservationService(ReservationRepository reservationRepository, RoomService roomService) {
        this.reservationRepository = Objects.requireNonNull(
                reservationRepository, "Reservation repository is required");
        this.roomService = Objects.requireNonNull(roomService, "Room service is required");
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int guestCount)
            throws SQLException {
        validateStay(checkIn, checkOut);
        validateGuestCount(guestCount);
        return reservationRepository.findAvailableRooms(checkIn, checkOut, guestCount);
    }

    public List<Room> findAvailableRooms(LocalDateTime checkIn, LocalDateTime checkOut, int guestCount)
            throws SQLException {
        validateStayTimes(checkIn, checkOut);
        validateGuestCount(guestCount);
        return reservationRepository.findAvailableRooms(checkIn, checkOut, guestCount);
    }

    public Reservation createWalkIn(
            AuthenticatedUser admin,
            Guest guest, int roomNumber, LocalDate checkIn, LocalDate checkOut, int guestCount)
            throws SQLException {
        requireAdmin(admin);
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

    public Reservation createWalkIn(
            AuthenticatedUser admin, Guest guest, int roomNumber,
            LocalDateTime checkIn, LocalDateTime checkOut, int guestCount) throws SQLException {
        requireAdmin(admin);
        validateGuest(guest);
        validateRoomNumber(roomNumber);
        validateGuestCount(guestCount);
        validateStayTimes(checkIn, checkOut);
        if (!LocalDate.now().equals(checkIn.toLocalDate())) {
            throw new ValidationException("A walk-in reservation must start today.");
        }
        if (checkIn.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ValidationException("Walk-in check-in time cannot be in the past.");
        }
        return reservationRepository.create(guest, roomNumber, checkIn, checkOut,
                guestCount, ReservationType.WALK_IN, ReservationStatus.CONFIRMED);
    }

    public WalkInReceipt createWalkInPaid(
            AuthenticatedUser admin, Guest guest, int roomNumber,
            LocalDateTime checkIn, LocalDateTime checkOut, int guestCount, BigDecimal cashTendered)
            throws SQLException {
        requireAdmin(admin);
        validateGuest(guest);
        validateRoomNumber(roomNumber);
        validateGuestCount(guestCount);
        validateStayTimes(checkIn, checkOut);
        if (!LocalDate.now().equals(checkIn.toLocalDate())) {
            throw new ValidationException("A walk-in reservation must start today.");
        }
        if (checkIn.isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ValidationException("Walk-in check-in time cannot be in the past.");
        }
        if (cashTendered == null || cashTendered.signum() <= 0) {
            throw new ValidationException("Cash tendered must be a positive amount.");
        }
        BigDecimal normalizedCash;
        try {
            normalizedCash = cashTendered.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException("Cash tendered must have at most two decimal places.");
        }
        Room room = roomService.findByNumber(roomNumber);
        int nights = Math.max(1, Math.toIntExact(ChronoUnit.DAYS.between(
                checkIn.toLocalDate(), checkOut.toLocalDate())));
        Reservation quote = new Reservation(0, "WALK-IN-QUOTE", guest, roomNumber,
                room.getRoomType().getDatabaseValue(), room.getPricePerDay(),
                checkIn.toLocalDate(), checkOut.toLocalDate(), nights, guestCount,
                ReservationType.WALK_IN, ReservationStatus.CONFIRMED);
        CheckoutBill bill = calculateCheckoutBill(quote);
        if (normalizedCash.compareTo(bill.total()) < 0) {
            throw new ValidationException("Cash tendered is less than the total due of PHP "
                    + bill.total().toPlainString() + ".");
        }
        return reservationRepository.createWalkInPaid(
                guest, roomNumber, checkIn, checkOut, guestCount, normalizedCash, bill, admin.getId());
    }

    private static void validateStayTimes(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out must be after check-in.");
        }
    }

    public List<Reservation> findPendingOnlineReservations(AuthenticatedUser admin) throws SQLException {
        requireAdmin(admin);
        return reservationRepository.findPendingOnline();
    }

    public List<Reservation> findUpcomingReservationsForRoom(
            AuthenticatedUser admin, int roomNumber) throws SQLException {
        requireAdmin(admin);
        validateRoomNumber(roomNumber);
        return reservationRepository.findUpcomingReservationsForRoom(roomNumber);
    }

    public List<AdminReservationDetails> findAdminReservations(AuthenticatedUser admin) throws SQLException {
        requireAdmin(admin);
        return reservationRepository.findAdminReservations().stream()
                .map(this::toAdminDetails)
                .toList();
    }

    public AdminReservationDetails findAdminReservationDetails(
            AuthenticatedUser admin, String reference) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        return findAdminReservations(admin).stream()
                .filter(item -> item.booking().reservation().reference().equals(reference.trim()))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Reservation not found: " + reference.trim()));
    }

    public Reservation collectBalanceAndCheckIn(
            AuthenticatedUser admin, String reference, BigDecimal amount, String method) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        if (amount == null || amount.signum() < 0) {
            throw new ValidationException("Enter a valid balance payment amount.");
        }
        BigDecimal normalized;
        try {
            normalized = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException("Payment amount must have at most two decimal places.");
        }
        AdminReservationDetails details = findAdminReservationDetails(admin, reference);
        if (details.booking().reservation().type() != ReservationType.ONLINE) {
            throw new ValidationException("Walk-in reservations are paid when they are created.");
        }
        return reservationRepository.collectBalanceAndCheckIn(reference.trim(), normalized,
                details.booking().total(), method, admin.getId());
    }

    public List<BookingHistoryEntry> findBookingHistory(
            AuthenticatedUser admin, BookingHistoryPeriod period, LocalDate today) throws SQLException {
        requireAdmin(admin);
        if (period == null || today == null) {
            throw new ValidationException("Choose a booking history period.");
        }
        return bookingHistoryRepository.findBetween(
                period.startDate(today), period.endDateExclusive(today));
    }

    public void recordBookingDispute(AuthenticatedUser admin, String reference, String reason)
            throws SQLException {
        requireAdmin(admin);
        String normalizedReference = requireText(reference, "Reservation reference");
        String normalizedReason = requireText(reason, "Dispute reason");
        if (normalizedReason.length() > 500) {
            throw new ValidationException("Dispute reason must be 500 characters or fewer.");
        }
        Reservation reservation = reservationRepository.findByReference(normalizedReference);
        bookingHistoryRepository.recordDispute(
                reservation.id(), reservation.reference(), normalizedReason, admin.getId());
    }

    public List<AdminReservationDetails> findAdminCalendarReservations(
            AuthenticatedUser admin, LocalDate monthStart, LocalDate monthEnd) throws SQLException {
        requireAdmin(admin);
        if (monthStart == null || monthEnd == null || monthEnd.isBefore(monthStart)) {
            throw new ValidationException("A valid calendar date range is required.");
        }
        return reservationRepository.findAdminCalendarReservations(monthStart, monthEnd).stream()
                .map(this::toAdminDetails)
                .toList();
    }

    public AdminDashboardSnapshot getAdminDashboardSnapshot(
            AuthenticatedUser admin, LocalDate today) throws SQLException {
        requireAdmin(admin);
        Objects.requireNonNull(today, "Today is required");
        return new AdminDashboardSnapshot(
                roomService.getMonitoringSnapshot(),
                reservationRepository.countPendingOnlineReservations(),
                reservationRepository.countArrivals(today),
                reservationRepository.countDepartures(today),
                reservationRepository.findRecentAdminReservations(8).stream()
                        .map(this::toAdminDetails)
                        .toList());
    }

    public Reservation reviewOnlineReservation(
            AuthenticatedUser admin, String reference, boolean confirm) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        BigDecimal minimumDownpayment = BigDecimal.ZERO.setScale(2);
        if (confirm) {
            Reservation reservation = reservationRepository.findByReference(reference.trim());
            if (reservation == null || reservation.type() != ReservationType.ONLINE) {
                throw new ValidationException("Online reservation not found.");
            }
            minimumDownpayment = calculateAdminBill(reservation).total()
                    .multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        }
        return reservationRepository.reviewOnline(reference.trim(), confirm, minimumDownpayment);
    }

    public void verifyOnlinePayment(
            AuthenticatedUser admin, String reference, long paymentId) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        if (paymentId < 1) {
            throw new ValidationException("A valid payment record is required.");
        }
        Reservation reservation = reservationRepository.findByReference(reference.trim());
        if (reservation == null || reservation.type() != ReservationType.ONLINE) {
            throw new ValidationException("Online reservation not found.");
        }
        CheckoutBill bill = calculateAdminBill(reservation);
        reservationRepository.verifyOnlinePayment(reference.trim(), paymentId, admin.getId(), bill.total());
    }

    public void verifyDiscountId(AuthenticatedUser admin, String reference) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        Reservation reservation = reservationRepository.findByReference(reference.trim());
        if (reservation == null || reservation.type() != ReservationType.ONLINE
                || reservation.guest().classification() != GuestClassification.DISCOUNTED
                || reservation.guest().discountIdProofPath() == null) {
            throw new ValidationException("A discounted online reservation with an ID proof is required.");
        }
        Guest verifiedGuest = new Guest(
                reservation.guest().id(), reservation.guest().fullName(),
                reservation.guest().contactNumber(), reservation.guest().email(),
                reservation.guest().classification(), reservation.guest().discountIdType(),
                reservation.guest().discountIdNumber(), true,
                reservation.guest().discountIdProofPath());
        Reservation verifiedReservation = withGuest(reservation, verifiedGuest);
        reservationRepository.verifyDiscountId(
                reference.trim(), calculateCheckoutBill(verifiedReservation).total());
    }

    public List<Reservation> findArrivals(AuthenticatedUser admin, LocalDate today) throws SQLException {
        requireAdmin(admin);
        Objects.requireNonNull(today, "Today is required");
        return reservationRepository.findArrivals(today);
    }

    public List<Reservation> findCheckedInReservations(AuthenticatedUser admin) throws SQLException {
        requireAdmin(admin);
        return reservationRepository.findCheckedIn();
    }

    public Reservation findByReference(AuthenticatedUser admin, String reference) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        return reservationRepository.findByReference(reference.trim());
    }

    public CustomerPaymentStatus getPaymentStatus(AuthenticatedUser admin, String reference)
            throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        return reservationRepository.findPaymentStatus(reference.trim());
    }

    public Reservation checkIn(AuthenticatedUser admin, String reference, LocalDate today) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        Objects.requireNonNull(today, "Today is required");
        return reservationRepository.checkIn(reference.trim(), today);
    }

    public CheckoutBill calculateCheckoutBill(Reservation reservation) {
        Objects.requireNonNull(reservation, "Reservation is required");
        long elapsedDays = ChronoUnit.DAYS.between(
                reservation.checkIn(), reservation.checkOut());
        long calculatedNights = Math.max(1, elapsedDays);
        if (elapsedDays < 0 || calculatedNights > Integer.MAX_VALUE
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

    public CheckoutReceipt checkOut(
            AuthenticatedUser admin, String reference, BigDecimal cashTendered, LocalDate today)
            throws SQLException {
        requireAdmin(admin);
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
        if (reservation.type() == ReservationType.ONLINE) {
            throw new ValidationException("Online reservations are checked out after their verified balance is paid.");
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

    public Reservation completeCheckOut(AuthenticatedUser admin, String reference) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        Reservation reservation = reservationRepository.findByReference(reference.trim());
        if (reservation == null) throw new ValidationException("Reservation not found: " + reference.trim());
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("Only checked-in reservations can be checked out.");
        }
        return reservationRepository.completeCheckOut(reference.trim());
    }

    public StayExtensionQuote quoteStayExtension(
            AuthenticatedUser admin, String reference, LocalDateTime proposedCheckout) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        if (proposedCheckout == null) throw new ValidationException("Choose a new check-out date and time.");
        AdminReservationDetails details = findAdminReservationDetails(admin, reference.trim());
        Reservation reservation = details.booking().reservation();
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("Only checked-in guests can extend their stay.");
        }
        LocalDateTime currentCheckout = LocalDateTime.of(
                reservation.checkOut(), details.booking().checkOutTime());
        if (!proposedCheckout.isAfter(currentCheckout)) {
            throw new ValidationException("The new check-out time must be later than the current one.");
        }
        if (!proposedCheckout.toLocalDate().isAfter(reservation.checkOut())) {
            throw new ValidationException("Extend the check-out date by at least one calendar day to add a billed night.");
        }
        if (!proposedCheckout.isAfter(LocalDateTime.now())) {
            throw new ValidationException("The new check-out time must be in the future.");
        }
        long elapsedDays = ChronoUnit.DAYS.between(reservation.checkIn(), proposedCheckout.toLocalDate());
        int proposedNights = Math.toIntExact(Math.max(1, elapsedDays));
        Reservation extended = new Reservation(reservation.id(), reservation.reference(), reservation.guest(),
                reservation.roomNumber(), reservation.roomType(), reservation.roomRate(), reservation.checkIn(),
                proposedCheckout.toLocalDate(), proposedNights, reservation.guestCount(),
                reservation.type(), reservation.status());
        CheckoutBill currentBill = calculateCheckoutBill(reservation);
        CheckoutBill proposedBill = calculateCheckoutBill(extended);
        BigDecimal extensionCharge = proposedBill.total().subtract(currentBill.total())
                .setScale(2, RoundingMode.HALF_UP);
        if (extensionCharge.signum() < 0) {
            throw new ValidationException("The extension total cannot be lower than the current reservation total.");
        }
        return new StayExtensionQuote(reservation.reference(), currentCheckout, proposedCheckout,
                reservation.nights(), proposedNights, currentBill.total(), proposedBill.total(), extensionCharge);
    }

    public Reservation extendStay(
            AuthenticatedUser admin, StayExtensionQuote quote, String paymentMethod) throws SQLException {
        requireAdmin(admin);
        if (quote == null) throw new ValidationException("Recalculate the stay extension before saving.");
        validateReference(quote.reference());
        return reservationRepository.extendStay(quote, paymentMethod, admin.getId());
    }

    public void checkOutOnline(AuthenticatedUser admin, String reference, LocalDate today) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        Objects.requireNonNull(today, "Today is required");
        Reservation reservation = reservationRepository.findByReference(reference.trim());
        if (reservation == null || reservation.type() != ReservationType.ONLINE) {
            throw new ValidationException("Online reservation not found: " + reference.trim());
        }
        if (reservation.status() != ReservationStatus.CHECKED_IN) {
            throw new ValidationException("Only checked-in reservations can be checked out.");
        }
        if (today.isBefore(reservation.checkOut())) {
            throw new ValidationException("The reservation check-out date has not arrived.");
        }
        if (reservationRepository.findPaymentStatus(reference.trim())
                != CustomerPaymentStatus.FULLY_PAID) {
            throw new ValidationException(
                    "The online reservation balance must be fully paid and verified first.");
        }
        reservationRepository.checkOutOnline(reference.trim());
    }

    public void forceCheckOut(AuthenticatedUser admin, String reference, String reason) throws SQLException {
        requireAdmin(admin);
        validateReference(reference);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) {
            throw new ValidationException("A reason is required for a forced check-out.");
        }
        if (normalizedReason.length() > 500) {
            throw new ValidationException("The forced check-out reason cannot exceed 500 characters.");
        }
        reservationRepository.forceCheckOut(reference.trim(), normalizedReason);
    }

    private AdminReservationDetails toAdminDetails(AdminReservationData data) {
        CustomerBookingData bookingData = data.booking();
        Reservation reservation = bookingData.reservation();
        CheckoutBill bill = calculateAdminBill(reservation);
        BigDecimal amountSubmitted = bookingData.amountSubmitted() == null
                ? BigDecimal.ZERO.setScale(2)
                : bookingData.amountSubmitted().setScale(2, RoundingMode.HALF_UP);
        BigDecimal balance = bill.total().subtract(amountSubmitted).max(BigDecimal.ZERO).setScale(2);
        String methods = bookingData.paymentMethods() == null || bookingData.paymentMethods().isBlank()
                ? "Not submitted" : bookingData.paymentMethods();
        CustomerBooking booking = new CustomerBooking(
                reservation,
                bookingData.checkInTime(),
                bookingData.checkOutTime(),
                bookingData.paymentStatus(),
                bill.total(),
                amountSubmitted,
                balance,
                methods);
        BigDecimal verifiedAmount;
        if (reservation.type() == ReservationType.WALK_IN
                && reservation.status() == ReservationStatus.CHECKED_OUT) {
            verifiedAmount = bill.total();
        } else {
            verifiedAmount = data.payments().stream()
                    .filter(AdminPaymentRecord::verified)
                    .map(AdminPaymentRecord::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return new AdminReservationDetails(booking, verifiedAmount, data.payments());
    }

    private CheckoutBill calculateAdminBill(Reservation reservation) {
        if (reservation.type() == ReservationType.ONLINE
                && reservation.guest().classification() == GuestClassification.DISCOUNTED
                && !reservation.guest().discountVerified()) {
            Guest provisionalGuest = new Guest(
                    reservation.guest().id(), reservation.guest().fullName(),
                    reservation.guest().contactNumber(), reservation.guest().email(),
                    reservation.guest().classification(), reservation.guest().discountIdType(),
                    reservation.guest().discountIdNumber(), true,
                    reservation.guest().discountIdProofPath());
            return calculateCheckoutBill(withGuest(reservation, provisionalGuest));
        }
        return calculateCheckoutBill(reservation);
    }

    private static Reservation withGuest(Reservation reservation, Guest guest) {
        return new Reservation(
                reservation.id(), reservation.reference(), guest, reservation.roomNumber(),
                reservation.roomType(), reservation.roomRate(), reservation.checkIn(),
                reservation.checkOut(), reservation.nights(), reservation.guestCount(),
                reservation.type(), reservation.status());
    }

    private static void requireAdmin(AuthenticatedUser admin) {
        if (admin == null || admin.getRole() != com.hotel.core.model.UserRole.ADMIN || admin.getId() < 1) {
            throw new ValidationException("A signed-in administrator account is required.");
        }
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
