package tn.esprit.mythoria.utils;

import tn.esprit.util.DatabaseBootstrap;
import tn.esprit.util.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private Connection connection;
    private static MyDatabase myDatabase;

    private MyDatabase() {
        try {
            connection = openConnection();
            DatabaseBootstrap.ensureApplicationSchema(connection);
            DatabaseBootstrap.ensureEventSchema(connection);
            System.out.println("Connexion etablie avec succes");
        } catch (SQLException e) {
            System.out.println("Erreur de connexion " + e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (myDatabase == null) {
            myDatabase = new MyDatabase();
        } else {
            System.out.println("Connexion deja etablie");
        }
        return myDatabase;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = openConnection();
            }
            DatabaseBootstrap.ensureApplicationSchema(connection);
            DatabaseBootstrap.ensureEventSchema(connection);
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database: " + DatabaseConfig.url(), e);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.url(), DatabaseConfig.user(), DatabaseConfig.password());
    }
}
