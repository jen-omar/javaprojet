package com.marketplace.services;

import com.marketplace.interfaces.IService;
import com.marketplace.models.Bid;
import com.marketplace.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BidService — implements IService<Bid>.
 * All SQL for the `bid` table lives here.
 */
public class BidService implements IService<Bid> {

    private final Connection conn;

    public BidService() {
        conn = MyConnection.getInstance().getConnection();
    }

    // ── INSERT ────────────────────────────────────────────────────
    @Override
    public void add(Bid b) {
        String sql = "INSERT INTO bid (bidder_name, amount, created_at, product_id) VALUES (?, ?, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getBidderName());
            ps.setBigDecimal(2, b.getAmount());
            ps.setInt(3, b.getProductId());
            ps.executeUpdate();
            System.out.println("✓ Enchère ajoutée.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────
    @Override
    public void update(Bid b) {
        String sql = "UPDATE bid SET bidder_name=?, amount=?, product_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getBidderName());
            ps.setBigDecimal(2, b.getAmount());
            ps.setInt(3, b.getProductId());
            ps.setInt(4, b.getId());
            ps.executeUpdate();
            System.out.println("✓ Enchère mise à jour : " + b.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM bid WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✓ Enchère supprimée : " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────────
    @Override
    public List<Bid> getAll() {
        List<Bid> list = new ArrayList<>();
        String sql = "SELECT * FROM bid ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Bid b = new Bid();
                b.setId(rs.getInt("id"));
                b.setBidderName(rs.getString("bidder_name"));
                b.setAmount(rs.getBigDecimal("amount"));
                b.setProductId(rs.getInt("product_id"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null)
                    b.setCreatedAt(ts.toLocalDateTime());
                list.add(b);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
