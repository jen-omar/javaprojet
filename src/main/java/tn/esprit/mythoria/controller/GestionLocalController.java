package tn.esprit.mythoria.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.LocalService;
import tn.esprit.mythoria.utils.LocalImageUtil;
import tn.esprit.mythoria.utils.LocalExcelExporter;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GestionLocalController {

    @FXML
    private ListView<Local> lvlocals;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cbSort;
    LocalService localService = new LocalService();

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
        lvlocals.setCellFactory(listView -> createLocalCell());
        lvlocals.setPlaceholder(new Label("Aucun local trouve."));

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
            lvlocals.setItems(list);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les locaux : " + e.getMessage());
        }
    }

    @FXML
    public void gotoAjouter(ActionEvent event) {
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
    public void gotoModifier(ActionEvent event) {
        Local localSelectionne = lvlocals.getSelectionModel().getSelectedItem();

        if (localSelectionne == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez selectionner un local a modifier.");
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
    public void supprimerLocal(ActionEvent event) {
        Local selectedLocal = lvlocals.getSelectionModel().getSelectedItem();
        if (selectedLocal == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez selectionner un local a supprimer.");
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
                showAlert(Alert.AlertType.INFORMATION, "Succes", "Local supprime avec succes.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    @FXML
    public void exportLocauxExcel(ActionEvent event) {
        try {
            List<Local> locals = localService.afficher();
            if (locals.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Attention", "Aucun local a exporter.");
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter les locaux en Excel");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx")
            );
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            fileChooser.setInitialFileName("locals_export_" + timestamp + ".xlsx");

            Stage stage = (Stage) lvlocals.getScene().getWindow();
            File selectedFile = fileChooser.showSaveDialog(stage);

            if (selectedFile == null) {
                return;
            }

            LocalExcelExporter.exportToExcel(locals, selectedFile);

            showAlert(Alert.AlertType.INFORMATION, "Succes", "Export termine : " + selectedFile.getAbsolutePath());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de recuperer les locaux : " + e.getMessage());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'export Excel : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private ListCell<Local> createLocalCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Local local, boolean empty) {
                super.updateItem(local, empty);

                if (empty || local == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(null);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setStyle("-fx-background-color: transparent; -fx-padding: 6;");
                setGraphic(buildLocalCard(local, isSelected()));
            }
        };
    }

    private HBox buildLocalCard(Local local, boolean selected) {
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
        Image image = LocalImageUtil.loadImage(local.getImage(), 110, 86);

        Label imageFallback = new Label(image == null ? "Pas d'image" : "");
        imageFallback.setStyle("-fx-text-fill: #8f9399; -fx-font-size: 12px; -fx-font-weight: bold;");

        imageView.setImage(image);
        imageContainer.getChildren().addAll(imageView, imageFallback);

        Label nameLabel = new Label(local.getName());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(limitText(local.getDescription(), 100));
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-text-fill: #bcc1c8; -fx-font-size: 12px;");

        Label addressLabel = new Label(local.getAddress());
        addressLabel.setStyle("-fx-text-fill: #d7dce3; -fx-font-size: 13px;");

        HBox tagsRow = new HBox(
                10,
                buildTag("Prix " + local.getPrice() + " DT", "#233245", "#dcecff"),
                buildTag("Capacite " + local.getCapacity(), "#26382d", "#d8f5df"),
                buildTag(local.getStatus(), statusBackground(local.getStatus()), "#ffffff")
        );
        tagsRow.setAlignment(Pos.CENTER_LEFT);

        VBox textBox = new VBox(8, nameLabel, addressLabel, descriptionLabel, tagsRow);
        textBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox card = new HBox(16, imageContainer, textBox);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14));
        card.setStyle(buildCardStyle(selected));

        return card;
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

    private String statusBackground(String status) {
        if ("DISPONIBLE".equalsIgnoreCase(status)) {
            return "#2f7d4f";
        }
        if ("EN_MAINTENANCE".equalsIgnoreCase(status)) {
            return "#8a5c1b";
        }
        return "#6b2b36";
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
}
