package com.hotel.core.service;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.CheckoutBill;
import com.hotel.core.model.CheckoutReceipt;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.Guest;
import com.hotel.core.model.Reservation;
import com.hotel.core.model.Room;
import com.hotel.core.model.enums.GuestClassification;
import com.hotel.core.model.enums.CustomerPaymentStatus;
import com.hotel.core.model.enums.HousekeepingStatus;
import com.hotel.core.model.enums.ReservationStatus;
import com.hotel.core.model.enums.ReservationType;
import com.hotel.core.model.enums.RoomType;
import com.hotel.core.model.UserRole;
import com.hotel.core.repository.ReservationRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationServiceTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final AuthenticatedUser ADMIN =
            new AuthenticatedUser(1, "admin", null, null, "Hotel Admin", UserRole.ADMIN);
    private static final AuthenticatedUser CUSTOMER =
            new AuthenticatedUser(2, "guest@example.com", "guest@example.com",
                    "09170000000", "Hotel Guest", UserRole.CUSTOMER);

    @Test
    void createsWalkInAsConfirmedAndStartingToday() throws Exception {
        StubReservationRepository repository = new StubReservationRepository();
        ReservationService service = new ReservationService(repository);

        service.createWalkIn(ADMIN, regularGuest(), 101, TODAY, TODAY.plusDays(2), 1);

        assertEquals(ReservationType.WALK_IN, repository.createdType);
        assertEquals(ReservationStatus.CONFIRMED, repository.createdStatus);
        assertEquals(2, repository.createdNights);
    }

    @Test
    void rejectsInvalidDatesAndGuestCountBeforePersistence() {
        StubReservationRepository repository = new StubReservationRepository();
        ReservationService service = new ReservationService(repository);

        assertThrows(ValidationException.class,
                () -> service.createWalkIn(ADMIN, regularGuest(), 101, TODAY, TODAY, 1));
        assertThrows(ValidationException.class,
                () -> service.createWalkIn(ADMIN, regularGuest(), 101, TODAY, TODAY.plusDays(1), 0));
        assertThrows(ValidationException.class,
                () -> service.createWalkIn(
                        ADMIN, regularGuest(), 101, TODAY.plusDays(1), TODAY.plusDays(2), 1));
        assertEquals(0, repository.createCalls);
    }

    @Test
    void requiresVerifiedDiscountIdBeforeCreatingDiscountedGuest() {
        Guest unverified = new Guest(0, "Guest", "09170000000", "guest@example.com",
                GuestClassification.DISCOUNTED, "Senior Citizen", "SC-001", false);

        assertThrows(ValidationException.class,
                () -> new ReservationService(new StubReservationRepository())
                        .createWalkIn(ADMIN, unverified, 101, TODAY, TODAY.plusDays(1), 1));
    }

    @Test
    void calculatesRegularTaxAndVerifiedDiscountCorrectly() {
        ReservationService service = new ReservationService(new StubReservationRepository());

        CheckoutBill regular = service.calculateCheckoutBill(reservation(regularGuest()));
        assertEquals(new BigDecimal("200.00"), regular.roomCost());
        assertEquals(new BigDecimal("24.00"), regular.taxAmount());
        assertEquals(new BigDecimal("224.00"), regular.total());

        Guest discounted = new Guest(1, "Guest", "09170000000", "guest@example.com",
                GuestClassification.DISCOUNTED, "Senior Citizen", "SC-001", true);
        CheckoutBill discountedBill = service.calculateCheckoutBill(reservation(discounted));
        assertEquals(new BigDecimal("40.00"), discountedBill.discountAmount());
        assertEquals(new BigDecimal("0.00"), discountedBill.taxAmount());
        assertEquals(new BigDecimal("160.00"), discountedBill.total());
    }

    @Test
    void rejectsCashBelowAmountDueAndCheckoutBeforeDeparture() {
        StubReservationRepository repository = new StubReservationRepository();
        repository.reservation = reservation(regularGuest());
        ReservationService service = new ReservationService(repository);

        assertThrows(ValidationException.class,
                () -> service.checkOut(ADMIN, "RES-TEST", new BigDecimal("223.99"), TODAY.plusDays(2)));
        assertThrows(ValidationException.class,
                () -> service.checkOut(ADMIN, "RES-TEST", new BigDecimal("224.00"), TODAY.plusDays(1)));
        assertEquals(0, repository.checkoutCalls);
    }

    @Test
    void checksOutWithCalculatedChange() throws Exception {
        StubReservationRepository repository = new StubReservationRepository();
        repository.reservation = reservation(regularGuest());
        ReservationService service = new ReservationService(repository);

        CheckoutReceipt receipt = service.checkOut(
                ADMIN, "RES-TEST", new BigDecimal("250.000"), TODAY.plusDays(2));

        assertNotNull(receipt);
        assertEquals(new BigDecimal("250.00"), receipt.cashTendered());
        assertEquals(new BigDecimal("26.00"), receipt.change());
        assertEquals(1, repository.checkoutCalls);
    }

    @Test
    void customerCannotUseStaffReservationOperations() {
        StubReservationRepository repository = new StubReservationRepository();
        ReservationService service = new ReservationService(repository);

        assertThrows(ValidationException.class,
                () -> service.createWalkIn(CUSTOMER, regularGuest(), 101, TODAY, TODAY.plusDays(1), 1));
        assertThrows(ValidationException.class,
                () -> service.findArrivals(CUSTOMER, TODAY));
        assertThrows(ValidationException.class,
                () -> service.checkIn(CUSTOMER, "RES-TEST", TODAY));
        assertThrows(ValidationException.class,
                () -> service.checkOut(CUSTOMER, "RES-TEST", BigDecimal.TEN, TODAY));
        assertEquals(0, repository.createCalls);
        assertEquals(0, repository.checkoutCalls);
    }

    @Test
    void forceCheckoutRequiresAdminAndReason() throws Exception {
        StubReservationRepository repository = new StubReservationRepository();
        ReservationService service = new ReservationService(repository);

        assertThrows(ValidationException.class,
                () -> service.forceCheckOut(CUSTOMER, "RES-TEST", "policy"));
        assertThrows(ValidationException.class,
                () -> service.forceCheckOut(ADMIN, "RES-TEST", "  "));
        service.forceCheckOut(ADMIN, "RES-TEST", "  Safety concern  ");

        assertEquals("RES-TEST", repository.forcedCheckoutReference);
        assertEquals("Safety concern", repository.forcedCheckoutReason);
    }

    @Test
    void onlineCheckoutRequiresFullyVerifiedBalanceAndDoesNotCollectCashAgain() throws Exception {
        StubReservationRepository repository = new StubReservationRepository();
        Guest guest = regularGuest();
        repository.reservation = new Reservation(
                1, "RES-TEST", guest, 101, "Single", new BigDecimal("100.00"),
                TODAY.minusDays(2), TODAY, 2, 1, ReservationType.ONLINE, ReservationStatus.CHECKED_IN);
        ReservationService service = new ReservationService(repository);

        repository.paymentStatus = CustomerPaymentStatus.PARTIALLY_PAID;
        assertThrows(ValidationException.class,
                () -> service.checkOutOnline(ADMIN, "RES-TEST", TODAY));
        assertEquals(0, repository.onlineCheckoutCalls);

        repository.paymentStatus = CustomerPaymentStatus.FULLY_PAID;
        service.checkOutOnline(ADMIN, "RES-TEST", TODAY);
        assertEquals(1, repository.onlineCheckoutCalls);
        assertThrows(ValidationException.class,
                () -> service.checkOut(ADMIN, "RES-TEST", BigDecimal.TEN, TODAY));
    }

    private static Guest regularGuest() {
        return new Guest(1, "Guest", "09170000000", "guest@example.com",
                GuestClassification.REGULAR, null, null, false);
    }

    private static Reservation reservation(Guest guest) {
        return new Reservation(1, "RES-TEST", guest, 101, "Single", new BigDecimal("100.00"),
                TODAY, TODAY.plusDays(2), 2, 1, ReservationType.WALK_IN, ReservationStatus.CHECKED_IN);
    }

    private static final class StubReservationRepository extends ReservationRepository {

        private int createCalls;
        private int checkoutCalls;
        private String forcedCheckoutReference;
        private String forcedCheckoutReason;
        private int onlineCheckoutCalls;
        private CustomerPaymentStatus paymentStatus = CustomerPaymentStatus.UNPAID;
        private int createdNights;
        private ReservationType createdType;
        private ReservationStatus createdStatus;
        private Reservation reservation;

        @Override
        public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut, int guestCount) {
            return List.of(new Room(101, 1, RoomType.SINGLE, 1, "Single room",
                    new BigDecimal("100.00"), false, HousekeepingStatus.CLEAN));
        }

        @Override
        public Reservation create(
                Guest guest, int roomNumber, LocalDate checkIn, LocalDate checkOut,
                int guestCount, ReservationType type, ReservationStatus initialStatus) {
            createCalls++;
            createdType = type;
            createdStatus = initialStatus;
            createdNights = Math.toIntExact(java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut));
            return new Reservation(1, "RES-TEST", guest, roomNumber, "Single",
                    new BigDecimal("100.00"), checkIn, checkOut, createdNights, guestCount, type, initialStatus);
        }

        @Override
        public Reservation findByReference(String reference) {
            return reservation;
        }

        @Override
        public CheckoutReceipt checkOut(String reference, BigDecimal cashTendered, CheckoutBill bill) {
            checkoutCalls++;
            return new CheckoutReceipt("RCT-TEST", reservation, bill, cashTendered,
                    cashTendered.subtract(bill.total()), LocalDateTime.now());
        }

        @Override
        public void forceCheckOut(String reference, String reason) {
            forcedCheckoutReference = reference;
            forcedCheckoutReason = reason;
        }

        @Override
        public CustomerPaymentStatus findPaymentStatus(String reference) {
            return paymentStatus;
        }

        @Override
        public void checkOutOnline(String reference) {
            onlineCheckoutCalls++;
        }
    }
}
