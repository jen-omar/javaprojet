package com.marketplace.util;

import tn.esprit.util.DatabaseBootstrap;
import tn.esprit.util.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;

    private Connection connection;

    private MyConnection() {
        try {
            connection = openConnection();
            DatabaseBootstrap.ensureApplicationSchema(connection);
            DatabaseBootstrap.ensureMarketplaceSchema(connection);
            System.out.println("Marketplace database connection established: " + DatabaseConfig.url());
        } catch (SQLException e) {
            System.err.println("Marketplace database connection failed: " + e.getMessage());
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
            if (connection == null || connection.isClosed()) {
                connection = openConnection();
            }
            DatabaseBootstrap.ensureApplicationSchema(connection);
            DatabaseBootstrap.ensureMarketplaceSchema(connection);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database: " + DatabaseConfig.url(), e);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.url(), DatabaseConfig.user(), DatabaseConfig.password());
    }
}
