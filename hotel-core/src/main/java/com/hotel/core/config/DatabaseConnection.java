package com.hotel.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {

    private static final String CONFIG_RESOURCE = "/db.properties";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Properties properties = new Properties();

        try (InputStream config = DatabaseConnection.class.getResourceAsStream(CONFIG_RESOURCE)) {
            if (config == null) {
                throw new SQLException("Missing classpath configuration: " + CONFIG_RESOURCE);
            }
            properties.load(config);
        } catch (IOException e) {
            throw new SQLException("Unable to load database configuration", e);
        }

        String url = requiredProperty(properties, "url");
        String user = requiredProperty(properties, "user");
        String password = requiredProperty(properties, "password");
        return DriverManager.getConnection(url, user, password);
    }

    private static String requiredProperty(Properties properties, String name) throws SQLException {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new SQLException("Missing required database property: " + name);
        }
        return value;
    }
}
