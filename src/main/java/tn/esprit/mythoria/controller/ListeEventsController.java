package tn.esprit.mythoria.controller;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Event;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.EventService;

import java.io.IOException;
import java.sql.SQLException;
public class ListeEventsController {
    @FXML
    private Label lblTitre;

    @FXML
    private ListView<Event> lvEvents;
    EventService eventService = new EventService();
    private Local localSelectionne;
    private final int creatorId = 1;
    public void setLocalSelectionne(Local local) {
        this.localSelectionne = local;
        lblTitre.setText("Events du local : " + local.getName());
        chargerEvents();
    }
    private void chargerEvents() {
        try {
            lvEvents.setItems(FXCollections.observableArrayList(
                    eventService.afficherParLocalEtCreateur(localSelectionne.getId(), creatorId)
            ));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les events : " + e.getMessage());
        }
    }
    @FXML
    public void gotoModifierEvent() {
        Event eventSelectionne = lvEvents.getSelectionModel().getSelectedItem();

        if (eventSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un event à modifier.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/FormEvent.fxml"));
            Scene scene = new Scene(loader.load());

            FormEventController controller = loader.getController();
            controller.setLocalSelectionne(localSelectionne);
            controller.setModeModification(eventSelectionne);

            Stage stage = (Stage) lvEvents.getScene().getWindow();
            stage.setTitle("Modifier Event");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    @FXML
    public void supprimerEvent() {
        Event eventSelectionne = lvEvents.getSelectionModel().getSelectedItem();

        if (eventSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un event à supprimer.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment supprimer l'event : " + eventSelectionne.getTitle() + " ?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                eventService.supprimer(eventSelectionne.getId());
                chargerEvents();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Event supprimé avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }
    @FXML
    public void retourGestionLocalEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/GestionLocalEvent.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) lvEvents.getScene().getWindow();
            stage.setTitle("Gestion des Events par Local");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
