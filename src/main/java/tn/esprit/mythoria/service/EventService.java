package tn.esprit.mythoria.service;

import tn.esprit.mythoria.entity.Event;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.utils.EmailUtil;
import tn.esprit.mythoria.utils.MyDatabase;

import jakarta.mail.MessagingException;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EventService implements IService<Event> {

    private Connection connection;

    public record EventCancellationResult(
            int recipientCount,
            int sentEmailCount,
            boolean emailConfigured,
            String emailError
    ) {
    }

    public EventService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Event event) throws SQLException {
        if (connection == null) {
            throw new SQLException("Connexion a la base de donnees indisponible.");
        }

        int nextId = event.getId() > 0 ? event.getId() : getNextId();
        String sql = "INSERT INTO event (id, title, description, image, date, location, created_at, max_tickets, max_vip_tickets, max_normal_tickets, creator_id, local_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nextId);
            ps.setString(2, event.getTitle());
            ps.setString(3, event.getDescription());
            ps.setString(4, event.getImage());
            ps.setTimestamp(5, Timestamp.valueOf(event.getDate()));
            ps.setString(6, event.getLocation());
            ps.setTimestamp(7, Timestamp.valueOf(event.getCreatedAt()));
            ps.setInt(8, event.getMaxTickets());
            ps.setInt(9, event.getMaxVipTickets());
            ps.setInt(10, event.getMaxNormalTickets());
            ps.setInt(11, event.getCreatorId());
            ps.setInt(12, event.getLocalId());
            ps.executeUpdate();
        }

        event.setId(nextId);
        System.out.println("Event ajoute avec succes.");
    }

    @Override
    public void modifier(Event event) throws SQLException {
        String sql = "UPDATE event SET title=?, description=?, image=?, date=?, location=?, created_at=?, max_tickets=?, max_vip_tickets=?, max_normal_tickets=?, creator_id=?, local_id=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, event.getTitle());
        ps.setString(2, event.getDescription());
        ps.setString(3, event.getImage());
        ps.setTimestamp(4, Timestamp.valueOf(event.getDate()));
        ps.setString(5, event.getLocation());
        ps.setTimestamp(6, Timestamp.valueOf(event.getCreatedAt()));
        ps.setInt(7, event.getMaxTickets());
        ps.setInt(8, event.getMaxVipTickets());
        ps.setInt(9, event.getMaxNormalTickets());
        ps.setInt(10, event.getCreatorId());
        ps.setInt(11, event.getLocalId());
        ps.setInt(12, event.getId());

        ps.executeUpdate();
        System.out.println("Event modifie avec succes.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        deleteEventAndTickets(id);
        System.out.println("Event supprime avec succes.");
    }

    public EventCancellationResult cancelEvent(Event event, Local local) throws SQLException {
        if (event == null) {
            throw new IllegalArgumentException("Event vide.");
        }

        List<String> recipients = getParticipantEmails(event.getId());
        deleteEventAndTickets(event.getId());

        boolean emailConfigured = EmailUtil.isConfigured();
        int sentEmailCount = 0;
        String emailError = null;

        if (emailConfigured && !recipients.isEmpty()) {
            try {
                sentEmailCount = EmailUtil.sendBulkEmail(
                        recipients,
                        "Annulation de l'event " + safeText(event.getTitle(), "Mythoria"),
                        buildCancellationEmail(event, local)
                );
            } catch (MessagingException | RuntimeException e) {
                emailError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            }
        }

        return new EventCancellationResult(recipients.size(), sentEmailCount, emailConfigured, emailError);
    }

    @Override
    public List<Event> afficher() throws SQLException {
        List<Event> events = new ArrayList<>();
        String sql = "SELECT * FROM event";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            events.add(mapEvent(rs));
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
            return mapEvent(rs);
        }

        return null;
    }

    public boolean existeEventPourLocalEtDate(int localId, java.time.LocalDate date) throws SQLException {
        String sql = "SELECT * FROM event WHERE local_id = ? AND DATE(date) = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, localId);
        ps.setDate(2, Date.valueOf(date));

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
            events.add(mapEvent(rs));
        }

        return events;
    }

    public boolean existeAutreEventPourLocalEtDate(int localId, java.time.LocalDate date, int eventId) throws SQLException {
        String sql = "SELECT * FROM event WHERE local_id = ? AND DATE(date) = ? AND id <> ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, localId);
        ps.setDate(2, Date.valueOf(date));
        ps.setInt(3, eventId);

        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    public List<Event> sortEventsByCritere(List<Event> events, String critere) {
        Comparator<Event> comparator;
        switch (critere) {
            case "Date":
                comparator = Comparator.comparing(Event::getDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "Title":
                comparator = Comparator.comparing(Event::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Description":
                comparator = Comparator.comparing(Event::getDescription, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Location":
                comparator = Comparator.comparing(Event::getLocation, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Ticket":
                comparator = Comparator.comparingInt(Event::getMaxTickets);
                break;
            default:
                comparator = Comparator.comparing(Event::getDate, Comparator.nullsLast(Comparator.naturalOrder()));
                break;
        }

        return events.stream().sorted(comparator).toList();
    }

    public List<Event> rechercherEtTrierParLocalEtCreateur(String critere, String searchText, int localId, int creatorId) throws SQLException {
        String query = searchText == null ? "" : searchText.toLowerCase(Locale.ROOT);
        List<Event> events = afficherParLocalEtCreateur(localId, creatorId).stream()
                .filter(event -> matchesSearch(event, query))
                .toList();
        return sortEventsByCritere(events, critere);
    }

    private boolean matchesSearch(Event event, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }

        String dateValue = event.getDate() == null ? "" : event.getDate().toString();
        String createdAtValue = event.getCreatedAt() == null ? "" : event.getCreatedAt().toString();
        return containsIgnoreCase(event.getTitle(), query)
                || containsIgnoreCase(event.getDescription(), query)
                || containsIgnoreCase(event.getImage(), query)
                || containsIgnoreCase(event.getLocation(), query)
                || dateValue.toLowerCase(Locale.ROOT).contains(query)
                || createdAtValue.toLowerCase(Locale.ROOT).contains(query)
                || String.valueOf(event.getMaxTickets()).contains(query)
                || String.valueOf(event.getMaxVipTickets()).contains(query)
                || String.valueOf(event.getMaxNormalTickets()).contains(query)
                || String.valueOf(event.getCreatorId()).contains(query)
                || String.valueOf(event.getLocalId()).contains(query)
                || String.valueOf(event.getId()).contains(query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private List<String> getParticipantEmails(int eventId) throws SQLException {
        if (!tableExists("ticket")) {
            return List.of();
        }

        String userTable = findUserTableName();
        if (userTable == null) {
            return List.of();
        }

        Set<String> emails = new LinkedHashSet<>();
        String sql = "SELECT DISTINCT u.email FROM ticket t "
                + "JOIN `" + userTable + "` u ON u.id = t.user_id "
                + "WHERE t.event_id = ? AND u.email IS NOT NULL AND u.email <> ''";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, eventId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String email = rs.getString("email");
                    if (email != null && !email.isBlank()) {
                        emails.add(email.trim());
                    }
                }
            }
        }

        return new ArrayList<>(emails);
    }

    private void deleteEventAndTickets(int eventId) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);

            if (tableExists("ticket")) {
                try (PreparedStatement deleteTickets = connection.prepareStatement("DELETE FROM ticket WHERE event_id=?")) {
                    deleteTickets.setInt(1, eventId);
                    deleteTickets.executeUpdate();
                }
            }

            try (PreparedStatement deleteEvent = connection.prepareStatement("DELETE FROM event WHERE id=?")) {
                deleteEvent.setInt(1, eventId);
                deleteEvent.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rs = metadata.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }

        try (ResultSet rs = metadata.getTables(connection.getCatalog(), null, tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private String findUserTableName() throws SQLException {
        if (tableExists("user")) {
            return "user";
        }
        if (tableExists("users")) {
            return "users";
        }
        return null;
    }

    private String buildCancellationEmail(Event event, Local local) {
        StringBuilder body = new StringBuilder();
        body.append("Bonjour,").append("\n\n");
        body.append("Nous vous informons que l'event suivant a ete annule :").append("\n\n");
        body.append("Titre : ").append(safeText(event.getTitle(), "Sans titre")).append('\n');
        body.append("Date : ").append(event.getDate() != null ? event.getDate().toString() : "Non precisee").append('\n');
        body.append("Lieu : ").append(safeText(event.getLocation(), "Non precise")).append('\n');

        if (local != null) {
            body.append("Local : ").append(safeText(local.getName(), "Local sans nom")).append('\n');
            body.append("Adresse : ").append(safeText(local.getAddress(), "Adresse non precisee")).append('\n');
        }

        body.append("\nMerci pour votre comprehension.").append('\n');
        body.append("Equipe Mythoria");
        return body.toString();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Event mapEvent(ResultSet rs) throws SQLException {
        Event event = new Event();
        event.setId(rs.getInt("id"));
        event.setTitle(rs.getString("title"));
        event.setDescription(rs.getString("description"));
        event.setImage(rs.getString("image"));

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

    private int getNextId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM event";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new SQLException("Impossible de generer un identifiant pour l'event.");
    }
}
