package com.hotel.core.repository;

import com.hotel.core.model.StoredStaffAccount;

import java.sql.SQLException;
import java.util.Optional;

public interface UserRepository {

    Optional<StoredStaffAccount> findByUsername(String username) throws SQLException;

    void create(StoredStaffAccount account) throws SQLException;
}
