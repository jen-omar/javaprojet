package tn.esprit.mythoria.controller;

import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Event;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.EventService;
import tn.esprit.mythoria.utils.LocalImageUtil;
import tn.esprit.mythoria.utils.MeteoApiUtil;
import tn.esprit.mythoria.utils.QrCodeUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ListeEventsController {
    @FXML
    private Label lblTitre;

    @FXML
    private Label lblLocalMeta;

    @FXML
    private Label lblLocalStatus;

    @FXML
    private ImageView localImagePreview;

    @FXML
    private Label imagePlaceholder;

    @FXML
    private ListView<Event> lvEvents;

    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cbSort;

    EventService eventService = new EventService();
    private Local localSelectionne;
    private final int creatorId = 1;

    @FXML
    public void initialize() {
        cbSort.setItems(FXCollections.observableArrayList("Date", "Title", "Description", "Location", "Ticket"));
        cbSort.getSelectionModel().selectFirst();
        lvEvents.setCellFactory(listView -> createEventCell());
        lvEvents.setPlaceholder(new Label("Aucun event trouve."));

        cbSort.valueProperty().addListener((observable, oldValue, newValue) -> chargerEvents());
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> chargerEvents());
    }

    public void setLocalSelectionne(Local local) {
        this.localSelectionne = local;
        refreshLocalHeader();
        chargerEvents();
    }

    private void chargerEvents() {
        if (localSelectionne == null) {
            return;
        }

        try {
            String searchText = txtSearch != null ? txtSearch.getText() : "";
            String sortOption = cbSort != null ? cbSort.getValue() : null;

            lvEvents.setItems(FXCollections.observableArrayList(
                    eventService.rechercherEtTrierParLocalEtCreateur(
                            sortOption, searchText, localSelectionne.getId(), creatorId
                    )
            ));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les events : " + e.getMessage());
        }
    }

    @FXML
    public void gotoModifierEvent() {
        Event eventSelectionne = lvEvents.getSelectionModel().getSelectedItem();

        if (eventSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez selectionner un event a modifier.");
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
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez selectionner un event a supprimer.");
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
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Event supprime avec succes.");
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

    private void refreshLocalHeader() {
        if (localSelectionne == null) {
            lblTitre.setText("Liste des Events");
            lblLocalMeta.setText("Aucun local selectionne.");
            lblLocalStatus.setText("");
            localImagePreview.setImage(null);
            localImagePreview.setVisible(false);
            localImagePreview.setManaged(false);
            imagePlaceholder.setText("Aucune image locale");
            imagePlaceholder.setVisible(true);
            imagePlaceholder.setManaged(true);
            return;
        }

        lblTitre.setText("Events du local : " + localSelectionne.getName());
        lblLocalMeta.setText(localSelectionne.getAddress() + " | Capacite " + localSelectionne.getCapacity());
        lblLocalStatus.setText("Statut : " + localSelectionne.getStatus());

        Image image = LocalImageUtil.loadImage(localSelectionne.getImage(), 150, 110);
        boolean hasImage = image != null;

        localImagePreview.setImage(image);
        localImagePreview.setVisible(hasImage);
        localImagePreview.setManaged(hasImage);

        imagePlaceholder.setText(hasImage ? "" : "Image du local introuvable");
        imagePlaceholder.setVisible(!hasImage);
        imagePlaceholder.setManaged(!hasImage);
    }

    private ListCell<Event> createEventCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Event event, boolean empty) {
                super.updateItem(event, empty);

                if (empty || event == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setStyle("-fx-background-color: transparent; -fx-padding: 6;");
                setGraphic(buildEventCard(event, isSelected()));
            }
        };
    }

    private HBox buildEventCard(Event event, boolean selected) {
        StackPane imageContainer = new StackPane();
        imageContainer.setMinSize(120, 96);
        imageContainer.setPrefSize(120, 96);
        imageContainer.setMaxSize(120, 96);
        imageContainer.setStyle(
                "-fx-background-color: #111214;"
                        + "-fx-border-color: #5a5a5a;"
                        + "-fx-border-radius: 16;"
                        + "-fx-background-radius: 16;"
        );

        ImageView imageView = new ImageView();
        imageView.setFitWidth(110);
        imageView.setFitHeight(86);
        imageView.setPreserveRatio(true);
        Image image = LocalImageUtil.loadImage(resolveEventImageSource(event), 110, 86);

        Label imageFallback = new Label(image == null ? "Pas d'image" : "");
        imageFallback.setStyle("-fx-text-fill: #8f9399; -fx-font-size: 12px; -fx-font-weight: bold;");

        imageView.setImage(image);
        imageContainer.getChildren().addAll(imageView, imageFallback);

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(limitText(event.getDescription(), 120));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #bcc1c8; -fx-font-size: 12px;");

        Label dateLabel = new Label("Date : " + formatDate(event));
        dateLabel.setStyle("-fx-text-fill: #d7dce3; -fx-font-size: 13px;");

        Label locationLabel = new Label("Lieu : " + fallback(event.getLocation(), "Non precise"));
        locationLabel.setStyle("-fx-text-fill: #d7dce3; -fx-font-size: 13px;");

        HBox tagsRow = new HBox(
                10,
                buildTag("Total " + event.getMaxTickets(), "#233245", "#dcecff"),
                buildTag("VIP " + event.getMaxVipTickets(), "#3f274a", "#f0dfff"),
                buildTag("Normal " + event.getMaxNormalTickets(), "#26382d", "#d8f5df")
        );
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(8, titleLabel, dateLabel, locationLabel, descriptionLabel, tagsRow);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        Button meteoButton = buildActionButton("Meteo");
        meteoButton.setOnAction(actionEvent -> chargerMeteo(event, meteoButton));

        Button qrCodeButton = buildActionButton("QR Code");
        qrCodeButton.setOnAction(actionEvent -> afficherQrCode(event));

        Button cancelButton = buildActionButton("Cancel");
        cancelButton.setOnAction(actionEvent -> cancelEvent(event, cancelButton));

        VBox actionsBox = new VBox(10, meteoButton, qrCodeButton, cancelButton);
        actionsBox.setAlignment(Pos.CENTER);

        HBox card = new HBox(16, imageContainer, textBox, actionsBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14));
        card.setStyle(buildCardStyle(selected));

        return card;
    }

    private void cancelEvent(Event event, Button cancelButton) {
        if (event == null) {
            showAlert(Alert.AlertType.WARNING, "Annulation", "Impossible d'annuler : event vide.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Annuler event");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Voulez-vous vraiment annuler l'event : " + fallback(event.getTitle(), "Sans titre") + " ?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        cancelButton.setDisable(true);
        cancelButton.setText("...");

        Task<EventService.EventCancellationResult> task = new Task<>() {
            @Override
            protected EventService.EventCancellationResult call() throws Exception {
                return eventService.cancelEvent(event, localSelectionne);
            }
        };

        task.setOnSucceeded(workerStateEvent -> {
            cancelButton.setDisable(false);
            cancelButton.setText("Cancel");
            chargerEvents();
            showAlert(Alert.AlertType.INFORMATION, "Annulation", buildCancellationMessage(task.getValue()));
        });

        task.setOnFailed(workerStateEvent -> {
            cancelButton.setDisable(false);
            cancelButton.setText("Cancel");
            Throwable exception = task.getException();
            String message = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Une erreur inattendue est survenue.";
            showAlert(Alert.AlertType.ERROR, "Annulation", "Impossible d'annuler l'event : " + message);
        });

        Thread thread = new Thread(task, "event-cancel-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private String buildCancellationMessage(EventService.EventCancellationResult result) {
        StringBuilder message = new StringBuilder("Event annule avec succes.");

        if (result == null) {
            return message.toString();
        }

        if (result.recipientCount() == 0) {
            message.append("\nAucun participant avec email n'a ete trouve.");
            return message.toString();
        }

        if (!result.emailConfigured()) {
            message.append("\n").append(result.recipientCount())
                    .append(" participant(s) trouve(s), mais l'email SMTP n'est pas configure.");
            return message.toString();
        }

        if (result.emailError() != null && !result.emailError().isBlank()) {
            message.append("\nL'event est annule, mais l'envoi email a echoue : ")
                    .append(result.emailError());
            return message.toString();
        }

        message.append("\nEmail envoye a ")
                .append(result.sentEmailCount())
                .append(" participant(s).");
        return message.toString();
    }

    private Label buildTag(String text, String backgroundColor, String textColor) {
        Label tag = new Label(text);
        tag.setStyle(
                "-fx-background-color: " + backgroundColor + ";"
                        + "-fx-text-fill: " + textColor + ";"
                        + "-fx-background-radius: 999;"
                        + "-fx-padding: 6 12 6 12;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
        );
        return tag;
    }

    private Button buildActionButton(String text) {
        Button button = new Button(text);
        button.setPrefHeight(36);
        button.setPrefWidth(92);
        button.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #c0c0c0, #8e8e8e);"
                        + "-fx-text-fill: black;"
                        + "-fx-font-size: 12px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 10;"
                        + "-fx-cursor: hand;"
        );
        return button;
    }

    private void afficherQrCode(Event event) {
        if (event == null) {
            showAlert(Alert.AlertType.WARNING, "QR Code", "Impossible de generer le QR code : event vide.");
            return;
        }

        try {
            Path outputFile = Paths.get("qrcodes", "event-" + event.getId() + "-local-" + resolveLocalId() + ".png");
            String qrCodePath = QrCodeUtil.generateQrCode(buildQrCodePayload(event), outputFile, 320);
            showQrCodePopup(event, qrCodePath);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "QR Code",
                    "Impossible de generer le QR code : " + (e.getMessage() != null ? e.getMessage() : "Erreur inconnue."));
        }
    }

    private String buildQrCodePayload(Event event) {
        StringBuilder payload = new StringBuilder();
        payload.append("Mythoria Event").append('\n');
        payload.append("Event ID: ").append(event.getId()).append('\n');
        payload.append("Title: ").append(fallback(event.getTitle(), "Sans titre")).append('\n');
        payload.append("Date: ").append(formatDate(event)).append('\n');
        payload.append("Location: ").append(fallback(resolveMeteoLocation(event), "Non precise")).append('\n');

        if (localSelectionne != null) {
            payload.append("Local ID: ").append(localSelectionne.getId()).append('\n');
            payload.append("Local: ").append(fallback(localSelectionne.getName(), "Local sans nom")).append('\n');
            payload.append("Address: ").append(fallback(localSelectionne.getAddress(), "Adresse non precisee")).append('\n');
        }

        payload.append("Tickets: ").append(event.getMaxTickets())
                .append(" total, ")
                .append(event.getMaxVipTickets())
                .append(" VIP, ")
                .append(event.getMaxNormalTickets())
                .append(" normal");

        return payload.toString();
    }

    private void showQrCodePopup(Event event, String qrCodePath) {
        ImageView qrImageView = new ImageView(new Image(Paths.get(qrCodePath).toUri().toString()));
        qrImageView.setFitWidth(320);
        qrImageView.setFitHeight(320);
        qrImageView.setPreserveRatio(true);

        Label details = new Label(formatQrCodeDetails(event));
        details.setWrapText(true);
        details.setStyle("-fx-font-size: 13px;");

        VBox content = new VBox(12, qrImageView, details);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(8));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("QR Code");
        alert.setHeaderText(fallback(event.getTitle(), "Event"));
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private String formatQrCodeDetails(Event event) {
        return "Local : " + (localSelectionne != null ? fallback(localSelectionne.getName(), "Local sans nom") : "N/A")
                + "\nDate : " + formatDate(event)
                + "\nLieu : " + fallback(resolveMeteoLocation(event), "Non precise");
    }

    private void chargerMeteo(Event event, Button meteoButton) {
        if (event == null || event.getDate() == null) {
            showAlert(Alert.AlertType.WARNING, "Meteo", "Impossible de charger la meteo : la date de l'event est vide.");
            return;
        }

        String location = resolveMeteoLocation(event);
        if (location == null || location.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Meteo", "Impossible de charger la meteo : le lieu de l'event est vide.");
            return;
        }

        LocalDate eventDate = event.getDate().toLocalDate();
        meteoButton.setDisable(true);
        meteoButton.setText("...");

        Task<MeteoApiUtil.MeteoForecast> task = new Task<>() {
            @Override
            protected MeteoApiUtil.MeteoForecast call() throws Exception {
                return MeteoApiUtil.getMeteo(location, eventDate);
            }
        };

        task.setOnSucceeded(workerStateEvent -> {
            meteoButton.setDisable(false);
            meteoButton.setText("Meteo");
            showMeteoPopup(event, task.getValue());
        });

        task.setOnFailed(workerStateEvent -> {
            meteoButton.setDisable(false);
            meteoButton.setText("Meteo");
            Throwable exception = task.getException();
            String message = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Une erreur inattendue est survenue.";
            showAlert(Alert.AlertType.ERROR, "Meteo", "Impossible de charger la meteo : " + message);
        });

        Thread thread = new Thread(task, "meteo-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void showMeteoPopup(Event event, MeteoApiUtil.MeteoForecast meteo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Meteo");
        alert.setHeaderText(fallback(event.getTitle(), "Event") + " - " + formatDate(meteo.date()));

        VBox content = new VBox(10);
        content.setPadding(new Insets(8));
        content.getChildren().addAll(
                buildMeteoLine("Lieu demande", meteo.requestedLocation()),
                buildMeteoLine("Lieu trouve", meteo.resolvedLocation() + countrySuffix(meteo.country())),
                buildMeteoLine("Description", meteo.weatherDescription() + " (code " + meteo.weatherCode() + ")"),
                buildMeteoLine("Temperature max", meteo.temperatureMax() + " " + meteo.temperatureUnit()),
                buildMeteoLine("Temperature min", meteo.temperatureMin() + " " + meteo.temperatureUnit()),
                buildMeteoLine("Precipitations", meteo.precipitationSum() + " " + meteo.precipitationUnit()),
                buildMeteoLine("Vent max", meteo.windSpeedMax() + " " + meteo.windSpeedUnit()),
                buildMeteoLine("Coordonnees", meteo.latitude() + ", " + meteo.longitude()),
                buildMeteoLine("Source", meteo.source())
        );

        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    private HBox buildMeteoLine(String label, String value) {
        Label labelNode = new Label(label + " :");
        labelNode.setMinWidth(130);
        labelNode.setStyle("-fx-font-weight: bold;");

        Label valueNode = new Label(fallback(value, "N/A"));
        valueNode.setWrapText(true);
        HBox.setHgrow(valueNode, Priority.ALWAYS);

        HBox row = new HBox(8, labelNode, valueNode);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private String buildCardStyle(boolean selected) {
        String borderColor = selected ? "#d6e4ff" : "#4f545b";
        String background = selected
                ? "linear-gradient(to right, #273447, #1c242f)"
                : "linear-gradient(to right, #202227, #17191d)";

        return "-fx-background-color: " + background + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-radius: 18;"
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-border-width: 1.2;";
    }

    private String resolveEventImageSource(Event event) {
        if (event.getImage() != null && !event.getImage().isBlank()) {
            return event.getImage();
        }
        return localSelectionne != null ? localSelectionne.getImage() : null;
    }

    private String resolveMeteoLocation(Event event) {
        String eventLocation = event.getLocation();
        if (eventLocation != null && !eventLocation.isBlank()) {
            return eventLocation.trim();
        }
        return localSelectionne != null ? localSelectionne.getAddress() : null;
    }

    private int resolveLocalId() {
        return localSelectionne != null ? localSelectionne.getId() : 0;
    }

    private String formatDate(Event event) {
        if (event.getDate() == null) {
            return "Date non precisee";
        }
        return event.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String formatDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String countrySuffix(String country) {
        return country == null || country.isBlank() ? "" : ", " + country;
    }

    private String limitText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "Aucune description.";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
