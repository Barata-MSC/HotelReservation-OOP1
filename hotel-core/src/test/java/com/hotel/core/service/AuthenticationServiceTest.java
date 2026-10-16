package com.hotel.core.service;

import com.hotel.core.exception.InvalidCredentialsException;
import com.hotel.core.model.StoredStaffAccount;
import com.hotel.core.model.StaffUser;
import com.hotel.core.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticationServiceTest {

    private static final String PASSWORD = "receptionist secure password";
    private final PasswordHasher passwordHasher = new PasswordHasher(10_000);

    @Test
    void authenticatesEnabledStaffAndReturnsOnlyTheirProfile() throws Exception {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        repository.account = account(true, PASSWORD);

        StaffUser user = service(repository).authenticate(" frontdesk ", PASSWORD.toCharArray());

        assertEquals(17, user.getId());
        assertEquals("frontdesk", user.getUsername());
        assertEquals("Front Desk", user.getFullName());
        assertEquals("Receptionist", user.getRole());
    }

    @Test
    void givesTheSameGenericErrorForMissingWrongAndDisabledAccounts() {
        AuthenticationService missingAccountService = service(new InMemoryUserRepository());
        assertInvalidCredentials(
                () -> missingAccountService.authenticate("unknown", PASSWORD.toCharArray()));

        InMemoryUserRepository wrongPasswordRepository = new InMemoryUserRepository();
        wrongPasswordRepository.account = account(true, PASSWORD);
        assertInvalidCredentials(
                () -> service(wrongPasswordRepository).authenticate("frontdesk", "wrong password".toCharArray()));

        InMemoryUserRepository disabledRepository = new InMemoryUserRepository();
        disabledRepository.account = account(false, PASSWORD);
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

    private StoredStaffAccount account(boolean enabled, String password) {
        return new StoredStaffAccount(
                17, "frontdesk", passwordHasher.hash(password.toCharArray()),
                "Front Desk", "Receptionist", enabled);
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

        private StoredStaffAccount account;
        private SQLException failure;
        private int lookupCount;

        @Override
        public Optional<StoredStaffAccount> findByUsername(String username) throws SQLException {
            lookupCount++;
            if (failure != null) {
                throw failure;
            }
            if (account == null || !account.getUsername().equals(username)) {
                return Optional.empty();
            }
            return Optional.of(account);
        }

        @Override
        public void create(StoredStaffAccount newAccount) {
            account = newAccount;
        }
    }
}
