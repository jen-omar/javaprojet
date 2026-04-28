package com.marketplace.services;

import com.marketplace.interfaces.IService;
import com.marketplace.models.Wishlist;
import com.marketplace.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * WishlistService — implements IService<Wishlist>.
 * All SQL for the `wishlist` table lives here.
 */
public class WishlistService implements IService<Wishlist> {

    private final Connection conn;

    public WishlistService() {
        conn = MyConnection.getInstance().getConnection();
    }

    // ── INSERT ────────────────────────────────────────────────────
    @Override
    public void add(Wishlist w) {
        String sql = "INSERT INTO wishlist (client_name, created_at, product_id) VALUES (?, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, w.getClientName());
            ps.setInt(2, w.getProductId());
            ps.executeUpdate();
            System.out.println("✓ Wishlist ajouté.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────
    @Override
    public void update(Wishlist w) {
        String sql = "UPDATE wishlist SET client_name=?, product_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, w.getClientName());
            ps.setInt(2, w.getProductId());
            ps.setInt(3, w.getId());
            ps.executeUpdate();
            System.out.println("✓ Wishlist mis à jour : " + w.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM wishlist WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✓ Wishlist supprimé : " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────────
    @Override
    public List<Wishlist> getAll() {
        List<Wishlist> list = new ArrayList<>();
        String sql = "SELECT * FROM wishlist ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Wishlist w = new Wishlist();
                w.setId(rs.getInt("id"));
                w.setClientName(rs.getString("client_name"));
                w.setProductId(rs.getInt("product_id"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null)
                    w.setCreatedAt(ts.toLocalDateTime());
                list.add(w);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── CHECK DUPLICATE ───────────────────────────────────────────
    /** Retourne true si ce client a déjà ce produit dans sa wishlist. */
    public boolean isAlreadyInWishlist(String clientName, int productId) {
        String sql = "SELECT COUNT(*) FROM wishlist WHERE client_name=? AND product_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clientName);
            ps.setInt(2, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
