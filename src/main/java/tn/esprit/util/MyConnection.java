package tn.esprit.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;

    private MyConnection() {
        try (Connection test = openConnection()) {
            DatabaseBootstrap.ensureApplicationSchema(test);
            System.out.println("Database connection established successfully: " + DatabaseConfig.url());
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            Connection connection = openConnection();
            DatabaseBootstrap.ensureApplicationSchema(connection);
            return connection;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Could not connect to database: " + DatabaseConfig.url(), e);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.url(), DatabaseConfig.user(), DatabaseConfig.password());
    }
}
