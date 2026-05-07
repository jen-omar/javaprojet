package tn.esprit.services;

import tn.esprit.Models.Message;
import tn.esprit.interfaces.GlobalInterface;
import tn.esprit.util.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO implements GlobalInterface<Message> {
    private final Connection cnx;

    public MessageDAO() {
        this.cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public void add(Message message) {
        String req = "INSERT INTO message (sender_id, brief_id, content, audio_path, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, message.getSenderId());
            ps.setInt(2, message.getBriefId());
            ps.setString(3, message.getContent());
            ps.setString(4, message.getAudioPath());
            ps.setTimestamp(5, Timestamp.valueOf(message.getCreatedAt()));
            ps.executeUpdate();
            System.out.println("✅ Message Sent");
        } catch (SQLException e) {
            System.err.println("❌ Error sending message: " + e.getMessage());
        }
    }

    @Override
    public void update(Message message) {
        String req = "UPDATE message SET content = ?, audio_path = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, message.getContent());
            ps.setString(2, message.getAudioPath());
            ps.setInt(3, message.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error updating message: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String req = "DELETE FROM message WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Error deleting message: " + e.getMessage());
        }
    }

    @Override
    public List<Message> getAll() {
        return getByBriefId(-1); // Not really useful to get all messages across all chats
    }

    public List<Message> getByBriefId(int briefId) {
        List<Message> messages = new ArrayList<>();
        String req = "SELECT m.*, u.username as sender_name FROM message m " +
                     "JOIN user u ON m.sender_id = u.id " +
                     "WHERE m.brief_id = ? ORDER BY m.created_at ASC";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, briefId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message(
                        rs.getInt("id"),
                        rs.getInt("sender_id"),
                        rs.getInt("brief_id"),
                        rs.getString("content"),
                        rs.getString("audio_path"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    m.setSenderUsername(rs.getString("sender_name"));
                    messages.add(m);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting messages for brief: " + e.getMessage());
        }
        return messages;
    }
}
