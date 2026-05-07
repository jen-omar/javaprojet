package tn.esprit.services;

import tn.esprit.Models.Brief;
import tn.esprit.interfaces.GlobalInterface;
import tn.esprit.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BriefService implements GlobalInterface<Brief> {
    private final Connection cnx;

    public BriefService() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Brief brief) {
        String req = "INSERT INTO brief (title, description, budget_max, deadline, status, client_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, brief.getTitle());
            ps.setString(2, brief.getDescription());
            ps.setDouble(3, brief.getBudgetMax());
            ps.setTimestamp(4, Timestamp.valueOf(brief.getDeadline()));
            ps.setString(5, brief.getStatus());
            ps.setInt(6, brief.getClientId());
            ps.setTimestamp(7, Timestamp.valueOf(brief.getCreatedAt()));
            ps.executeUpdate();
            System.out.println("✅ Brief Added");
        } catch (SQLException e) {
            System.err.println("❌ Error adding brief: " + e.getMessage());
        }
    }

    @Override
    public void update(Brief brief) {
        String req = "UPDATE brief SET title = ?, description = ?, budget_max = ?, deadline = ?, status = ?, client_id = ?, created_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, brief.getTitle());
            ps.setString(2, brief.getDescription());
            ps.setDouble(3, brief.getBudgetMax());
            ps.setTimestamp(4, Timestamp.valueOf(brief.getDeadline()));
            ps.setString(5, brief.getStatus());
            ps.setInt(6, brief.getClientId());
            ps.setTimestamp(7, Timestamp.valueOf(brief.getCreatedAt()));
            ps.setInt(8, brief.getId());
            ps.executeUpdate();
            System.out.println("✅ Brief Updated");
        } catch (SQLException e) {
            System.err.println("❌ Error updating brief: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String req = "DELETE FROM brief WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Brief Deleted");
        } catch (SQLException e) {
            System.err.println("❌ Error deleting brief: " + e.getMessage());
        }
    }

    @Override
    public List<Brief> getAll() {
        List<Brief> briefs = new ArrayList<>();
        String req = "SELECT b.*, u.username as client_name FROM brief b JOIN user u ON b.client_id = u.id";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Brief b = new Brief(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getDouble("budget_max"),
                        rs.getTimestamp("deadline").toLocalDateTime(),
                        rs.getString("status"),
                        rs.getInt("client_id"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                b.setClientUsername(rs.getString("client_name"));
                briefs.add(b);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting briefs: " + e.getMessage());
        }
        return briefs;
    }

    public List<Brief> getByStatus(String status) {
        List<Brief> briefs = new ArrayList<>();
        String req = "SELECT b.*, u.username as client_name FROM brief b JOIN user u ON b.client_id = u.id WHERE b.status = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Brief b = new Brief(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getDouble("budget_max"),
                            rs.getTimestamp("deadline").toLocalDateTime(),
                            rs.getString("status"),
                            rs.getInt("client_id"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    b.setClientUsername(rs.getString("client_name"));
                    briefs.add(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting open briefs: " + e.getMessage());
        }
        return briefs;
    }

    public List<Brief> getByClient(int clientId) {
        List<Brief> briefs = new ArrayList<>();
        String req = "SELECT b.*, u.username as client_name FROM brief b JOIN user u ON b.client_id = u.id WHERE b.client_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Brief b = new Brief(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getDouble("budget_max"),
                            rs.getTimestamp("deadline").toLocalDateTime(),
                            rs.getString("status"),
                            rs.getInt("client_id"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    b.setClientUsername(rs.getString("client_name"));
                    briefs.add(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting client briefs: " + e.getMessage());
        }
        return briefs;
    }

    /**
     * Accepts a proposal in a single database transaction. 
     * brief.status becomes 'IN_PROGRESS' AND proposal.is_accepted becomes 1.
     */
    public boolean acceptProposal(int paramBriefId, int propId) {
        String updateProposal = "UPDATE proposal SET is_accepted = 1 WHERE id = ?";
        String rejectOthers = "UPDATE proposal SET is_accepted = 0 WHERE brief_id = ? AND id != ?";
        String updateBrief = "UPDATE brief SET status = 'IN_PROGRESS' WHERE id = ?";
        
        try {
            cnx.setAutoCommit(false); // start transaction
            
            try (PreparedStatement ps1 = cnx.prepareStatement(updateProposal)) {
                ps1.setInt(1, propId);
                ps1.executeUpdate();
            }
            
            try (PreparedStatement psReject = cnx.prepareStatement(rejectOthers)) {
                psReject.setInt(1, paramBriefId);
                psReject.setInt(2, propId);
                psReject.executeUpdate();
            }
            
            try (PreparedStatement ps2 = cnx.prepareStatement(updateBrief)) {
                ps2.setInt(1, paramBriefId);
                ps2.executeUpdate();
            }
            
            cnx.commit(); // end transaction
            System.out.println("✅ Proposal accepted successfully using transaction.");
            return true;
        } catch (SQLException e) {
            try { cnx.rollback(); } catch (SQLException rollbackEx) { }
            System.err.println("❌ Transaction failed, rolling back. " + e.getMessage());
            return false;
        } finally {
            try { cnx.setAutoCommit(true); } catch (SQLException ex) {}
        }
    }
}
