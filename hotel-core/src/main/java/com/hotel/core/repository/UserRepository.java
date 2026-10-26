package com.hotel.core.repository;

import com.hotel.core.model.StoredUserAccount;

import java.sql.SQLException;
import java.util.Optional;

public interface UserRepository {

    Optional<StoredUserAccount> findByUsernameOrEmail(String identifier) throws SQLException;

    StoredUserAccount create(StoredUserAccount account) throws SQLException;
}
