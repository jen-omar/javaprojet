package tn.esprit.services;

import tn.esprit.Models.World;
import tn.esprit.interfaces.GlobalInterface;
import tn.esprit.util.MyConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JDBC-based service implementation for the {@link World} entity.
 * Implements {@link GlobalInterface} following the Esprit service pattern.
 * <p>
 * Expects a MySQL table:
 * <pre>
 * CREATE TABLE world (
 *     id              INT AUTO_INCREMENT PRIMARY KEY,
 *     uuid            VARCHAR(36)  NOT NULL,
 *     title           VARCHAR(255) NOT NULL,
 *     description     TEXT,
 *     lore_snapshot   TEXT,
 *     created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
 * );
 * </pre>
 */
public class WorldService implements GlobalInterface<World> {

    private final Connection cnx;

    public WorldService() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(World world) {
        String sql = "INSERT INTO world (uuid, title, description, lore_snapshot, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, world.id().toString());
            ps.setString(2, world.title());
            ps.setString(3, world.description());
            ps.setString(4, world.loreSnapshot());
            ps.setTimestamp(5, Timestamp.from(world.createdAt() != null ? world.createdAt() : Instant.now()));
            ps.executeUpdate();
            System.out.println("✅ World added: " + world.title());
        } catch (SQLException e) {
            System.err.println("❌ Failed to add world: " + e.getMessage());
        }
    }

    @Override
    public void update(World world) {
        String sql = "UPDATE world SET title = ?, description = ?, lore_snapshot = ? WHERE uuid = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, world.title());
            ps.setString(2, world.description());
            ps.setString(3, world.loreSnapshot());
            ps.setString(4, world.id().toString());
            ps.executeUpdate();
            System.out.println("✅ World updated: " + world.title());
        } catch (SQLException e) {
            System.err.println("❌ Failed to update world: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM world WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ World deleted (id=" + id + ")");
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete world: " + e.getMessage());
        }
    }

    @Override
    public List<World> getAll() {
        List<World> worlds = new ArrayList<>();
        String sql = "SELECT * FROM world";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                World w = new World(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("lore_snapshot"),
                        rs.getTimestamp("created_at").toInstant(),
                        new ArrayList<>()
                );
                worlds.add(w);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch worlds: " + e.getMessage());
        }
        return worlds;
    }
}
