package tn.esprit.mythoria.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.LocalService;
import tn.esprit.mythoria.utils.LocalImageUtil;
import tn.esprit.mythoria.utils.MapApiUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GestionLocalEventController {
    private static final double CARD_WIDTH = 270;
    private static final double IMAGE_WIDTH = 250;
    private static final double IMAGE_HEIGHT = 140;

    @FXML
    private TilePane localsTilePane;

    @FXML
    private TextField txtSearch;

    @FXML
    private ComboBox<String> cbSort;

    private final LocalService localService = new LocalService();
    private ObservableList<Local> currentLocals = FXCollections.observableArrayList();
    private Consumer<Node> embeddedNavigator;

    public void setEmbeddedNavigator(Consumer<Node> embeddedNavigator) {
        this.embeddedNavigator = embeddedNavigator;
    }

    @FXML
    public void initialize() {
        cbSort.setItems(FXCollections.observableArrayList(
                "Name",
                "Description",
                "Address",
                "Price",
                "Capacity",
                "Status"
        ));
        cbSort.getSelectionModel().selectFirst();
        localsTilePane.setPrefColumns(3);

        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> chargerLocaux());
        cbSort.valueProperty().addListener((observable, oldValue, newValue) -> chargerLocaux());
        chargerLocaux();
    }

    private void chargerLocaux() {
        try {
            String searchText = txtSearch != null ? txtSearch.getText() : "";
            String sortOption = cbSort != null ? cbSort.getValue() : null;
            ObservableList<Local> list = FXCollections.observableArrayList(
                    localService.rechercherEtTrierLocaux(searchText, sortOption)
            );
            currentLocals = list;

            localsTilePane.getChildren().clear();
            if (list.isEmpty()) {
                localsTilePane.getChildren().add(buildEmptyState());
                return;
            }

            for (Local local : list) {
                localsTilePane.getChildren().add(buildLocalCard(local));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les locaux : " + e.getMessage());
        }
    }

    @FXML
    public void openLocalsMap(ActionEvent actionEvent) {
        if (currentLocals == null || currentLocals.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Maps", "Aucun local a afficher sur la carte.");
            return;
        }

        Button mapsButton = actionEvent != null && actionEvent.getSource() instanceof Button button ? button : null;
        if (mapsButton != null) {
            mapsButton.setDisable(true);
            mapsButton.setText("Loading...");
        }

        List<Local> localsSnapshot = new ArrayList<>(currentLocals);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return MapApiUtil.buildLocalsMapHtml(localsSnapshot);
            }
        };

        task.setOnSucceeded(event -> {
            if (mapsButton != null) {
                mapsButton.setDisable(false);
                mapsButton.setText("Maps");
            }
            showLocalsMapWindow(task.getValue());
        });

        task.setOnFailed(event -> {
            if (mapsButton != null) {
                mapsButton.setDisable(false);
                mapsButton.setText("Maps");
            }
            Throwable exception = task.getException();
            String message = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Une erreur inattendue est survenue.";
            showAlert(Alert.AlertType.ERROR, "Maps", "Impossible de charger la carte : " + message);
        });

        Thread thread = new Thread(task, "locals-map-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void showLocalsMapWindow(String html) {
        WebView webView = new WebView();
        webView.setPrefSize(980, 640);
        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                refreshLeafletMap(webView);
            }
        });

        Stage mapStage = new Stage();
        mapStage.setTitle("Carte des locaux");
        mapStage.setScene(new Scene(webView, 980, 640));
        webView.widthProperty().addListener((observable, oldValue, newValue) -> refreshLeafletMap(webView));
        webView.heightProperty().addListener((observable, oldValue, newValue) -> refreshLeafletMap(webView));
        webView.getEngine().loadContent(html);
        mapStage.show();
        refreshLeafletMap(webView);
    }

    private void refreshLeafletMap(WebView webView) {
        try {
            webView.getEngine().executeScript(
                    "setTimeout(function(){ if (window.refreshMapSize) window.refreshMapSize(); }, 100);"
            );
        } catch (Exception ignored) {
        }
    }

    private void gotoAjouterEvent(Local localSelectionne) {
        if (localSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez selectionner un local.");
            return;
        }

        if (isLocalUnderMaintenance(localSelectionne)) {
            showAlert(Alert.AlertType.WARNING, "Attention",
                    "Impossible d'ajouter un event : ce local est en maintenance.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/FormEvent.fxml"));
            Node view = loader.load();

            FormEventController controller = loader.getController();
            controller.setModeAjout();
            controller.setLocalSelectionne(localSelectionne);
            controller.setEmbeddedNavigator(embeddedNavigator);

            if (embeddedNavigator != null) {
                embeddedNavigator.accept(view);
            } else {
                Scene scene = new Scene((javafx.scene.Parent) view);
                Stage stage = (Stage) localsTilePane.getScene().getWindow();
                stage.setTitle("Ajouter Event");
                stage.setScene(scene);
                stage.show();
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le formulaire event : " + e.getMessage());
        }
    }

    private void gotoEvents(Local localSelectionne) {
        if (localSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez selectionner un local.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/ListeEvents.fxml"));
            Node view = loader.load();

            ListeEventsController controller = loader.getController();
            controller.setLocalSelectionne(localSelectionne);
            controller.setEmbeddedNavigator(embeddedNavigator);

            if (embeddedNavigator != null) {
                embeddedNavigator.accept(view);
            } else {
                Scene scene = new Scene((javafx.scene.Parent) view);
                Stage stage = (Stage) localsTilePane.getScene().getWindow();
                stage.setTitle("Liste des Events");
                stage.setScene(scene);
                stage.show();
            }

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la liste des events : " + e.getMessage());
        }
    }

    private VBox buildLocalCard(Local local) {
        StackPane imageContainer = new StackPane();
        imageContainer.setMinSize(IMAGE_WIDTH, IMAGE_HEIGHT);
        imageContainer.setPrefSize(IMAGE_WIDTH, IMAGE_HEIGHT);
        imageContainer.setMaxSize(IMAGE_WIDTH, IMAGE_HEIGHT);
        imageContainer.setStyle(
                "-fx-background-color: #111214;"
                        + "-fx-border-color: #5a5a5a;"
                        + "-fx-border-radius: 16;"
                        + "-fx-background-radius: 16;"
        );

        ImageView imageView = new ImageView();
        imageView.setFitWidth(242);
        imageView.setFitHeight(132);
        imageView.setPreserveRatio(true);
        Image image = LocalImageUtil.loadImage(local.getImage(), 242, 132);

        Label imageFallback = new Label(image == null ? "Pas d'image" : "");
        imageFallback.setStyle("-fx-text-fill: #8f9399; -fx-font-size: 12px; -fx-font-weight: bold;");

        imageView.setImage(image);
        imageContainer.getChildren().addAll(imageView, imageFallback);

        Label nameLabel = new Label(fallback(local.getName(), "Local sans nom"));
        nameLabel.setWrapText(true);
        nameLabel.setMinHeight(44);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");

        Label addressLabel = new Label(fallback(local.getAddress(), "Adresse non precisee"));
        addressLabel.setWrapText(true);
        addressLabel.setStyle("-fx-text-fill: #d7dce3; -fx-font-size: 12px;");

        Label descriptionLabel = new Label(limitText(local.getDescription(), 74));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMinHeight(48);
        descriptionLabel.setStyle("-fx-text-fill: #bcc1c8; -fx-font-size: 12px;");

        HBox tagsRow = new HBox(
                8,
                buildTag("Prix " + local.getPrice() + " DT", "#233245", "#dcecff"),
                buildTag(local.getCapacity() + " places", "#26382d", "#d8f5df")
        );
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        HBox statusRow = new HBox(
                buildTag(fallback(local.getStatus(), "Statut N/A"), statusBackground(local.getStatus()), "#ffffff")
        );
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Button addEventButton = buildActionButton("Add Event", true);
        addEventButton.setOnAction(event -> gotoAjouterEvent(local));

        Button viewEventsButton = buildActionButton("Voir Event", false);
        viewEventsButton.setOnAction(event -> gotoEvents(local));

        HBox actions = new HBox(10, addEventButton, viewEventsButton);
        actions.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, imageContainer, nameLabel, addressLabel, descriptionLabel, tagsRow, statusRow, actions);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(14));
        card.setPrefWidth(CARD_WIDTH);
        card.setMinWidth(CARD_WIDTH);
        card.setMaxWidth(CARD_WIDTH);
        card.setMinHeight(380);
        card.setStyle(buildCardStyle());

        return card;
    }

    private Label buildEmptyState() {
        Label emptyState = new Label("Aucun local trouve.");
        emptyState.setMinWidth(760);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setStyle(
                "-fx-text-fill: #c9ced6;"
                        + "-fx-font-size: 15px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-padding: 80 0 80 0;"
        );
        return emptyState;
    }

    private Button buildActionButton(String text, boolean primary) {
        Button button = new Button(text);
        button.setPrefHeight(36);
        button.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(button, Priority.ALWAYS);
        button.setStyle(primary
                ? "-fx-background-color: linear-gradient(to bottom, #c0c0c0, #8e8e8e);"
                + "-fx-text-fill: black;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-cursor: hand;"
                : "-fx-background-color: linear-gradient(to bottom, #9d9d9d, #6e6e6e);"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 10;"
                + "-fx-cursor: hand;");
        return button;
    }

    private Label buildTag(String text, String backgroundColor, String textColor) {
        Label tag = new Label(text);
        tag.setMinHeight(28);
        tag.setStyle(
                "-fx-background-color: " + backgroundColor + ";"
                        + "-fx-text-fill: " + textColor + ";"
                        + "-fx-background-radius: 999;"
                        + "-fx-padding: 6 10 6 10;"
                        + "-fx-font-size: 10px;"
                        + "-fx-font-weight: bold;"
        );
        return tag;
    }

    private String buildCardStyle() {
        return "-fx-background-color: linear-gradient(to bottom, #202227, #17191d);"
                + "-fx-background-radius: 18;"
                + "-fx-border-radius: 18;"
                + "-fx-border-color: #4f545b;"
                + "-fx-border-width: 1.2;";
    }

    private String statusBackground(String status) {
        if ("DISPONIBLE".equalsIgnoreCase(status)) {
            return "#2f7d4f";
        }
        if ("EN_MAINTENANCE".equalsIgnoreCase(status)) {
            return "#8a5c1b";
        }
        return "#6b2b36";
    }

    private boolean isLocalUnderMaintenance(Local local) {
        return local != null && "EN_MAINTENANCE".equalsIgnoreCase(local.getStatus());
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
