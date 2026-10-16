package com.hotel.core.setup;

import com.hotel.core.exception.ValidationException;
import com.hotel.core.model.StoredStaffAccount;
import com.hotel.core.repository.UserRepository;
import com.hotel.core.service.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffAccountSetupTest {

    @Test
    void createsEnabledReceptionistUsingAHashedPassword() throws Exception {
        CapturingUserRepository repository = new CapturingUserRepository();
        PasswordHasher passwordHasher = new PasswordHasher();
        StaffAccountSetup setup = new StaffAccountSetup(repository, passwordHasher);
        char[] password = "a secure receptionist password".toCharArray();

        setup.createReceptionist(" frontdesk ", " Front Desk ", password);

        assertEquals("frontdesk", repository.createdAccount.getUsername());
        assertEquals("Front Desk", repository.createdAccount.getFullName());
        assertEquals("Receptionist", repository.createdAccount.getRole());
        assertTrue(repository.createdAccount.isEnabled());
        assertFalse(repository.createdAccount.getPasswordHash().contains(new String(password)));
        assertTrue(passwordHasher.verify(password, repository.createdAccount.getPasswordHash()));
    }

    @Test
    void rejectsInvalidAccountProfileBeforeWriting() {
        CapturingUserRepository repository = new CapturingUserRepository();
        StaffAccountSetup setup = new StaffAccountSetup(repository, new PasswordHasher());

        assertThrows(ValidationException.class,
                () -> setup.createReceptionist(" ", "Front Desk", "a secure receptionist password".toCharArray()));
        assertNull(repository.createdAccount);
    }

    private static final class CapturingUserRepository implements UserRepository {

        private StoredStaffAccount createdAccount;

        @Override
        public Optional<StoredStaffAccount> findByUsername(String username) {
            return Optional.empty();
        }

        @Override
        public void create(StoredStaffAccount account) throws SQLException {
            createdAccount = account;
        }
    }
}
