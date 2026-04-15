package com.marketplace.services;

import com.marketplace.interfaces.IService;
import com.marketplace.models.Product;
import com.marketplace.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProductService — implements IService<Product>.
 * All SQL for the `product` table lives here.
 */
public class ProductService implements IService<Product> {

    private final Connection conn;

    public ProductService() {
        conn = MyConnection.getInstance().getConnection();
    }

    // ── INSERT ────────────────────────────────────────────────────
    @Override
    public void add(Product p) {
        String sql = "INSERT INTO product (name, description, price, artist_name, created_at, " +
                "image_url, type, category, sale_type, status, min_bid_increment) " +
                "VALUES (?, ?, ?, ?, NOW(), ?, ?, ?, ?, ?, 0.01)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setBigDecimal(3, p.getPrice());
            ps.setString(4, p.getArtistName());
            ps.setString(5, p.getImageUrl());
            ps.setString(6, p.getType());
            ps.setString(7, p.getCategory());
            ps.setString(8, p.getSaleType());
            ps.setString(9, p.getStatus());
            ps.executeUpdate();
            System.out.println("✓ Produit ajouté : " + p.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────
    @Override
    public void update(Product p) {
        String sql = "UPDATE product SET name=?, description=?, price=?, artist_name=?, " +
                "image_url=?, type=?, category=?, sale_type=?, status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setBigDecimal(3, p.getPrice());
            ps.setString(4, p.getArtistName());
            ps.setString(5, p.getImageUrl());
            ps.setString(6, p.getType());
            ps.setString(7, p.getCategory());
            ps.setString(8, p.getSaleType());
            ps.setString(9, p.getStatus());
            ps.setInt(10, p.getId());
            ps.executeUpdate();
            System.out.println("✓ Produit mis à jour : " + p.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Marque un produit comme vendu (status = 'sold') sans charger tout l'objet. */
    public void markAsSold(int productId) {
        String sql = "UPDATE product SET status='sold' WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.executeUpdate();
            System.out.println("✓ Produit marqué vendu : " + productId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    /**
     * Deletes related bid/order/review/wishlist records first to respect FK
     * constraints.
     */
    @Override
    public void delete(int id) {
        String[] deps = {
                "DELETE FROM bid WHERE product_id=?",
                "DELETE FROM `order` WHERE product_id=?",
                "DELETE FROM review WHERE product_id=?",
                "DELETE FROM wishlist WHERE product_id=?"
        };
        try {
            for (String sql : deps) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                System.out.println("✓ Produit supprimé : " + id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────────
    @Override
    public List<Product> getAll() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM product ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Product p = new Product();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setPrice(rs.getBigDecimal("price"));
                p.setArtistName(rs.getString("artist_name"));
                p.setImageUrl(rs.getString("image_url"));
                p.setType(rs.getString("type"));
                p.setCategory(rs.getString("category"));
                p.setSaleType(rs.getString("sale_type"));
                p.setStatus(rs.getString("status"));
                p.setCurrentBid(rs.getBigDecimal("current_bid"));
                p.setCurrentBidder(rs.getString("current_bidder"));
                p.setBuyer(rs.getString("buyer"));
                Timestamp ts1 = rs.getTimestamp("created_at");
                if (ts1 != null)
                    p.setCreatedAt(ts1.toLocalDateTime());
                Timestamp ts2 = rs.getTimestamp("auction_end_time");
                if (ts2 != null)
                    p.setAuctionEndTime(ts2.toLocalDateTime());
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
