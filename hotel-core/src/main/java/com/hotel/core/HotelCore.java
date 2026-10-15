/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.hotel.core;

import com.hotel.core.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author Admin
 */
public class HotelCore {

    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            System.out.println("Connected");
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
        }
    }
}
