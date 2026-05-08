package tn.esprit.mythoria.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Event;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.EventService;
import tn.esprit.mythoria.utils.AiDescriptionUtil;
import tn.esprit.mythoria.utils.AiImageUtil;
import tn.esprit.mythoria.utils.LocalImageUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FormEventController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label lblLocal;

    @FXML
    private Label lblLocalMeta;

    @FXML
    private Label lblLocalStatus;

    @FXML
    private ImageView localImagePreview;

    @FXML
    private Label imagePlaceholder;

    @FXML
    private TextField tfTitle;

    @FXML
    private TextArea taDescription;

    @FXML
    private TextField tfImage;

    @FXML
    private ImageView eventImagePreview;

    @FXML
    private Label eventImagePlaceholder;

    @FXML
    private DatePicker dpDate;

    @FXML
    private TextField tfLocation;

    @FXML
    private TextField tfMaxTickets;

    @FXML
    private TextField tfMaxVipTickets;

    @FXML
    private TextField tfMaxNormalTickets;

    @FXML
    private Button btnSave;

    @FXML
    private Button btnImproveDescription;

    @FXML
    private Button btnGenerateEventImage;

    private Local localSelectionne;
    private Event eventActuel;
    private boolean modeModification = false;

    private final EventService eventService = new EventService();

    @FXML
    public void initialize() {
        dpDate.setEditable(false);
        tfLocation.setEditable(false);
        tfLocation.setFocusTraversable(false);
        tfImage.textProperty().addListener((observable, oldValue, newValue) -> refreshEventImagePreview(newValue));
        refreshEventImagePreview(tfImage.getText());
    }

    public void setLocalSelectionne(Local local) {
        this.localSelectionne = local;
        syncLocationFromLocal();
        refreshLocalPreview();
    }

    public void setModeAjout() {
        modeModification = false;
        eventActuel = null;
        titleLabel.setText("Ajouter un Event");
        btnSave.setText("Enregistrer");
        syncLocationFromLocal();
        refreshEventImagePreview(tfImage.getText());
    }

    public void setModeModification(Event event) {
        modeModification = true;
        eventActuel = event;
        titleLabel.setText("Modifier un Event");
        btnSave.setText("Modifier");

        tfTitle.setText(event.getTitle());
        taDescription.setText(event.getDescription());
        tfImage.setText(event.getImage());
        if (event.getDate() != null) {
            dpDate.setValue(event.getDate().toLocalDate());
        }
        tfMaxTickets.setText(String.valueOf(event.getMaxTickets()));
        tfMaxVipTickets.setText(String.valueOf(event.getMaxVipTickets()));
        tfMaxNormalTickets.setText(String.valueOf(event.getMaxNormalTickets()));

        syncLocationFromLocal();
        refreshLocalPreview();
        refreshEventImagePreview(event.getImage());
    }

    @FXML
    public void chooseEventImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image pour l'event");
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
    public void generateEventImage() {
        String prompt = buildEventImagePrompt();
        if (prompt.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Image IA",
                    "Ajoutez au moins un titre ou une description avant de generer l'image.");
            return;
        }

        setGenerateImageButtonLoading(true);
        int eventId = eventActuel != null ? eventActuel.getId() : 0;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return AiImageUtil.generateEventImage(prompt, eventId);
            }
        };

        task.setOnSucceeded(event -> {
            setGenerateImageButtonLoading(false);
            tfImage.setText(task.getValue());
            refreshEventImagePreview(task.getValue());
            showAlert(Alert.AlertType.INFORMATION, "Image IA", "Image generee et ajoutee au formulaire.");
        });

        task.setOnFailed(event -> {
            setGenerateImageButtonLoading(false);
            Throwable exception = task.getException();
            String message = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Impossible de generer l'image.";
            showAlert(Alert.AlertType.ERROR, "Image IA", message);
        });

        Thread thread = new Thread(task, "event-image-ai");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void improveDescription() {
        runDescriptionImprovement("event");
    }

    @FXML
    public void saveEvent() {
        if (!controleSaisie()) {
            return;
        }

        try {
            LocalDate dateChoisie = dpDate.getValue();
            int maxTickets = Integer.parseInt(tfMaxTickets.getText().trim());
            int maxVipTickets = Integer.parseInt(tfMaxVipTickets.getText().trim());
            int maxNormalTickets = Integer.parseInt(tfMaxNormalTickets.getText().trim());
            String location = localSelectionne.getAddress();
            String image = normalizeImage(tfImage.getText());

            if (modeModification) {
                if (eventService.existeAutreEventPourLocalEtDate(localSelectionne.getId(), dateChoisie, eventActuel.getId())) {
                    showAlert(Alert.AlertType.WARNING, "Conflit",
                            "Impossible de modifier : un autre event existe deja dans ce local a cette date.");
                    return;
                }

                eventActuel.setTitle(tfTitle.getText().trim());
                eventActuel.setDescription(taDescription.getText().trim());
                eventActuel.setImage(image);
                eventActuel.setDate(dateChoisie.atStartOfDay());
                eventActuel.setLocation(location);
                eventActuel.setMaxTickets(maxTickets);
                eventActuel.setMaxVipTickets(maxVipTickets);
                eventActuel.setMaxNormalTickets(maxNormalTickets);
                eventActuel.setCreatorId(1);
                eventActuel.setLocalId(localSelectionne.getId());

                if (eventActuel.getCreatedAt() == null) {
                    eventActuel.setCreatedAt(LocalDateTime.now());
                }

                eventService.modifier(eventActuel);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Event modifie avec succes.");
            } else {
                if (eventService.existeEventPourLocalEtDate(localSelectionne.getId(), dateChoisie)) {
                    showAlert(Alert.AlertType.WARNING, "Conflit",
                            "Impossible d'ajouter un evenement au meme local le meme jour.");
                    return;
                }

                Event event = new Event();
                event.setTitle(tfTitle.getText().trim());
                event.setDescription(taDescription.getText().trim());
                event.setImage(image);
                event.setDate(dateChoisie.atStartOfDay());
                event.setLocation(location);
                event.setCreatedAt(LocalDateTime.now());
                event.setMaxTickets(maxTickets);
                event.setMaxVipTickets(maxVipTickets);
                event.setMaxNormalTickets(maxNormalTickets);
                event.setCreatorId(1);
                event.setLocalId(localSelectionne.getId());

                eventService.ajouter(event);
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Event ajoute avec succes.");
            }

            retourListe();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    e.getMessage() != null ? e.getMessage() : "Une erreur inattendue est survenue.");
        }
    }

    private boolean controleSaisie() {
        String erreurs = "";

        if (localSelectionne == null) {
            erreurs += "- Veuillez selectionner un local.\n";
        } else if (!modeModification && isLocalUnderMaintenance()) {
            erreurs += "- Impossible d'ajouter un event sur un local en maintenance.\n";
        }

        if (tfTitle.getText() == null || tfTitle.getText().trim().isEmpty()) {
            erreurs += "- Le titre est obligatoire.\n";
        }

        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty()) {
            erreurs += "- La description est obligatoire.\n";
        }

        if (dpDate.getValue() == null) {
            erreurs += "- La date est obligatoire.\n";
        } else if (dpDate.getValue().isBefore(LocalDate.now())) {
            erreurs += "- La date doit etre egale ou posterieure a aujourd'hui.\n";
        }

        if (tfMaxTickets.getText() == null || tfMaxTickets.getText().trim().isEmpty()) {
            erreurs += "- Le nombre max de tickets est obligatoire.\n";
        } else {
            try {
                Integer.parseInt(tfMaxTickets.getText().trim());
            } catch (NumberFormatException e) {
                erreurs += "- Le nombre max de tickets doit etre un entier valide.\n";
            }
        }

        if (tfMaxVipTickets.getText() == null || tfMaxVipTickets.getText().trim().isEmpty()) {
            erreurs += "- Le nombre max de tickets VIP est obligatoire.\n";
        } else {
            try {
                Integer.parseInt(tfMaxVipTickets.getText().trim());
            } catch (NumberFormatException e) {
                erreurs += "- Le nombre max de tickets VIP doit etre un entier valide.\n";
            }
        }

        if (tfMaxNormalTickets.getText() == null || tfMaxNormalTickets.getText().trim().isEmpty()) {
            erreurs += "- Le nombre max de tickets normaux est obligatoire.\n";
        } else {
            try {
                Integer.parseInt(tfMaxNormalTickets.getText().trim());
            } catch (NumberFormatException e) {
                erreurs += "- Le nombre max de tickets normaux doit etre un entier valide.\n";
            }
        }

        if (erreurs.isEmpty()) {
            int maxTickets = Integer.parseInt(tfMaxTickets.getText().trim());
            int maxVipTickets = Integer.parseInt(tfMaxVipTickets.getText().trim());
            int maxNormalTickets = Integer.parseInt(tfMaxNormalTickets.getText().trim());

            if (maxTickets <= 0) {
                erreurs += "- Le nombre max de tickets doit etre superieur a 0.\n";
            }

            if (maxVipTickets < 0) {
                erreurs += "- Le nombre max de tickets VIP ne peut pas etre negatif.\n";
            }

            if (maxNormalTickets < 0) {
                erreurs += "- Le nombre max de tickets normaux ne peut pas etre negatif.\n";
            }

            if (maxVipTickets + maxNormalTickets > maxTickets) {
                erreurs += "- La somme des tickets VIP et normaux ne doit pas depasser le total.\n";
            }
        }

        if (!erreurs.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreurs de saisie", erreurs);
            return false;
        }

        return true;
    }

    @FXML
    public void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/GestionLocalEvent.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) tfTitle.getScene().getWindow();
            stage.setTitle("Gestion des Events par Local");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir a la liste : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
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

        Thread thread = new Thread(task, "event-description-ai");
        thread.setDaemon(true);
        thread.start();
    }

    private void setImproveButtonLoading(boolean loading) {
        if (btnImproveDescription != null) {
            btnImproveDescription.setDisable(loading);
            btnImproveDescription.setText(loading ? "..." : "Ameliorer IA");
        }
    }

    private void setGenerateImageButtonLoading(boolean loading) {
        if (btnGenerateEventImage != null) {
            btnGenerateEventImage.setDisable(loading);
            btnGenerateEventImage.setText(loading ? "..." : "Generer IA");
        }
    }

    private String buildEventImagePrompt() {
        String title = textValue(tfTitle);
        String description = textValue(taDescription);
        String date = dpDate != null && dpDate.getValue() != null ? dpDate.getValue().toString() : "";
        String location = textValue(tfLocation);
        String localName = localSelectionne != null ? fallback(localSelectionne.getName(), "") : "";
        String localAddress = localSelectionne != null ? fallback(localSelectionne.getAddress(), "") : "";
        String localStatus = localSelectionne != null ? fallback(localSelectionne.getStatus(), "") : "";
        String tickets = textValue(tfMaxTickets);
        String vipTickets = textValue(tfMaxVipTickets);
        String normalTickets = textValue(tfMaxNormalTickets);

        if (title.isBlank() && description.isBlank()) {
            return "";
        }

        return """
                Professional event promotional image, realistic cinematic photo style, no text, no logo.
                Event title: %s.
                Event description: %s.
                Event date: %s.
                Location: %s.
                Local name: %s.
                Local address: %s.
                Local status: %s.
                Ticket capacity: %s total, %s VIP, %s normal.
                High quality venue atmosphere, suitable for an event management application.
                """.formatted(
                title,
                description,
                date,
                location,
                localName,
                localAddress,
                localStatus,
                tickets,
                vipTickets,
                normalTickets
        ).trim();
    }

    private String textValue(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String textValue(TextArea field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void refreshLocalPreview() {
        if (localSelectionne == null) {
            lblLocal.setText("Aucun local selectionne");
            lblLocalMeta.setText("Selectionnez un local avant de creer un event.");
            lblLocalStatus.setText("");
            localImagePreview.setImage(null);
            localImagePreview.setVisible(false);
            localImagePreview.setManaged(false);
            imagePlaceholder.setText("Aucune image locale");
            imagePlaceholder.setVisible(true);
            imagePlaceholder.setManaged(true);
            return;
        }

        lblLocal.setText("Local lie : " + localSelectionne.getName());
        lblLocalMeta.setText(localSelectionne.getAddress() + " | Capacite " + localSelectionne.getCapacity());
        lblLocalStatus.setText("Statut : " + localSelectionne.getStatus());

        Image image = LocalImageUtil.loadImage(localSelectionne.getImage(), 220, 220);
        boolean hasImage = image != null;

        localImagePreview.setImage(image);
        localImagePreview.setVisible(hasImage);
        localImagePreview.setManaged(hasImage);

        imagePlaceholder.setText(hasImage ? "" : "Image du local introuvable");
        imagePlaceholder.setVisible(!hasImage);
        imagePlaceholder.setManaged(!hasImage);
    }

    private void refreshEventImagePreview(String imageSource) {
        Image image = LocalImageUtil.loadImage(imageSource, 220, 160);
        boolean hasImage = image != null;

        eventImagePreview.setImage(image);
        eventImagePreview.setVisible(hasImage);
        eventImagePreview.setManaged(hasImage);

        eventImagePlaceholder.setVisible(!hasImage);
        eventImagePlaceholder.setManaged(!hasImage);

        if (imageSource == null || imageSource.isBlank()) {
            eventImagePlaceholder.setText("Aucune image d'event");
        } else if (!hasImage) {
            eventImagePlaceholder.setText("Image d'event introuvable");
        } else {
            eventImagePlaceholder.setText("");
        }
    }

    private void syncLocationFromLocal() {
        if (localSelectionne != null) {
            tfLocation.setText(localSelectionne.getAddress());
        } else {
            tfLocation.clear();
        }
    }

    private String normalizeImage(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isLocalUnderMaintenance() {
        return localSelectionne != null && "EN_MAINTENANCE".equalsIgnoreCase(localSelectionne.getStatus());
    }
}
