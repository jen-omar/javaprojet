package com.marketplace.services;

import com.marketplace.interfaces.IService;
import com.marketplace.models.Order;
import com.marketplace.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderService — implements IService<Order>.
 * All SQL for the `order` table lives here.
 * Note: `order` is a SQL reserved word — always use backtick quoting.
 */
public class OrderService implements IService<Order> {

    private final Connection conn;

    public OrderService() {
        conn = MyConnection.getInstance().getConnection();
    }

    // ── INSERT ────────────────────────────────────────────────────
    @Override
    public void add(Order o) {
        String sql = "INSERT INTO `order` (buyer_name, price, order_type, created_at, product_id) " +
                "VALUES (?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getBuyerName());
            ps.setBigDecimal(2, o.getPrice());
            ps.setString(3, o.getOrderType());
            ps.setInt(4, o.getProductId());
            ps.executeUpdate();
            System.out.println("✓ Commande ajoutée.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────
    @Override
    public void update(Order o) {
        String sql = "UPDATE `order` SET buyer_name=?, price=?, order_type=?, product_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, o.getBuyerName());
            ps.setBigDecimal(2, o.getPrice());
            ps.setString(3, o.getOrderType());
            ps.setInt(4, o.getProductId());
            ps.setInt(5, o.getId());
            ps.executeUpdate();
            System.out.println("✓ Commande mise à jour : " + o.getId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM `order` WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✓ Commande supprimée : " + id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ── SELECT ALL ────────────────────────────────────────────────
    @Override
    public List<Order> getAll() {
        List<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM `order` ORDER BY created_at DESC";
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Order o = new Order();
                o.setId(rs.getInt("id"));
                o.setBuyerName(rs.getString("buyer_name"));
                o.setPrice(rs.getBigDecimal("price"));
                o.setOrderType(rs.getString("order_type"));
                o.setProductId(rs.getInt("product_id"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null)
                    o.setCreatedAt(ts.toLocalDateTime());
                list.add(o);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
