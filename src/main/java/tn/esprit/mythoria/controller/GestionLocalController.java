package tn.esprit.mythoria.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.LocalService;

import java.io.IOException;
import java.sql.SQLException;

public class GestionLocalController {

    @FXML
    private ListView<Local> lvlocals;
    LocalService localService=new LocalService();
    @FXML
    public void initialize(){
        chargerLocaux();

    }
    private void chargerLocaux(){
        try {
            ObservableList<Local> list= FXCollections.observableList(localService.afficher());
            lvlocals.setItems(list);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void gotoAjouter(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/FormLocal.fxml"));
            Parent root = loader.load();

            FormLocalController controller = loader.getController();
            controller.setModeAjout();

            Stage stage = (Stage) lvlocals.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ajouter un local");
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page d'ajout : " + e.getMessage());
        }
    }

    @FXML
    void gotoModifier(ActionEvent event) {
        Local localSelectionne = lvlocals.getSelectionModel().getSelectedItem();

        if (localSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un local à modifier.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/FormLocal.fxml"));
            Parent root = loader.load();

            FormLocalController controller = loader.getController();
            controller.setModeModification(localSelectionne);

            Stage stage = (Stage) lvlocals.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Modifier un local");
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la page de modification : " + e.getMessage());
        }
    }

    @FXML
    void supprimerLocal(ActionEvent event) {
        Local selectedLocal=lvlocals.getSelectionModel().getSelectedItem();
        if(selectedLocal==null){
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un local à supprimer.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment supprimer le local : " + selectedLocal.getName() + " ?");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                localService.supprimer(selectedLocal.getId());
                chargerLocaux();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Local supprimé avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
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
