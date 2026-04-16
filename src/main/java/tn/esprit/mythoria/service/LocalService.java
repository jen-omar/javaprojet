package tn.esprit.mythoria.service;

import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalService implements IService<Local>{
    private Connection connection;
    public LocalService(){
        connection= MyDatabase.getInstance().getConnection();
    }
    @Override
    public void ajouter(Local local) throws SQLException {
        String sql="INSERT INTO `local`(`name`, `description`, `price`, `address`, `capacity`, `image`, `status`) VALUES (?,?,?,?,?,?,?)";
        PreparedStatement ps =connection.prepareStatement(sql);
        ps.setString(1,local.getName());
        ps.setString(2, local.getDescription());
        ps.setDouble(3, local.getPrice());
        ps.setString(4, local.getAddress());
        ps.setInt(5, local.getCapacity());
        ps.setString(6, local.getImage());
        ps.setString(7, local.getStatus());
        ps.executeUpdate();
        System.out.println("Local ajouter avec succes");
    }

    @Override
    public void modifier(Local local) throws SQLException {
        String sql = "UPDATE local SET name=?, description=?, price=?, address=?, capacity=?, image=?, status=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, local.getName());
        ps.setString(2, local.getDescription());
        ps.setDouble(3, local.getPrice());
        ps.setString(4, local.getAddress());
        ps.setInt(5, local.getCapacity());
        ps.setString(6, local.getImage());
        ps.setString(7, local.getStatus());
        ps.setInt(8, local.getId());

        ps.executeUpdate();
        System.out.println("Local modifié avec succès.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM local WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
        System.out.println("Local supprimé avec succès.");
    }

    @Override
    public List<Local> afficher() throws SQLException {
        List<Local> locals = new ArrayList<>();
        String sql = "SELECT * FROM local";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while(rs.next()){
            Local local=new Local();
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
}
