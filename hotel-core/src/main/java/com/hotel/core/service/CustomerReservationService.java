package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.CustomerBookingData;
import com.hotel.core.model.CustomerBookingQuote;
import com.hotel.core.model.CustomerBookingRequest;
import com.hotel.core.model.CustomerNotification;
import com.hotel.core.model.CustomerPaymentSubmission;
import com.hotel.core.model.CustomerReceipt;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.CustomerPaymentKind;
import com.hotel.core.model.enums.CustomerPaymentStatus;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.repository.ReservationRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public final class CustomerReservationService {

    private static final BigDecimal DOWNPAYMENT_RATE = new BigDecimal("0.50");
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final PaymentProofStorage proofStorage;

    public CustomerReservationService() {
        this(new ReservationRepository(), PaymentProofStorage.fromConfiguration());
    }

    public CustomerReservationService(
            ReservationRepository reservationRepository, PaymentProofStorage proofStorage) {
        this.reservationRepository = Objects.requireNonNull(
                reservationRepository, "Reservation repository is required");
        this.reservationService = new ReservationService(reservationRepository);
        this.proofStorage = Objects.requireNonNull(proofStorage, "Payment proof storage is required");
    }

    public List<Room> findAvailableRooms(
            AuthenticatedUser customer,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount,
            RoomType roomType) throws SQLException {
        requireCustomer(customer);
        validateStay(checkIn, checkOut);
        validateGuestCount(guestCount);
        if (roomType == null) {
            throw new ValidationException("Room type is required.");
        }
        return reservationRepository.findAvailableRooms(checkIn, checkOut, guestCount, roomType);
    }

    private List<Room> findAvailableRooms(
            AuthenticatedUser customer, LocalDateTime checkIn, LocalDateTime checkOut,
            int guestCount, RoomType roomType) throws SQLException {
        requireCustomer(customer);
        validateGuestCount(guestCount);
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn) || roomType == null) {
            throw new ValidationException("Valid stay dates, times, and room type are required.");
        }
        return reservationRepository.findAvailableRooms(checkIn, checkOut, guestCount, roomType, null);
    }

    public List<Room> findAvailableRoomsForChange(
            AuthenticatedUser customer,
            String reference,
            LocalDate checkIn,
            LocalDate checkOut,
            int guestCount,
            RoomType roomType) throws SQLException {
        requireCustomer(customer);
        String normalizedReference = validateReference(reference);
        CustomerBookingData booking = findBookingData(customer.getId(), normalizedReference);
        if (booking.reservation().status() != ReservationStatus.PENDING) {
            throw new ValidationException("Only pending reservations can be changed.");
        }
        validateStay(checkIn, checkOut);
        validateGuestCount(guestCount);
        if (roomType == null) {
            throw new ValidationException("Room type is required.");
        }
        return reservationRepository.findAvailableRooms(
                checkIn, checkOut, guestCount, roomType, normalizedReference);
    }

    private List<Room> findAvailableRoomsForChange(
            AuthenticatedUser customer, String reference, LocalDateTime checkIn, LocalDateTime checkOut,
            int guestCount, RoomType roomType) throws SQLException {
        requireCustomer(customer);
        String normalizedReference = validateReference(reference);
        CustomerBookingData booking = findBookingData(customer.getId(), normalizedReference);
        if (booking.reservation().status() != ReservationStatus.PENDING) {
            throw new ValidationException("Only pending reservations can be changed.");
        }
        validateGuestCount(guestCount);
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn) || roomType == null) {
            throw new ValidationException("Valid stay dates, times, and room type are required.");
        }
        return reservationRepository.findAvailableRooms(
                checkIn, checkOut, guestCount, roomType, normalizedReference);
    }

    public CustomerBookingQuote quote(
            Guest guest, BigDecimal roomRate, LocalDate checkIn, LocalDate checkOut) {
        Guest normalizedGuest = normalizeGuest(guest);
        int nights = nightsBetween(checkIn, checkOut);
        Guest billingGuest = normalizedGuest.classification() == GuestClassification.DISCOUNTED
                ? new Guest(normalizedGuest.id(), normalizedGuest.fullName(),
                        normalizedGuest.contactNumber(), normalizedGuest.email(),
                        GuestClassification.DISCOUNTED, normalizedGuest.discountIdType(),
                        normalizedGuest.discountIdNumber(), true)
                : normalizedGuest;
        Reservation reservation = new Reservation(
                0, "QUOTE", billingGuest, 1, "Quote", roomRate,
                checkIn, checkOut, nights, 1,
                com.hotel.core.model.enums.ReservationType.ONLINE, ReservationStatus.PENDING);
        CheckoutBill bill = reservationService.calculateCheckoutBill(reservation);
        BigDecimal deposit = bill.total().multiply(DOWNPAYMENT_RATE).setScale(2, RoundingMode.HALF_UP);
        return new CustomerBookingQuote(
                bill, deposit, bill.total().subtract(deposit),
                normalizedGuest.classification() == GuestClassification.DISCOUNTED
                        && !normalizedGuest.discountVerified());
    }

    public CustomerReceipt createBooking(
            AuthenticatedUser customer,
            CustomerBookingRequest request,
            CustomerPaymentSubmission payment) throws SQLException, IOException {
        requireCustomer(customer);
        CustomerBookingRequest normalized = normalizeRequest(request);
        validatePayment(payment);
        Room room = findSelectedRoom(customer, normalized, null);
        CustomerBookingQuote quote = quote(
                normalized.guest(), room.getPricePerDay(),
                normalized.checkIn().toLocalDate(), normalized.checkOut().toLocalDate());
        validatePaymentRange(payment.amount(), quote.downpayment(), quote.bill().total());
        Path storedIdProof = null;
        Path storedPaymentProof = null;
        try {
            storedIdProof = storeDiscountIdProof(normalized.guest());
            normalized = withGuestProof(normalized, storedIdProof);
            storedPaymentProof = storePaymentProof(payment.proofFile());
            CustomerBooking booking = reservationRepository.createCustomerOnline(
                    customer.getId(), normalized, room.getPricePerDay(),
                    quote.bill().total(), payment.amount(),
                    normalizedPayment(payment), storedPaymentProof.toString());
            return receipt(booking, quote.bill());
        } catch (SQLException | RuntimeException exception) {
            deleteStoredProofAfterFailure(storedPaymentProof, exception);
            deleteStoredProofAfterFailure(storedIdProof, exception);
            throw exception;
        } catch (IOException exception) {
            deleteStoredProofAfterFailure(storedPaymentProof, exception);
            deleteStoredProofAfterFailure(storedIdProof, exception);
            throw exception;
        }
    }

    public CustomerBooking requestChange(
            AuthenticatedUser customer, String reference, CustomerBookingRequest request)
            throws SQLException, IOException {
        requireCustomer(customer);
        String normalizedReference = validateReference(reference);
        CustomerBookingData existing = findBookingData(customer.getId(), normalizedReference);
        if (existing.reservation().status() != ReservationStatus.PENDING) {
            throw new ValidationException("Only pending reservations can be changed.");
        }
        CustomerBookingRequest normalized = normalizeRequest(request);
        Room room = findSelectedRoom(customer, normalized, normalizedReference);
        CustomerBookingQuote quote = quote(normalized.guest(), room.getPricePerDay(),
                normalized.checkIn().toLocalDate(), normalized.checkOut().toLocalDate());
        if (existing.amountSubmitted().compareTo(quote.bill().total()) > 0) {
            throw new ValidationException(
                    "The updated booking total cannot be less than payments already submitted.");
        }
        String existingIdProof = existing.reservation().guest().discountIdProofPath();
        Path storedIdProof = null;
        try {
            if (normalized.guest().classification() == GuestClassification.DISCOUNTED
                    && !Objects.equals(normalized.guest().discountIdProofPath(), existingIdProof)) {
                storedIdProof = storeDiscountIdProof(normalized.guest());
                normalized = withGuestProof(normalized, storedIdProof);
            }
            reservationRepository.updateCustomerPending(
                    customer.getId(), normalizedReference, normalized,
                    room.getPricePerDay(), quote.bill().total());
        } catch (SQLException | RuntimeException exception) {
            deleteStoredProofAfterFailure(storedIdProof, exception);
            throw exception;
        } catch (IOException exception) {
            deleteStoredProofAfterFailure(storedIdProof, exception);
            throw exception;
        }
        if (existingIdProof != null
                && !Objects.equals(normalized.guest().discountIdProofPath(), existingIdProof)) {
            deleteReplacedIdProof(existingIdProof);
        }
        return findBooking(customer, normalizedReference);
    }

    public void cancelBooking(AuthenticatedUser customer, String reference) throws SQLException {
        requireCustomer(customer);
        String normalizedReference = validateReference(reference);
        CustomerBookingData booking = findBookingData(customer.getId(), normalizedReference);
        if (booking.reservation().status() != ReservationStatus.PENDING) {
            throw new ValidationException("Only pending reservations can be cancelled.");
        }
        reservationRepository.cancelCustomerPending(customer.getId(), normalizedReference);
    }

    public CustomerReceipt payRemainingBalance(
            AuthenticatedUser customer,
            String reference,
            CustomerPaymentSubmission payment) throws SQLException, IOException {
        requireCustomer(customer);
        String normalizedReference = validateReference(reference);
        validatePayment(payment);
        CustomerBooking booking = findBooking(customer, normalizedReference);
        if (booking.reservation().status() == ReservationStatus.CANCELLED
                || booking.reservation().status() == ReservationStatus.CHECKED_OUT) {
            throw new ValidationException("Payments cannot be submitted for this reservation.");
        }
        if (booking.balance().signum() <= 0) {
            throw new ValidationException("There is no remaining balance for this reservation.");
        }
        validatePaymentRange(payment.amount(), booking.balance(), booking.balance());
        Path storedProof = storePaymentProof(payment.proofFile());
        try {
            reservationRepository.submitCustomerPayment(
                    customer.getId(), normalizedReference, CustomerPaymentKind.BALANCE,
                    payment.amount(), booking.total(), normalizedPayment(payment), storedProof.toString());
        } catch (SQLException | RuntimeException exception) {
            deleteStoredProofAfterFailure(storedProof, exception);
            throw exception;
        }
        return findReceipt(customer, normalizedReference);
    }

    public List<CustomerBooking> findBookings(AuthenticatedUser customer) throws SQLException {
        requireCustomer(customer);
        return reservationRepository.findCustomerBookings(customer.getId()).stream()
                .map(this::toBooking)
                .toList();
    }

    public CustomerBooking findBooking(AuthenticatedUser customer, String reference) throws SQLException {
        requireCustomer(customer);
        return toBooking(findBookingData(customer.getId(), validateReference(reference)));
    }

    public CustomerReceipt findReceipt(AuthenticatedUser customer, String reference) throws SQLException {
        CustomerBooking booking = findBooking(customer, reference);
        CheckoutBill bill = calculateBill(booking.reservation());
        return receipt(booking, bill);
    }

    public List<CustomerNotification> findNotifications(AuthenticatedUser customer) throws SQLException {
        requireCustomer(customer);
        return reservationRepository.findCustomerNotifications(customer.getId());
    }

    public int countUnreadNotifications(AuthenticatedUser customer) throws SQLException {
        requireCustomer(customer);
        return reservationRepository.countUnreadCustomerNotifications(customer.getId());
    }

    public void markNotificationRead(AuthenticatedUser customer, long notificationId) throws SQLException {
        requireCustomer(customer);
        if (notificationId < 1) {
            throw new ValidationException("Notification id must be a positive number.");
        }
        reservationRepository.markCustomerNotificationRead(customer.getId(), notificationId);
    }

    private CustomerBookingData findBookingData(int customerId, String reference) throws SQLException {
        CustomerBookingData booking = reservationRepository.findCustomerBooking(customerId, reference);
        if (booking == null) {
            throw new ValidationException("Reservation not found.");
        }
        return booking;
    }

    private CustomerBooking toBooking(CustomerBookingData data) {
        Reservation reservation = data.reservation();
        CheckoutBill bill = calculateBill(reservation);
        BigDecimal amountSubmitted = data.amountSubmitted() == null
                ? BigDecimal.ZERO.setScale(2) : data.amountSubmitted().setScale(2, RoundingMode.HALF_UP);
        BigDecimal balance = bill.total().subtract(amountSubmitted).max(BigDecimal.ZERO).setScale(2);
        String methods = data.paymentMethods() == null || data.paymentMethods().isBlank()
                ? "Not submitted" : data.paymentMethods();
        return new CustomerBooking(
                reservation, data.checkInTime(), data.checkOutTime(), data.paymentStatus(),
                bill.total(), amountSubmitted, balance, methods);
    }

    private CheckoutBill calculateBill(Reservation reservation) {
        Guest guest = reservation.guest();
        if (guest.classification() == GuestClassification.DISCOUNTED && !guest.discountVerified()) {
            guest = new Guest(guest.id(), guest.fullName(), guest.contactNumber(), guest.email(),
                    guest.classification(), guest.discountIdType(), guest.discountIdNumber(), true);
            reservation = new Reservation(
                    reservation.id(), reservation.reference(), guest, reservation.roomNumber(),
                    reservation.roomType(), reservation.roomRate(), reservation.checkIn(),
                    reservation.checkOut(), reservation.nights(), reservation.guestCount(),
                    reservation.type(), reservation.status());
        }
        return reservationService.calculateCheckoutBill(reservation);
    }

    private CustomerReceipt receipt(CustomerBooking booking, CheckoutBill bill) {
        return new CustomerReceipt(
                booking, bill, booking.amountSubmitted(), booking.balance(),
                booking.paymentStatus(), booking.paymentMethods());
    }

    private Room findSelectedRoom(
            AuthenticatedUser customer, CustomerBookingRequest request, String excludedReference)
            throws SQLException {
        List<Room> available = excludedReference == null
                ? findAvailableRooms(customer, request.checkIn(),
                        request.checkOut(), request.guestCount(), request.roomType())
                : findAvailableRoomsForChange(customer, excludedReference,
                        request.checkIn(), request.checkOut(),
                        request.guestCount(), request.roomType());
        return available.stream()
                .filter(room -> room.getRoomNumber() == request.roomNumber())
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "The selected room is not available for the requested dates."));
    }

    private static CustomerBookingRequest normalizeRequest(CustomerBookingRequest request) {
        if (request == null) {
            throw new ValidationException("Booking details are required.");
        }
        Guest guest = normalizeGuest(request.guest());
        if (request.roomNumber() < 1) {
            throw new ValidationException("Select an available room.");
        }
        if (request.roomType() == null) {
            throw new ValidationException("Room type is required.");
        }
        if (request.checkIn() == null || request.checkOut() == null) {
            throw new ValidationException("Check-in and check-out dates and times are required.");
        }
        validateStay(request.checkIn().toLocalDate(), request.checkOut().toLocalDate());
        if (!request.checkOut().isAfter(request.checkIn())) {
            throw new ValidationException("Check-out must be after check-in.");
        }
        if (request.checkIn().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Check-in cannot be in the past.");
        }
        validateGuestCount(request.guestCount());
        return new CustomerBookingRequest(
                guest, request.roomNumber(), request.roomType(),
                request.checkIn(), request.checkOut(), request.guestCount());
    }

    private static Guest normalizeGuest(Guest guest) {
        if (guest == null) {
            throw new ValidationException("Guest details are required.");
        }
        String fullName = requireText(guest.fullName(), "Guest name");
        String contact = requireText(guest.contactNumber(), "Contact number");
        String email = requireText(guest.email(), "Email").toLowerCase(java.util.Locale.ROOT);
        AuthenticationService.validateCustomerAccount(fullName, email, contact);
        if (guest.classification() == null) {
            throw new ValidationException("Guest classification is required.");
        }
        if (guest.classification() == GuestClassification.DISCOUNTED) {
            String idNumber = requireText(guest.discountIdNumber(), "Discount ID number");
            String proofPath = guest.discountIdProofPath();
            if (idNumber.length() > 100) {
                throw new ValidationException("Discount ID details are too long.");
            }
            return new Guest(0, fullName, contact, email, guest.classification(),
                    null, idNumber, false, proofPath);
        }
        return new Guest(0, fullName, contact, email, guest.classification(), null, null, false);
    }

    private Path storeDiscountIdProof(Guest guest) throws IOException {
        if (guest.classification() != GuestClassification.DISCOUNTED) {
            return null;
        }
        String proofPath = requireText(guest.discountIdProofPath(), "Discount ID proof");
        try {
            return proofStorage.storeAnyProof(Path.of(proofPath));
        } catch (ValidationException exception) {
            throw new ValidationException("Discount ID proof could not be stored. " + exception.getMessage());
        }
    }

    private Path storePaymentProof(Path proofFile) throws IOException {
        try {
            return proofStorage.storePaymentProof(proofFile);
        } catch (ValidationException exception) {
            throw new ValidationException("Payment proof: " + exception.getMessage());
        }
    }

    private static CustomerBookingRequest withGuestProof(CustomerBookingRequest request, Path proof) {
        Guest guest = request.guest();
        Guest updatedGuest = new Guest(
                guest.id(), guest.fullName(), guest.contactNumber(), guest.email(),
                guest.classification(), null, guest.discountIdNumber(), false,
                proof == null ? null : proof.toString());
        return new CustomerBookingRequest(
                updatedGuest, request.roomNumber(), request.roomType(),
                request.checkIn(), request.checkOut(), request.guestCount());
    }

    private void deleteReplacedIdProof(String oldPath) {
        try {
            proofStorage.deleteStored(Path.of(oldPath));
        } catch (IOException exception) {
            java.util.logging.Logger.getLogger(CustomerReservationService.class.getName())
                    .warning("Could not remove replaced customer ID proof: " + exception.getMessage());
        }
    }

    private static CustomerPaymentSubmission normalizedPayment(CustomerPaymentSubmission payment) {
        return new CustomerPaymentSubmission(
                payment.method(), payment.amount().setScale(2, RoundingMode.UNNECESSARY),
                payment.referenceDetails().trim(), payment.proofFile());
    }

    private static void validatePayment(CustomerPaymentSubmission payment) {
        if (payment == null || payment.method() == null) {
            throw new ValidationException("Choose a payment method.");
        }
        String reference = requireText(payment.referenceDetails(), "Payment reference or details");
        if (reference.length() > 255) {
            throw new ValidationException("Payment reference or details must be 255 characters or fewer.");
        }
        if (payment.proofFile() == null) {
            throw new ValidationException("Select a payment proof file.");
        }
        validatePaymentAmount(payment.amount(), null);
    }

    private static void validatePaymentRange(
            BigDecimal amount, BigDecimal minimum, BigDecimal maximum) {
        validatePaymentAmount(amount, maximum);
        if (amount.compareTo(minimum) < 0) {
            throw new ValidationException("The minimum payment is PHP "
                    + minimum.setScale(2, RoundingMode.HALF_UP).toPlainString() + ".");
        }
    }

    private static void validatePaymentAmount(BigDecimal amount, BigDecimal maximum) {
        if (amount == null || amount.signum() <= 0) {
            throw new ValidationException("Payment amount must be greater than zero.");
        }
        try {
            amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ValidationException("Payment amount must have no more than two decimal places.");
        }
        if (maximum != null && amount.compareTo(maximum) > 0) {
            throw new ValidationException("Payment amount cannot exceed the remaining booking balance.");
        }
    }

    private static void validateStay(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out date must be after check-in date.");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new ValidationException("Check-in date cannot be in the past.");
        }
        nightsBetween(checkIn, checkOut);
    }

    private static int nightsBetween(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new ValidationException("Check-out date must be after check-in date.");
        }
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights > Integer.MAX_VALUE) {
            throw new ValidationException("The stay duration is too long.");
        }
        return Math.toIntExact(nights);
    }

    private static void validateGuestCount(int guestCount) {
        if (guestCount < 1) {
            throw new ValidationException("At least one guest is required.");
        }
    }

    private static String validateReference(String reference) {
        String value = requireText(reference, "Reservation reference");
        if (value.length() > 30) {
            throw new ValidationException("Reservation reference is invalid.");
        }
        return value;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(label + " is required.");
        }
        return value.trim();
    }

    private static void requireCustomer(AuthenticatedUser customer) {
        if (customer == null || customer.getRole() == null
                || customer.getRole() != com.hotel.core.model.UserRole.CUSTOMER
                || customer.getId() < 1) {
            throw new ValidationException("A signed-in customer account is required.");
        }
    }

    private void deleteStoredProofAfterFailure(Path storedProof, Exception failure) {
        if (storedProof == null) {
            return;
        }
        try {
            proofStorage.deleteStored(storedProof);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }
}
