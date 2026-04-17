package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.Models.User;
import tn.esprit.services.UserDAO;
import tn.esprit.util.UserSession;

public class AdminUsersController {

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colRole;

    @FXML private ComboBox<String> roleChanger;

    private final UserDAO userDAO = new UserDAO();
    private final ObservableList<User> usersData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Enforce Admin access
        User currentUser = UserSession.getInstance().getUser();
        if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getPrimaryRole())) {
            System.err.println("UNAUTHORIZED ACCESS TO ADMIN PANEL");
            return;
        }

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        // Map the method getPrimaryRole to the column
        colRole.setCellValueFactory(new PropertyValueFactory<>("primaryRole"));

        roleChanger.setItems(FXCollections.observableArrayList("ROLE_ADMIN", "ROLE_AUTHOR", "ROLE_CLIENT"));
        
        loadUsers();
    }

    private void loadUsers() {
        usersData.setAll(userDAO.getAll());
        usersTable.setItems(usersData);
    }
    
    @FXML
    public void onRefresh() {
        loadUsers();
    }

    @FXML
    public void onChangeRole() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        String newRole = roleChanger.getValue();

        if (selected != null && newRole != null) {
            if (selected.getId() == UserSession.getInstance().getUser().getId()) {
                System.out.println("❌ You cannot alter your own admin privileges from here.");
                return;
            }
            selected.setRoles("[\"" + newRole + "\"]");
            userDAO.update(selected);
            loadUsers();
        }
    }

    @FXML
    public void onDeleteUser() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected.getId() == UserSession.getInstance().getUser().getId()) {
                System.out.println("❌ You cannot ban yourself.");
                return;
            }
            userDAO.delete(selected.getId());
            loadUsers();
        }
    }
}
