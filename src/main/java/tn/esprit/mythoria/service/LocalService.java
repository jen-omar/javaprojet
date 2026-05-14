package tn.esprit.mythoria.service;

import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LocalService implements IService<Local> {
    private Connection connection;

    public LocalService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Local local) throws SQLException {
        if (connection == null) {
            throw new SQLException("Connexion a la base de donnees indisponible.");
        }

        int nextId = local.getId() > 0 ? local.getId() : getNextId();
        String sql = "INSERT INTO `local`(`id`, `name`, `description`, `price`, `address`, `capacity`, `image`, `status`) VALUES (?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, nextId);
            ps.setString(2, local.getName());
            ps.setString(3, local.getDescription());
            ps.setDouble(4, local.getPrice());
            ps.setString(5, local.getAddress());
            ps.setInt(6, local.getCapacity());
            ps.setString(7, local.getImage());
            ps.setString(8, local.getStatus());
            ps.executeUpdate();
        }

        local.setId(nextId);
        System.out.println("Local ajoute avec succes");
    }

    @Override
    public void modifier(Local local) throws SQLException {
        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            String updateLocalSql = "UPDATE local SET name=?, description=?, price=?, address=?, capacity=?, image=?, status=? WHERE id=?";
            try (PreparedStatement ps = connection.prepareStatement(updateLocalSql)) {
                ps.setString(1, local.getName());
                ps.setString(2, local.getDescription());
                ps.setDouble(3, local.getPrice());
                ps.setString(4, local.getAddress());
                ps.setInt(5, local.getCapacity());
                ps.setString(6, local.getImage());
                ps.setString(7, local.getStatus());
                ps.setInt(8, local.getId());
                ps.executeUpdate();
            }

            // Event location must always mirror the linked local address.
            String updateEventsSql = "UPDATE event SET location=? WHERE local_id=?";
            try (PreparedStatement ps = connection.prepareStatement(updateEventsSql)) {
                ps.setString(1, local.getAddress());
                ps.setInt(2, local.getId());
                ps.executeUpdate();
            }

            connection.commit();
            System.out.println("Local modifie avec succes.");
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            String deleteEventsSql = "DELETE FROM event WHERE local_id=?";
            try (PreparedStatement ps = connection.prepareStatement(deleteEventsSql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            String deleteLocalSql = "DELETE FROM local WHERE id=?";
            try (PreparedStatement ps = connection.prepareStatement(deleteLocalSql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            connection.commit();
            System.out.println("Local supprime avec succes.");
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    @Override
    public List<Local> afficher() throws SQLException {
        List<Local> locals = new ArrayList<>();
        String sql = "SELECT * FROM local";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Local local = new Local();
            local.setId(rs.getInt("id"));
            local.setName(rs.getString("name"));
            local.setDescription(rs.getString("description"));
            local.setPrice(rs.getDouble("price"));
            local.setAddress(rs.getString("address"));
            local.setCapacity(rs.getInt("capacity"));
            local.setImage(rs.getString("image"));
            local.setStatus(rs.getString("status"));
            locals.add(local);
        }
        return locals;
    }

    @Override
    public Local getById(int id) throws SQLException {
        String sql = "SELECT * FROM local WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Local(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getString("address"),
                    rs.getInt("capacity"),
                    rs.getString("image"),
                    rs.getString("status")
            );
        }

        return null;
    }

    public List<Local> rechercherEtTrierLocaux(String searchText, String sortOption) throws SQLException {
        String query = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);

        List<Local> filteredLocals = afficher().stream()
                .filter(local -> matchesSearch(local, query))
                .toList();

        return sortLocauxByCritere(filteredLocals, sortOption);
    }

    public List<Local> sortLocauxByCritere(String critere) throws SQLException {
        return sortLocauxByCritere(afficher(), critere);
    }

    public List<Local> sortLocauxByCritere(List<Local> locals, String critere) {
        Comparator<Local> comparator;
        switch (critere) {
            case "Name":
                comparator = Comparator.comparing(Local::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Description":
                comparator = Comparator.comparing(Local::getDescription, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Address":
                comparator = Comparator.comparing(Local::getAddress, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Price":
                comparator = Comparator.comparingDouble(Local::getPrice);
                break;
            case "Capacity":
                comparator = Comparator.comparingInt(Local::getCapacity);
                break;
            case "Status":
                comparator = Comparator.comparing(Local::getStatus, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            default:
                comparator = Comparator.comparing(Local::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
        }

        return locals.stream().sorted(comparator).toList();
    }

    private boolean matchesSearch(Local local, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        return containsIgnoreCase(local.getName(), query)
                || containsIgnoreCase(local.getDescription(), query)
                || containsIgnoreCase(local.getAddress(), query)
                || containsIgnoreCase(local.getImage(), query)
                || containsIgnoreCase(local.getStatus(), query)
                || String.valueOf(local.getPrice()).contains(query)
                || String.valueOf(local.getCapacity()).contains(query)
                || String.valueOf(local.getId()).contains(query);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private int getNextId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM `local`";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new SQLException("Impossible de generer un identifiant pour le local.");
    }
}
