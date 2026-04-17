package com.example.mythoriadesktop.data;

import com.example.mythoriadesktop.ValidationUtils;
import com.example.mythoriadesktop.model.Wallet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WalletRepository {
    private static final Logger LOG = Logger.getLogger(WalletRepository.class.getName());

    private final DatabaseConnection databaseConnection;

    public WalletRepository() {
        this(DatabaseConfig.fromEnvironment());
    }

    public WalletRepository(DatabaseConfig databaseConfig) {
        this.databaseConnection = new DatabaseConnection(databaseConfig);
    }

    public List<Wallet> findByUserId(int userId) {
        String sql = """
                SELECT id, updated_at, user_id, solde, statut, devise, plafond
                FROM portefeuille
                WHERE user_id = ?
                ORDER BY updated_at DESC, id DESC
                """;

        List<Wallet> wallets = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);

            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    wallets.add(mapWallet(rs));
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to load wallets for user " + userId);
        }
        return wallets;
    }

    public List<Wallet> findAll() {
        String sql = """
                SELECT id, updated_at, user_id, solde, statut, devise, plafond
                FROM portefeuille
                ORDER BY updated_at DESC, id DESC
                """;

        List<Wallet> wallets = new ArrayList<>();
        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                wallets.add(mapWallet(rs));
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to load all wallets");
        }
        return wallets;
    }

    public Optional<Wallet> create(int userId, double balance, String status, String currency, double ceiling) {
        ValidationUtils.validateWalletAmounts(balance, ceiling);
        String sql = """
                INSERT INTO portefeuille (user_id, solde, statut, devise, plafond, updated_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        String validatedStatus = ValidationUtils.requireStatus(status);
        String validatedCurrency = ValidationUtils.requireCurrency(currency);

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setDouble(2, balance);
            statement.setString(3, validatedStatus);
            statement.setString(4, validatedCurrency);
            statement.setDouble(5, ceiling);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1));
                }
            }
            return findLatestForUser(userId, balance, status, currency, ceiling);
        } catch (SQLException ex) {
            if (requiresExplicitId(ex)) {
                return createWithExplicitId(userId, balance, status, currency, ceiling);
            }
            LOG.log(Level.WARNING, ex, () -> "Failed to create wallet for user " + userId);
            throw new IllegalStateException("Database error while creating wallet: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to create wallet for user " + userId);
            throw new IllegalStateException("Unexpected error while creating wallet: " + ex.getMessage(), ex);
        }
    }

    public Optional<Wallet> update(int walletId, int userId, double balance, String status, String currency, double ceiling) {
        ValidationUtils.validateWalletAmounts(balance, ceiling);
        String sql = """
                UPDATE portefeuille
                SET user_id = ?, solde = ?, statut = ?, devise = ?, plafond = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        String validatedStatus = ValidationUtils.requireStatus(status);
        String validatedCurrency = ValidationUtils.requireCurrency(currency);

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setDouble(2, balance);
            statement.setString(3, validatedStatus);
            statement.setString(4, validatedCurrency);
            statement.setDouble(5, ceiling);
            statement.setInt(6, walletId);
            int updatedRows = statement.executeUpdate();
            if (updatedRows > 0) {
                return findById(walletId);
            }
            return Optional.empty();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to update wallet " + walletId);
            throw new IllegalStateException("Database error while updating wallet: " + ex.getMessage(), ex);
        }
    }

    public boolean delete(int walletId) {
        String sql = "DELETE FROM portefeuille WHERE id = ?";

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, walletId);
            return statement.executeUpdate() > 0;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to delete wallet " + walletId);
            return false;
        }
    }

    public Optional<Wallet> findById(int walletId) {
        String sql = """
                SELECT id, updated_at, user_id, solde, statut, devise, plafond
                FROM portefeuille
                WHERE id = ?
                LIMIT 1
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, walletId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapWallet(rs));
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to load wallet " + walletId);
        }

        return Optional.empty();
    }

    private Wallet mapWallet(ResultSet rs) throws java.sql.SQLException {
        return new Wallet(
                rs.getInt("id"),
                Optional.ofNullable(rs.getTimestamp("updated_at"))
                        .map(timestamp -> timestamp.toLocalDateTime().toString().replace('T', ' '))
                        .orElse(""),
                rs.getInt("user_id"),
                rs.getDouble("solde"),
                Optional.ofNullable(rs.getString("statut")).orElse(""),
                Optional.ofNullable(rs.getString("devise")).orElse(""),
                rs.getDouble("plafond")
        );
    }

    private static String normalizeText(String value) {
        return Optional.ofNullable(value).orElse("").trim();
    }

    private Optional<Wallet> createWithExplicitId(int userId, double balance, String status, String currency, double ceiling) {
        String sql = """
                INSERT INTO portefeuille (id, user_id, solde, statut, devise, plafond, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;

        String validatedStatus = ValidationUtils.requireStatus(status);
        String validatedCurrency = ValidationUtils.requireCurrency(currency);

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextId = nextWalletId(connection);
            statement.setInt(1, nextId);
            statement.setInt(2, userId);
            statement.setDouble(3, balance);
            statement.setString(4, validatedStatus);
            statement.setString(5, validatedCurrency);
            statement.setDouble(6, ceiling);
            statement.executeUpdate();
            return findById(nextId);
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to create wallet with explicit id for user " + userId);
            throw new IllegalStateException("Wallet insert failed: " + ex.getMessage(), ex);
        }
    }

    private Optional<Wallet> findLatestForUser(int userId, double balance, String status, String currency, double ceiling) {
        String sql = """
                SELECT id, updated_at, user_id, solde, statut, devise, plafond
                FROM portefeuille
                WHERE user_id = ?
                ORDER BY updated_at DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection = databaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapWallet(rs));
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            LOG.log(Level.WARNING, ex, () -> "Failed to reload latest wallet for user " + userId);
            return Optional.empty();
        }
    }

    private int nextWalletId(Connection connection) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM portefeuille";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("next_id");
            }
            return 1;
        }
    }

    private boolean requiresExplicitId(SQLException ex) {
        String message = Optional.ofNullable(ex.getMessage()).orElse("").toLowerCase();
        return message.contains("field 'id' doesn't have a default value")
                || message.contains("field 'id' doesn't have default value");
    }
}
