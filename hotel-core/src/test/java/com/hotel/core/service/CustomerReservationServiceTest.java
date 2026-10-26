package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.CustomerBooking;
import com.hotel.core.model.CustomerBookingData;
import com.hotel.core.model.CustomerBookingQuote;
import com.hotel.core.model.CustomerBookingRequest;
import com.hotel.core.model.CustomerPaymentSubmission;
import com.hotel.core.model.CustomerReceipt;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.UserRole;
import com.hotel.core.model.enums.CustomerPaymentStatus;
import com.hotel.core.model.enums.CustomerPaymentMethod;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.ReservationType;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerReservationServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void calculatesRegularQuoteAndHalfDownpayment() {
        CustomerBookingQuote quote = service(new StubReservationRepository()).quote(
                guest(GuestClassification.REGULAR, false),
                new BigDecimal("2500.00"), LocalDate.now().plusDays(5), LocalDate.now().plusDays(8));

        assertEquals(new BigDecimal("8400.00"), quote.bill().total());
        assertEquals(new BigDecimal("4200.00"), quote.downpayment());
        assertEquals(new BigDecimal("4200.00"), quote.remainingBalance());
        assertEquals(new BigDecimal("900.00"), quote.bill().taxAmount());
        assertEquals(false, quote.discountIsProvisional());
    }

    @Test
    void calculatesProvisionalDiscountedQuoteAndHalfDownpayment() {
        CustomerBookingQuote quote = service(new StubReservationRepository()).quote(
                guest(GuestClassification.DISCOUNTED, false),
                new BigDecimal("2500.00"), LocalDate.now().plusDays(5), LocalDate.now().plusDays(8));

        assertEquals(new BigDecimal("6000.00"), quote.bill().total());
        assertEquals(new BigDecimal("3000.00"), quote.downpayment());
        assertEquals(new BigDecimal("3000.00"), quote.remainingBalance());
        assertEquals(new BigDecimal("1500.00"), quote.bill().discountAmount());
        assertEquals(new BigDecimal("0.00"), quote.bill().taxAmount());
        assertEquals(true, quote.discountIsProvisional());
    }

    @Test
    void rejectsAdminAccountsFromCustomerReservationOperations() {
        CustomerReservationService service = service(new StubReservationRepository());
        AuthenticatedUser admin = new AuthenticatedUser(
                1, "admin", "admin@example.com", "09170000000", "Admin", UserRole.ADMIN);

        assertThrows(ValidationException.class, () -> service.findAvailableRooms(
                admin, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), 1, RoomType.SINGLE));
    }

    @Test
    void onlyPendingReservationsCanBeChangedOrCancelled() {
        StubReservationRepository repository = new StubReservationRepository();
        repository.booking = pendingBooking(ReservationStatus.CONFIRMED);
        CustomerReservationService service = service(repository);
        AuthenticatedUser customer = customer();
        CustomerBookingRequest request = new CustomerBookingRequest(
                guest(GuestClassification.REGULAR, false), 101, RoomType.SINGLE,
                LocalDate.now().plusDays(4).atTime(15, 0),
                LocalDate.now().plusDays(6).atTime(11, 0), 1);

        assertThrows(ValidationException.class,
                () -> service.requestChange(customer, "ONL-TEST", request));
        assertThrows(ValidationException.class,
                () -> service.cancelBooking(customer, "ONL-TEST"));
        assertEquals(0, repository.cancelCalls);
    }

    @Test
    void pendingReservationChangeExcludesItsOwnRoomFromAvailabilityCheck() throws Exception {
        StubReservationRepository repository = new StubReservationRepository();
        repository.booking = pendingBooking(ReservationStatus.PENDING);
        CustomerReservationService service = service(repository);
        CustomerBookingRequest request = new CustomerBookingRequest(
                guest(GuestClassification.REGULAR, false), 101, RoomType.SINGLE,
                LocalDate.now().plusDays(4).atTime(15, 0),
                LocalDate.now().plusDays(6).atTime(11, 0), 1);

        service.requestChange(customer(), "ONL-TEST", request);

        assertEquals("ONL-TEST", repository.excludedReference);
        assertEquals(1, repository.changeCalls);
    }

    @Test
    void createsPendingOnlineReservationWithRequiredDownpaymentAndStoredProof() throws Exception {
        StubReservationRepository repository = new StubReservationRepository();
        CustomerReservationService service = service(repository);
        AuthenticatedUser customer = customer();
        CustomerBookingRequest request = new CustomerBookingRequest(
                guest(GuestClassification.REGULAR, false), 101, RoomType.SINGLE,
                LocalDate.now().plusDays(3).atTime(15, 0),
                LocalDate.now().plusDays(5).atTime(11, 0), 1);
        Path proof = tempDirectory.resolve("proof.png");
        Files.write(proof, new byte[] {
                (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10
        });

        CustomerReceipt receipt = service.createBooking(customer, request,
                new CustomerPaymentSubmission(
                        CustomerPaymentMethod.GCASH, new BigDecimal("112.00"), "REF-123", proof));

        assertEquals(new BigDecimal("224.00"), repository.createdTotal);
        assertEquals(new BigDecimal("112.00"), repository.createdDownpayment);
        assertEquals(ReservationStatus.PENDING, receipt.booking().reservation().status());
        assertEquals(CustomerPaymentStatus.PARTIALLY_PAID, receipt.paymentStatus());
        assertEquals(new BigDecimal("112.00"), receipt.remainingBalance());
        assertTrue(Files.isRegularFile(Path.of(repository.createdProofPath)));
        assertEquals(false, receipt.booking().reservation().guest().discountVerified());
    }

    @Test
    void rejectsDownpaymentBelowRequiredMinimumBeforeCreatingReservation() {
        StubReservationRepository repository = new StubReservationRepository();
        CustomerReservationService service = service(repository);
        CustomerBookingRequest request = new CustomerBookingRequest(
                guest(GuestClassification.REGULAR, false), 101, RoomType.SINGLE,
                LocalDate.now().plusDays(3).atTime(15, 0),
                LocalDate.now().plusDays(5).atTime(11, 0), 1);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.createBooking(customer(), request,
                        new CustomerPaymentSubmission(
                                CustomerPaymentMethod.GCASH, new BigDecimal("111.99"),
                                "REF-123", tempDirectory.resolve("proof.png"))));

        assertTrue(exception.getMessage().contains("minimum payment is PHP 112.00"));
        assertNull(repository.createdTotal);
    }

    @Test
    void rejectsBalancePaymentBelowFullRemainingAmount() {
        StubReservationRepository repository = new StubReservationRepository();
        repository.booking = pendingBooking(ReservationStatus.CONFIRMED);
        CustomerReservationService service = service(repository);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.payRemainingBalance(customer(), "ONL-TEST",
                        new CustomerPaymentSubmission(
                                CustomerPaymentMethod.GCASH, new BigDecimal("111.99"),
                                "REF-123", tempDirectory.resolve("proof.png"))));

        assertTrue(exception.getMessage().contains("minimum payment is PHP 112.00"));
    }

    @Test
    void rejectsPaymentAboveBookingTotal() {
        StubReservationRepository repository = new StubReservationRepository();
        CustomerReservationService service = service(repository);
        CustomerBookingRequest request = new CustomerBookingRequest(
                guest(GuestClassification.REGULAR, false), 101, RoomType.SINGLE,
                LocalDate.now().plusDays(3).atTime(15, 0),
                LocalDate.now().plusDays(5).atTime(11, 0), 1);

        assertThrows(ValidationException.class, () -> service.createBooking(customer(), request,
                new CustomerPaymentSubmission(
                        CustomerPaymentMethod.GCASH, new BigDecimal("224.01"),
                        "REF-123", tempDirectory.resolve("proof.png"))));
        assertNull(repository.createdTotal);
    }

    private CustomerReservationService service(ReservationRepository repository) {
        return new CustomerReservationService(repository, new PaymentProofStorage(tempDirectory));
    }

    private static AuthenticatedUser customer() {
        return new AuthenticatedUser(
                7, "customer@example.com", "customer@example.com",
                "09170000000", "Customer", UserRole.CUSTOMER);
    }

    private static Guest guest(GuestClassification classification, boolean verified) {
        return new Guest(0, "Guest Name", "09170000000", "guest@example.com", classification,
                classification == GuestClassification.DISCOUNTED ? "Senior Citizen" : null,
                classification == GuestClassification.DISCOUNTED ? "SC-001" : null, verified);
    }

    private static CustomerBookingData pendingBooking(ReservationStatus status) {
        Reservation reservation = new Reservation(
                1, "ONL-TEST", guest(GuestClassification.REGULAR, false), 101,
                "Single", new BigDecimal("100.00"),
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), 2, 1,
                ReservationType.ONLINE, status);
        return new CustomerBookingData(
                reservation, LocalTime.of(15, 0), LocalTime.of(11, 0),
                CustomerPaymentStatus.PARTIALLY_PAID, new BigDecimal("112.00"), "GCash");
    }

    private static final class StubReservationRepository extends ReservationRepository {
        private CustomerBookingData booking;
        private int cancelCalls;
        private int changeCalls;
        private BigDecimal createdTotal;
        private BigDecimal createdDownpayment;
        private String createdProofPath;
        private String excludedReference;

        @Override
        public List<Room> findAvailableRooms(
                LocalDate checkIn, LocalDate checkOut, int guestCount, RoomType roomType) {
            return List.of(new Room(101, 1, RoomType.SINGLE, 1,
                    "Single room", new BigDecimal("100.00"), false,
                    com.hotel.core.model.enums.HousekeepingStatus.CLEAN));
        }

        @Override
        public List<Room> findAvailableRooms(
                LocalDate checkIn,
                LocalDate checkOut,
                int guestCount,
                RoomType roomType,
                String excludedReference) {
            this.excludedReference = excludedReference;
            return findAvailableRooms(checkIn, checkOut, guestCount, roomType);
        }

        @Override
        public List<Room> findAvailableRooms(
                LocalDateTime checkIn,
                LocalDateTime checkOut,
                int guestCount,
                RoomType roomType,
                String excludedReference) {
            this.excludedReference = excludedReference;
            return findAvailableRooms(
                    checkIn.toLocalDate(), checkOut.toLocalDate(), guestCount, roomType);
        }

        @Override
        public CustomerBookingData findCustomerBooking(int customerUserId, String reference) {
            return booking;
        }

        @Override
        public void cancelCustomerPending(int customerUserId, String reference) throws SQLException {
            cancelCalls++;
        }

        @Override
        public void updateCustomerPending(
                int customerUserId,
                String reference,
                CustomerBookingRequest request,
                BigDecimal expectedRoomRate,
                BigDecimal updatedTotal) throws SQLException {
            changeCalls++;
        }

        @Override
        public CustomerBooking createCustomerOnline(
                int customerUserId,
                CustomerBookingRequest request,
                BigDecimal expectedRoomRate,
                BigDecimal total,
                BigDecimal downpayment,
                com.hotel.core.model.CustomerPaymentSubmission payment,
                String proofPath) throws SQLException {
            createdTotal = total;
            createdDownpayment = downpayment;
            createdProofPath = proofPath;
            int nights = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(
                    request.checkIn().toLocalDate(), request.checkOut().toLocalDate()));
            Reservation reservation = new Reservation(
                    9, "ONL-TEST", request.guest(), request.roomNumber(),
                    request.roomType().getDatabaseValue(), expectedRoomRate,
                    request.checkIn().toLocalDate(), request.checkOut().toLocalDate(),
                    nights, request.guestCount(), ReservationType.ONLINE, ReservationStatus.PENDING);
            return new CustomerBooking(
                    reservation, request.checkIn().toLocalTime(), request.checkOut().toLocalTime(),
                    CustomerPaymentStatus.PARTIALLY_PAID, total, downpayment,
                    total.subtract(downpayment), payment.method().getDatabaseValue());
        }
    }
}
