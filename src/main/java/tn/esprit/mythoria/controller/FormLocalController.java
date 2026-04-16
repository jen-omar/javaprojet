package tn.esprit.mythoria.controller;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.LocalService;

import java.io.IOException;
import java.sql.SQLException;
public class FormLocalController {
    @FXML
    private Label titleLabel;

    @FXML
    private TextField tfName;

    @FXML
    private TextArea taDescription;

    @FXML
    private TextField tfPrice;

    @FXML
    private TextField tfAddress;

    @FXML
    private TextField tfCapacity;

    @FXML
    private TextField tfImage;

    @FXML
    private ComboBox<String> cbStatus;

    @FXML
    private Button btnSave;
    LocalService localService = new LocalService();
    private boolean modeModification = false;
    private Local localActuel;
    @FXML
    public void initialize() {
        cbStatus.setItems(FXCollections.observableArrayList(
                "DISPONIBLE",
                "INDISPONIBLE",
                "EN_MAINTENANCE"
        ));
    }
    public void setModeAjout() {
        modeModification = false;
        localActuel = null;
        titleLabel.setText("Ajouter un Local");
        btnSave.setText("Enregistrer");
    }
    public void setModeModification(Local local) {
        modeModification = true;
        localActuel = local;
        titleLabel.setText("Modifier un Local");
        btnSave.setText("Modifier");
        tfName.setText(local.getName());
        taDescription.setText(local.getDescription());
        tfPrice.setText(String.valueOf(local.getPrice()));
        tfAddress.setText(local.getAddress());
        tfCapacity.setText(String.valueOf(local.getCapacity()));
        tfImage.setText(local.getImage());
        cbStatus.setValue(local.getStatus());
    }
    @FXML
    public void saveLocal(){
        if(!controleSaisie()){
            return;
        }
        try{
            String name = tfName.getText().trim();
            String description = taDescription.getText().trim();
            double price = Double.parseDouble(tfPrice.getText().trim());
            String address = tfAddress.getText().trim();
            int capacity = Integer.parseInt(tfCapacity.getText().trim());
            String image = tfImage.getText().trim();
            String status = cbStatus.getValue();
            if(modeModification){
                localActuel.setName(name);
                localActuel.setDescription(description);
                localActuel.setPrice(price);
                localActuel.setAddress(address);
                localActuel.setCapacity(capacity);
                localActuel.setImage(image);
                localActuel.setStatus(status);
                localService.modifier(localActuel);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Local modifié avec succès.");
            }else{
                Local local = new Local(name, description, price, address, capacity, image, status);
                localService.ajouter(local);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Local ajouté avec succès.");

            }
            retourListe();
        }catch (SQLException e){
            showAlert(Alert.AlertType.ERROR,"Erruer SQL",e.getMessage());
        }
    }
    @FXML
    public void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/GestionLocal.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) tfName.getScene().getWindow();
            stage.setTitle("Gestion des Locaux");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à la liste : " + e.getMessage());
        }
    }
    private boolean controleSaisie(){
        String erreurs="";
        if (tfName.getText() == null || tfName.getText().trim().isEmpty()) {
            erreurs+="- Le nom est obligatoire.\n";
        }
        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty()) {
            erreurs += "- La description est obligatoire.\n";
        }
        if (tfAddress.getText() == null || tfAddress.getText().trim().isEmpty()) {
            erreurs += "- L'adresse est obligatoire.\n";
        }
        if (tfImage.getText() == null || tfImage.getText().trim().isEmpty()) {
            erreurs += "- L'image est obligatoire.\n";
        }

        if (cbStatus.getValue() == null) {
            erreurs += "- Veuillez choisir un statut.\n";
        }
        if (tfCapacity.getText() == null || tfCapacity.getText().trim().isEmpty()) {
            erreurs += "- La capacité est obligatoire.\n";
        }else{
            try{
                Integer.parseInt(tfCapacity.getText().trim());
            }catch (NumberFormatException e){
                erreurs+="- La capacité doit être un entier valide.\n";
            }
        }
        if (tfPrice.getText() == null || tfPrice.getText().trim().isEmpty()) {
            erreurs += "- Le prix est obligatoire.\n";
        } else {
            try {
                Double.parseDouble(tfPrice.getText().trim());
            } catch (NumberFormatException e) {
                erreurs += "- Le prix doit être un nombre valide.\n";
            }
        }
        if (!erreurs.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreurs de saisie", erreurs);
            return false;
        }
        return true;


    }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
