package tn.esprit.services;

import tn.esprit.Models.Proposal;
import tn.esprit.interfaces.GlobalInterface;
import tn.esprit.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProposalService implements GlobalInterface<Proposal> {
    private final Connection cnx;

    public ProposalService() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Proposal prop) {
        String req = "INSERT INTO proposal (price, days_to_complete, cover_letter, submitted_at, is_accepted, artist_id, brief_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setDouble(1, prop.getPrice());
            ps.setInt(2, prop.getDaysToComplete());
            ps.setString(3, prop.getCoverLetter());
            ps.setTimestamp(4, Timestamp.valueOf(prop.getSubmittedAt()));
            ps.setBoolean(5, prop.isAccepted());
            ps.setInt(6, prop.getArtistId());
            ps.setInt(7, prop.getBriefId());
            ps.executeUpdate();
            System.out.println("✅ Proposal Added");
        } catch (SQLException e) {
            System.err.println("❌ Error adding proposal: " + e.getMessage());
        }
    }

    @Override
    public void update(Proposal prop) {
        String req = "UPDATE proposal SET price = ?, days_to_complete = ?, cover_letter = ?, submitted_at = ?, is_accepted = ?, artist_id = ?, brief_id = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setDouble(1, prop.getPrice());
            ps.setInt(2, prop.getDaysToComplete());
            ps.setString(3, prop.getCoverLetter());
            ps.setTimestamp(4, Timestamp.valueOf(prop.getSubmittedAt()));
            ps.setBoolean(5, prop.isAccepted());
            ps.setInt(6, prop.getArtistId());
            ps.setInt(7, prop.getBriefId());
            ps.setInt(8, prop.getId());
            ps.executeUpdate();
            System.out.println("✅ Proposal Updated");
        } catch (SQLException e) {
            System.err.println("❌ Error updating proposal: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String req = "DELETE FROM proposal WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Proposal Deleted");
        } catch (SQLException e) {
            System.err.println("❌ Error deleting proposal: " + e.getMessage());
        }
    }

    @Override
    public List<Proposal> getAll() {
        List<Proposal> proposals = new ArrayList<>();
        String req = "SELECT p.*, u.username as artist_name FROM proposal p JOIN user u ON p.artist_id = u.id";
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Proposal p = new Proposal(
                        rs.getInt("id"),
                        rs.getDouble("price"),
                        rs.getInt("days_to_complete"),
                        rs.getString("cover_letter"),
                        rs.getTimestamp("submitted_at").toLocalDateTime(),
                        rs.getBoolean("is_accepted"),
                        rs.getInt("artist_id"),
                        rs.getInt("brief_id")
                );
                p.setArtistUsername(rs.getString("artist_name"));
                proposals.add(p);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting proposals: " + e.getMessage());
        }
        return proposals;
    }

    public List<Proposal> getByBriefId(int briefId) {
        List<Proposal> proposals = new ArrayList<>();
        String req = "SELECT p.*, u.username as artist_name FROM proposal p JOIN user u ON p.artist_id = u.id WHERE p.brief_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, briefId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Proposal p = new Proposal(
                            rs.getInt("id"),
                            rs.getDouble("price"),
                            rs.getInt("days_to_complete"),
                            rs.getString("cover_letter"),
                            rs.getTimestamp("submitted_at").toLocalDateTime(),
                            rs.getBoolean("is_accepted"),
                            rs.getInt("artist_id"),
                            rs.getInt("brief_id")
                    );
                    p.setArtistUsername(rs.getString("artist_name"));
                    proposals.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting proposals by brief: " + e.getMessage());
        }
        return proposals;
    }

    public List<Proposal> getByArtistId(int artistId) {
        List<Proposal> proposals = new ArrayList<>();
        String req = "SELECT p.*, u.username as artist_name FROM proposal p JOIN user u ON p.artist_id = u.id WHERE p.artist_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, artistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Proposal p = new Proposal(
                            rs.getInt("id"),
                            rs.getDouble("price"),
                            rs.getInt("days_to_complete"),
                            rs.getString("cover_letter"),
                            rs.getTimestamp("submitted_at").toLocalDateTime(),
                            rs.getBoolean("is_accepted"),
                            rs.getInt("artist_id"),
                            rs.getInt("brief_id")
                    );
                    p.setArtistUsername(rs.getString("artist_name"));
                    proposals.add(p);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting proposals by artist: " + e.getMessage());
        }
        return proposals;
    }
}
