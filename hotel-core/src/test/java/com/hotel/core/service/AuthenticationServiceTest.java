package com.hotel.core.service;

import com.hotel.core.exception.InvalidCredentialsException;
import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.AuthenticatedUser;
import com.hotel.core.model.StoredUserAccount;
import com.hotel.core.model.UserRole;
import com.hotel.core.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationServiceTest {

    private static final String PASSWORD = "admin secure password";
    private final PasswordHasher passwordHasher = new PasswordHasher(10_000);

    @Test
    void authenticatesEnabledAdminByUsernameAndReturnsOnlyTheirProfile() throws Exception {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.accounts.add(account(true, PASSWORD, UserRole.ADMIN));

        AuthenticatedUser user = service(repository).authenticate(" frontdesk ", PASSWORD.toCharArray());

        assertEquals(17, user.getId());
        assertEquals("frontdesk", user.getUsername());
        assertEquals("Front Desk", user.getFullName());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertTrue(user.getRole().canAccessAdminWorkspace());
    }

    @Test
    void authenticatesCustomersByEmailAndDoesNotGrantAdminWorkspaceAccess() throws Exception {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.accounts.add(new StoredUserAccount(
                18, "guest@example.com", passwordHasher.hash(PASSWORD.toCharArray()),
                "Guest Name", "guest@example.com", "09171234567", UserRole.CUSTOMER, true));

        AuthenticatedUser user = service(repository).authenticate(" GUEST@example.com ", PASSWORD.toCharArray());

        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertEquals("guest@example.com", user.getEmail());
        assertFalse(user.getRole().canAccessAdminWorkspace());
    }

    @Test
    void givesTheSameGenericErrorForMissingWrongAndDisabledAccounts() {
        AuthenticationService missingAccountService = service(new InMemoryUserRepository());
        assertInvalidCredentials(
                () -> missingAccountService.authenticate("unknown", PASSWORD.toCharArray()));

        InMemoryUserRepository wrongPasswordRepository = new InMemoryUserRepository();
        wrongPasswordRepository.accounts.add(account(true, PASSWORD, UserRole.ADMIN));
        assertInvalidCredentials(
                () -> service(wrongPasswordRepository).authenticate("frontdesk", "wrong password".toCharArray()));

        InMemoryUserRepository disabledRepository = new InMemoryUserRepository();
        disabledRepository.accounts.add(account(false, PASSWORD, UserRole.ADMIN));
        assertInvalidCredentials(
                () -> service(disabledRepository).authenticate("frontdesk", PASSWORD.toCharArray()));
    }

    @Test
    void rejectsEmptyCredentialsWithoutQueryingRepository() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AuthenticationService authenticationService = service(repository);

        assertInvalidCredentials(() -> authenticationService.authenticate(" ", PASSWORD.toCharArray()));
        assertInvalidCredentials(() -> authenticationService.authenticate("frontdesk", new char[0]));
        assertEquals(0, repository.lookupCount);
    }

    @Test
    void registersValidatedCustomerWithOnlyAHashedPasswordAndCustomerRole() throws Exception {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        char[] password = "a secure customer password".toCharArray();

        AuthenticatedUser user = service(repository).registerCustomer(
                " Guest Name ", " GUEST@example.com ", "0917 123 4567", password);

        StoredUserAccount created = repository.accounts.get(0);
        assertEquals("guest@example.com", created.getUsername());
        assertEquals("guest@example.com", created.getEmail());
        assertEquals("Guest Name", created.getFullName());
        assertEquals("0917 123 4567", created.getContactNumber());
        assertEquals(UserRole.CUSTOMER, created.getRole());
        assertFalse(created.getPasswordHash().contains(new String(password)));
        assertTrue(passwordHasher.verify(password, created.getPasswordHash()));
        assertEquals(UserRole.CUSTOMER, user.getRole());
        assertEquals(100, user.getId());
    }

    @Test
    void rejectsDuplicateEmailWithoutCreatingAnotherAccount() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.accounts.add(new StoredUserAccount(
                18, "guest@example.com", "hash", "Guest", "guest@example.com",
                "09171234567", UserRole.CUSTOMER, true));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service(repository).registerCustomer(
                        "Another Guest", "GUEST@example.com", "09171234567", PASSWORD.toCharArray()));

        assertEquals("An account already exists for that email.", exception.getMessage());
        assertEquals(1, repository.accounts.size());
    }

    @Test
    void rejectsDuplicateEmailIfAnotherSignupWinsTheDatabaseRace() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.createFailure = new java.sql.SQLIntegrityConstraintViolationException("duplicate email");

        assertThrows(ValidationException.class, () -> service(repository).registerCustomer(
                "Guest", "guest@example.com", "09171234567", PASSWORD.toCharArray()));
        assertTrue(repository.accounts.isEmpty());
    }

    @Test
    void rejectsInvalidCustomerProfileBeforeWriting() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AuthenticationService authenticationService = service(repository);

        assertThrows(ValidationException.class, () -> authenticationService.registerCustomer(
                " ", "guest@example.com", "09171234567", PASSWORD.toCharArray()));
        assertThrows(ValidationException.class, () -> authenticationService.registerCustomer(
                "Guest", "not-an-email", "09171234567", PASSWORD.toCharArray()));
        assertThrows(ValidationException.class, () -> authenticationService.registerCustomer(
                "Guest", "guest@example.com", "bad", PASSWORD.toCharArray()));
        assertEquals(0, repository.accounts.size());
    }

    @Test
    void propagatesDatabaseFailuresSeparatelyFromInvalidCredentials() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.failure = new SQLException("database unavailable");

        SQLException exception = assertThrows(SQLException.class,
                () -> service(repository).authenticate("frontdesk", PASSWORD.toCharArray()));
        assertEquals("database unavailable", exception.getMessage());
    }

    private AuthenticationService service(UserRepository repository) {
        return new AuthenticationService(repository, passwordHasher);
    }

    private StoredUserAccount account(boolean enabled, String password, UserRole role) {
        return new StoredUserAccount(
                17, "frontdesk", passwordHasher.hash(password.toCharArray()),
                "Front Desk", null, null, role, enabled);
    }

    private static void assertInvalidCredentials(ThrowingAuthentication authentication) {
        InvalidCredentialsException exception =
                assertThrows(InvalidCredentialsException.class, authentication::authenticate);
        assertEquals(InvalidCredentialsException.GENERIC_MESSAGE, exception.getMessage());
    }

    @FunctionalInterface
    private interface ThrowingAuthentication {
        void authenticate() throws Exception;
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final List<StoredUserAccount> accounts = new ArrayList<>();
        private SQLException failure;
        private SQLException createFailure;
        private int lookupCount;

        @Override
        public Optional<StoredUserAccount> findByUsernameOrEmail(String identifier) throws SQLException {
            lookupCount++;
            if (failure != null) {
                throw failure;
            }
            return accounts.stream()
                    .filter(account -> account.getUsername().equalsIgnoreCase(identifier)
                            || (account.getEmail() != null && account.getEmail().equalsIgnoreCase(identifier)))
                    .findFirst();
        }

        @Override
        public StoredUserAccount create(StoredUserAccount account) throws SQLException {
            if (failure != null) {
                throw failure;
            }
            if (createFailure != null) {
                throw createFailure;
            }
            StoredUserAccount saved = account.withId(100 + accounts.size());
            accounts.add(saved);
            return saved;
        }
    }
}
