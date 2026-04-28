package com.marketplace.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MyConnection — Singleton design pattern.
 *
 * Rules:
 *  1. Private constructor (prevents direct instantiation)
 *  2. Static container initialized to null
 *  3. Static getter: creates instance only if null, otherwise reuses it
 */
public class MyConnection {

    // ── Singleton container ─────────────────────────────────────
    private static MyConnection instance = null;

    // ── JDBC connection held inside the singleton ──────────────
    private Connection connection;

    // ── Connection parameters ───────────────────────────────────
    private static final String URL      = "jdbc:mysql://localhost:3306/marketplace";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    // ── Private constructor (Singleton rule #1) ─────────────────
    private MyConnection() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✓ Connexion à la base de données réussie.");
        } catch (SQLException e) {
            System.err.println("✗ Échec de la connexion : " + e.getMessage());
        }
    }

    // ── Static getter (Singleton rule #3) ──────────────────────
    public static MyConnection getInstance() {
        if (instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    // ── Accessor for the JDBC Connection ───────────────────────
    public Connection getConnection() {
        return connection;
    }
}
