package tn.esprit.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class managing the JDBC connection to the MySQL database.
 * <p>
 * Usage:
 * <pre>
 *     Connection cnx = MyConnection.getInstance().getConnection();
 * </pre>
 */
public class MyConnection {

    private static MyConnection instance;

    private final String url = "jdbc:mysql://localhost:3306/mythoria_db";
    private final String user = "root";
    private final String password = "";

    private Connection connection;

    private MyConnection() {
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Database connection established successfully.");
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            throw new RuntimeException("Could not connect to database", e);
        }
    }

    /**
     * Returns the single instance of MyConnection.
     * Creates the instance on first call (lazy initialization).
     */
    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    /**
     * Returns the active JDBC connection.
     */
    public Connection getConnection() {
        return connection;
    }
}
