package tn.esprit.mythoria.controller;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.LocalService;

import java.io.IOException;
import java.sql.SQLException;
public class GestionLocalEventController {
    @FXML
    private ListView<Local> lvLocals;
    LocalService localService = new LocalService();
    @FXML
    public void initialize() {
        chargerLocaux();
    }
    private void chargerLocaux() {
        try {
            ObservableList<Local> list = FXCollections.observableArrayList(localService.afficher());
            lvLocals.setItems(list);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les locaux : " + e.getMessage());
        }
    }
    @FXML
    public void gotoAjouterEvent() {
        Local localSelectionne = lvLocals.getSelectionModel().getSelectedItem();

        if (localSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un local.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/FormEvent.fxml"));
            Scene scene = new Scene(loader.load());

            FormEventController controller = loader.getController();
            controller.setLocalSelectionne(localSelectionne);

            Stage stage = (Stage) lvLocals.getScene().getWindow();
            stage.setTitle("Ajouter Event");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire event : " + e.getMessage());
        }
    }
    @FXML
    public void gotoEvents() {
        Local localSelectionne = lvLocals.getSelectionModel().getSelectedItem();

        if (localSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un local.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/ListeEvents.fxml"));
            Scene scene = new Scene(loader.load());

            ListeEventsController controller = loader.getController();
            controller.setLocalSelectionne(localSelectionne);

            Stage stage = (Stage) lvLocals.getScene().getWindow();
            stage.setTitle("Liste des Events");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la liste des events : " + e.getMessage());
        }
    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
