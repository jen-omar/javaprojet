package tn.esprit.mythoria.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.LocalService;
import tn.esprit.mythoria.utils.AiDescriptionUtil;
import tn.esprit.mythoria.utils.LocalImageUtil;

import java.io.File;
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
    private TextArea taAiLocalPrompt;

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
    private ImageView imagePreview;

    @FXML
    private Label imagePlaceholder;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnImproveDescription;

    @FXML
    private Button btnGenerateLocalFromAi;

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
        tfImage.textProperty().addListener((observable, oldValue, newValue) -> refreshImagePreview(newValue));
        refreshImagePreview(tfImage.getText());
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
        refreshImagePreview(local.getImage());
    }

    @FXML
    public void saveLocal() {
        if (!controleSaisie()) {
            return;
        }

        try {
            String name = tfName.getText().trim();
            String description = taDescription.getText().trim();
            double price = parsePrice();
            String address = tfAddress.getText().trim();
            int capacity = Integer.parseInt(tfCapacity.getText().trim());
            String image = tfImage.getText().trim();
            String status = cbStatus.getValue();

            if (modeModification) {
                localActuel.setName(name);
                localActuel.setDescription(description);
                localActuel.setPrice(price);
                localActuel.setAddress(address);
                localActuel.setCapacity(capacity);
                localActuel.setImage(image);
                localActuel.setStatus(status);
                localService.modifier(localActuel);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Local modifie avec succes.");
            } else {
                Local local = new Local(name, description, price, address, capacity, image, status);
                localService.ajouter(local);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Local ajoute avec succes.");
            }

            retourListe();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (Exception e) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Erreur",
                    e.getMessage() != null ? e.getMessage() : "Une erreur inattendue est survenue."
            );
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir a la liste : " + e.getMessage());
        }
    }

    @FXML
    public void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        Stage stage = (Stage) btnSave.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            tfImage.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    public void improveDescription() {
        runDescriptionImprovement("local");
    }

    @FXML
    public void generateLocalFromAi() {
        String prompt = taAiLocalPrompt != null ? taAiLocalPrompt.getText() : "";
        if (prompt == null || prompt.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "IA", "Decrivez le local avant de remplir le formulaire.");
            return;
        }

        setGenerateLocalButtonLoading(true);

        Task<AiDescriptionUtil.LocalDraft> task = new Task<>() {
            @Override
            protected AiDescriptionUtil.LocalDraft call() throws Exception {
                return AiDescriptionUtil.generateLocalDraft(prompt);
            }
        };

        task.setOnSucceeded(event -> {
            setGenerateLocalButtonLoading(false);
            applyLocalDraft(task.getValue());
            showAlert(Alert.AlertType.INFORMATION, "IA", "Le formulaire a ete rempli. Verifiez les champs avant d'enregistrer.");
        });

        task.setOnFailed(event -> {
            setGenerateLocalButtonLoading(false);
            Throwable exception = task.getException();
            String message = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Impossible de contacter le modele IA.";
            showAlert(Alert.AlertType.ERROR, "IA", message);
        });

        Thread thread = new Thread(task, "local-form-ai");
        thread.setDaemon(true);
        thread.start();
    }

    private boolean controleSaisie() {
        String erreurs = "";

        if (tfName.getText() == null || tfName.getText().trim().isEmpty()) {
            erreurs += "- Le nom est obligatoire.\n";
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
            erreurs += "- La capacite est obligatoire.\n";
        } else {
            try {
                int capacity = Integer.parseInt(tfCapacity.getText().trim());
                if (capacity <= 0) {
                    erreurs += "- La capacite doit etre superieure a 0.\n";
                }
            } catch (NumberFormatException e) {
                erreurs += "- La capacite doit etre un entier valide.\n";
            }
        }

        if (tfPrice.getText() == null || tfPrice.getText().trim().isEmpty()) {
            erreurs += "- Le prix est obligatoire.\n";
        } else {
            try {
                double price = parsePrice();
                if (price < 0) {
                    erreurs += "- Le prix ne peut pas etre negatif.\n";
                }
            } catch (NumberFormatException e) {
                erreurs += "- Le prix doit etre un nombre valide.\n";
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

    private void runDescriptionImprovement(String context) {
        String originalDescription = taDescription.getText();
        if (originalDescription == null || originalDescription.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "IA", "Veuillez saisir une description avant de lancer l'IA.");
            return;
        }

        setImproveButtonLoading(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return AiDescriptionUtil.improveDescription(originalDescription, context);
            }
        };

        task.setOnSucceeded(event -> {
            setImproveButtonLoading(false);
            taDescription.setText(task.getValue());
        });

        task.setOnFailed(event -> {
            setImproveButtonLoading(false);
            Throwable exception = task.getException();
            String message = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Impossible de contacter le modele IA.";
            showAlert(Alert.AlertType.ERROR, "IA", message);
        });

        Thread thread = new Thread(task, "local-description-ai");
        thread.setDaemon(true);
        thread.start();
    }

    private void setImproveButtonLoading(boolean loading) {
        if (btnImproveDescription != null) {
            btnImproveDescription.setDisable(loading);
            btnImproveDescription.setText(loading ? "..." : "Ameliorer IA");
        }
    }

    private void setGenerateLocalButtonLoading(boolean loading) {
        if (btnGenerateLocalFromAi != null) {
            btnGenerateLocalFromAi.setDisable(loading);
            btnGenerateLocalFromAi.setText(loading ? "Analyse..." : "Remplir formulaire");
        }
    }

    private void applyLocalDraft(AiDescriptionUtil.LocalDraft draft) {
        if (draft == null) {
            return;
        }

        setTextIfPresent(tfName, draft.name());
        setTextIfPresent(taDescription, draft.description());
        setTextIfPresent(tfAddress, draft.address());
        setTextIfPresent(tfImage, draft.image());

        if (draft.price() != null && draft.price() >= 0) {
            tfPrice.setText(formatNumber(draft.price()));
        }
        if (draft.capacity() != null && draft.capacity() > 0) {
            tfCapacity.setText(String.valueOf(draft.capacity()));
        }
        if (draft.status() != null && !draft.status().isBlank()) {
            cbStatus.setValue(draft.status());
        }
    }

    private void setTextIfPresent(TextField field, String value) {
        if (field != null && value != null && !value.isBlank()) {
            field.setText(value.trim());
        }
    }

    private void setTextIfPresent(TextArea field, String value) {
        if (field != null && value != null && !value.isBlank()) {
            field.setText(value.trim());
        }
    }

    private String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    private double parsePrice() {
        return Double.parseDouble(tfPrice.getText().trim().replace(',', '.'));
    }

    private void refreshImagePreview(String imageSource) {
        Image image = LocalImageUtil.loadImage(imageSource, 220, 220);
        boolean hasImage = image != null;

        imagePreview.setImage(image);
        imagePreview.setVisible(hasImage);
        imagePreview.setManaged(hasImage);

        imagePlaceholder.setVisible(!hasImage);
        imagePlaceholder.setManaged(!hasImage);

        if (imageSource == null || imageSource.isBlank()) {
            imagePlaceholder.setText("Aucune image selectionnee");
        } else if (!hasImage) {
            imagePlaceholder.setText("Image introuvable");
        } else {
            imagePlaceholder.setText("");
        }
    }
}
