package tn.esprit.services;

import tn.esprit.Models.User;
import tn.esprit.interfaces.GlobalInterface;
import tn.esprit.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO implements GlobalInterface<User> {
    private final Connection cnx;

    public UserDAO() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(User user) {
        String req = "INSERT INTO user (email, username, password, is_bot_enabled, roles) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setBoolean(4, user.isBotEnabled());
            ps.setString(5, user.getRoles());
            ps.executeUpdate();
            System.out.println("✅ User Added");
        } catch (SQLException e) {
            System.err.println("❌ Error adding user: " + e.getMessage());
        }
    }

    @Override
    public void update(User user) {
        String req = "UPDATE user SET email = ?, username = ?, password = ?, is_bot_enabled = ?, roles = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setBoolean(4, user.isBotEnabled());
            ps.setString(5, user.getRoles());
            ps.setInt(6, user.getId());
            ps.executeUpdate();
            System.out.println("✅ User Updated");
        } catch (SQLException e) {
            System.err.println("❌ Error updating user: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String req = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ User Deleted");
        } catch (SQLException e) {
            System.err.println("❌ Error deleting user: " + e.getMessage());
        }
    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String req = "SELECT * FROM user";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting all users: " + e.getMessage());
        }
        return users;
    }

    public User login(String identifier, String password) {
        String req = "SELECT * FROM user WHERE (email = ? OR username = ?) AND password = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ps.setString(3, password); // Note: Simple raw password matching. In real prod, use BCrypt
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("✅ Login Successful for " + identifier);
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error logging in: " + e.getMessage());
        }
        System.err.println("❌ Login Failed");
        return null;
    }

    public User checkEmailExists(String email) {
        String req = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error checking email: " + e.getMessage());
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("email"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getBoolean("is_bot_enabled"),
            rs.getString("roles")
        );
    }
}
