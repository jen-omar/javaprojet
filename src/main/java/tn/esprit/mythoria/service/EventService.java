package tn.esprit.mythoria.service;

import tn.esprit.mythoria.entity.Event;
import tn.esprit.mythoria.utils.MyDatabase;

import java.util.List;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventService implements IService<Event> {

    private Connection connection;

    public EventService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Event event) throws SQLException {
        String sql = "INSERT INTO event (title, description, date, location, created_at, max_tickets, max_vip_tickets, max_normal_tickets, creator_id, local_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, event.getTitle());
        ps.setString(2, event.getDescription());
        ps.setTimestamp(3, Timestamp.valueOf(event.getDate()));
        ps.setString(4, event.getLocation());
        ps.setTimestamp(5, Timestamp.valueOf(event.getCreatedAt()));
        ps.setInt(6, event.getMaxTickets());
        ps.setInt(7, event.getMaxVipTickets());
        ps.setInt(8, event.getMaxNormalTickets());
        ps.setInt(9, event.getCreatorId());
        ps.setInt(10, event.getLocalId());

        ps.executeUpdate();
        System.out.println("Event ajouté avec succès.");
    }

    @Override
    public void modifier(Event event) throws SQLException {
        String sql = "UPDATE event SET title=?, description=?, date=?, location=?, created_at=?, max_tickets=?, max_vip_tickets=?, max_normal_tickets=?, creator_id=?, local_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, event.getTitle());
        ps.setString(2, event.getDescription());
        ps.setTimestamp(3, Timestamp.valueOf(event.getDate()));
        ps.setString(4, event.getLocation());
        ps.setTimestamp(5, Timestamp.valueOf(event.getCreatedAt()));
        ps.setInt(6, event.getMaxTickets());
        ps.setInt(7, event.getMaxVipTickets());
        ps.setInt(8, event.getMaxNormalTickets());
        ps.setInt(9, event.getCreatorId());
        ps.setInt(10, event.getLocalId());
        ps.setInt(11, event.getId());

        ps.executeUpdate();
        System.out.println("Event modifié avec succès.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM event WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("Event supprimé avec succès.");
    }

    @Override
    public List<Event> afficher() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Event event = new Event();
            event.setId(rs.getInt("id"));
            event.setTitle(rs.getString("title"));
            event.setDescription(rs.getString("description"));

            Timestamp dateTs = rs.getTimestamp("date");
            if (dateTs != null) {
                event.setDate(dateTs.toLocalDateTime());
            }

            event.setLocation(rs.getString("location"));

            Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                event.setCreatedAt(createdTs.toLocalDateTime());
            }

            event.setMaxTickets(rs.getInt("max_tickets"));
            event.setMaxVipTickets(rs.getInt("max_vip_tickets"));
            event.setMaxNormalTickets(rs.getInt("max_normal_tickets"));
            event.setCreatorId(rs.getInt("creator_id"));
            event.setLocalId(rs.getInt("local_id"));

            events.add(event);
        }

        return events;
    }

    @Override
    public Event getById(int id) throws SQLException {
        String sql = "SELECT * FROM event WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Event event = new Event();
            event.setId(rs.getInt("id"));
            event.setTitle(rs.getString("title"));
            event.setDescription(rs.getString("description"));

            Timestamp dateTs = rs.getTimestamp("date");
            if (dateTs != null) {
                event.setDate(dateTs.toLocalDateTime());
            }

            event.setLocation(rs.getString("location"));

            Timestamp createdTs = rs.getTimestamp("created_at");
            if (createdTs != null) {
                event.setCreatedAt(createdTs.toLocalDateTime());
            }

            event.setMaxTickets(rs.getInt("max_tickets"));
            event.setMaxVipTickets(rs.getInt("max_vip_tickets"));
            event.setMaxNormalTickets(rs.getInt("max_normal_tickets"));
            event.setCreatorId(rs.getInt("creator_id"));
            event.setLocalId(rs.getInt("local_id"));

            return event;
        }

        return null;
    }
    public boolean existeEventPourLocalEtDate(int localId, java.time.LocalDate date) throws SQLException {
        String sql = "SELECT * FROM event WHERE local_id = ? AND DATE(date) = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, localId);
        ps.setDate(2, java.sql.Date.valueOf(date));

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }
    public List<Event> afficherParLocalEtCreateur(int localId, int creatorId) throws SQLException {
        List<Event> events = new ArrayList<>();

        String sql = "SELECT * FROM event WHERE local_id = ? AND creator_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, localId);
        ps.setInt(2, creatorId);

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Event event = new Event();
            event.setId(rs.getInt("id"));
            event.setTitle(rs.getString("title"));
            event.setDescription(rs.getString("description"));
            event.setDate(rs.getTimestamp("date").toLocalDateTime());
            event.setLocation(rs.getString("location"));
            event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            event.setMaxTickets(rs.getInt("max_tickets"));
            event.setMaxVipTickets(rs.getInt("max_vip_tickets"));
            event.setMaxNormalTickets(rs.getInt("max_normal_tickets"));
            event.setCreatorId(rs.getInt("creator_id"));
            event.setLocalId(rs.getInt("local_id"));

            events.add(event);
        }

        return events;
    }
    public boolean existeAutreEventPourLocalEtDate(int localId, java.time.LocalDate date, int eventId) throws SQLException {
        String sql = "SELECT * FROM event WHERE local_id = ? AND DATE(date) = ? AND id <> ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, localId);
        ps.setDate(2, java.sql.Date.valueOf(date));
        ps.setInt(3, eventId);

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }
}