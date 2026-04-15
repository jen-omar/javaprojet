package com.marketplace.services;

import com.marketplace.interfaces.IService;
import com.marketplace.models.Review;
import com.marketplace.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReviewService — implements IService<Review>.
 * All SQL for the `review` table lives here.
 */
public class ReviewService implements IService<Review> {

    private final Connection conn;

    public ReviewService() {
        conn = MyConnection.getInstance().getConnection();
    }

    // ── INSERT ────────────────────────────────────────────────────
    @Override
    public void add(Review r) {
        String sql = "INSERT INTO review (reviewer_name, rating, comment, created_at, product_id) " +
                "VALUES (?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getReviewerName());
            ps.setInt(2, r.getRating());
            ps.setString(3, r.getComment());
            ps.setInt(4, r.getProductId());
            ps.executeUpdate();
            System.out.println("✓ Avis ajouté.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────
    @Override
    public void update(Review r) {
        String sql = "UPDATE review SET reviewer_name=?, rating=?, comment=?, product_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getReviewerName());
            ps.setInt(2, r.getRating());
            ps.setString(3, r.getComment());
            ps.setInt(4, r.getProductId());
            ps.setInt(5, r.getId());
            ps.executeUpdate();
            System.out.println("✓ Avis mis à jour : " + r.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM review WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✓ Avis supprimé : " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────────
    @Override
    public List<Review> getAll() {
        List<Review> list = new ArrayList<>();
        String sql = "SELECT * FROM review ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Review r = new Review();
                r.setId(rs.getInt("id"));
                r.setReviewerName(rs.getString("reviewer_name"));
                r.setRating(rs.getInt("rating"));
                r.setComment(rs.getString("comment"));
                r.setProductId(rs.getInt("product_id"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null)
                    r.setCreatedAt(ts.toLocalDateTime());
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
