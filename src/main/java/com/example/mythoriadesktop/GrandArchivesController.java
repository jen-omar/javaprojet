package com.example.mythoriadesktop;

import com.example.mythoriadesktop.data.WorldRepository;
import com.example.mythoriadesktop.model.World;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;

public class GrandArchivesController {
    private static final String NAV_ACTIVE_STYLE = "nav-active";
    @FXML
    private Label rankLabel;

    @FXML
    private Label pcLabel;

    @FXML
    private ScrollPane archivesScroll;

    @FXML
    private FlowPane worldGrid;

    @FXML
    private StackPane formHost;

    @FXML
    private Button createButton;

    @FXML
    private StackPane contentStack;

    @FXML
    private Button navArchives;

    private final WorldRepository worldRepository = new WorldRepository();
    private FormController formController;

    private Node placeholderView;

    private Button activeNavButton;

    @FXML
    public void initialize() {
        rankLabel.setText("Journeyman");
        pcLabel.setText("100 PC");

        worldRepository.worlds().addListener((ListChangeListener<World>) c -> renderWorlds());
        renderWorlds();

        loadForm();

        setActiveNav(navArchives);
        showArchives();
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Object src = event.getSource();
        if (src instanceof Button btn) {
            setActiveNav(btn);
            String route = btn.getUserData() == null ? "" : btn.getUserData().toString();
            switch (route) {
                case "ARCHIVES" -> showArchives();
                case "HOME" -> showPlaceholder("Home");
                case "FELLOWSHIP" -> showPlaceholder("Fellowship");
                case "VENUES" -> showPlaceholder("Venues");
                case "HAPPENINGS" -> showPlaceholder("Happenings");
                case "KINSHIP" -> showPlaceholder("Kinship");
                case "BAZAAR" -> showPlaceholder("The Bazaar");
                case "PROFILE" -> showPlaceholder("Profile");
                default -> showPlaceholder(route.isBlank() ? "Page" : route);
            }
        }
    }

    @FXML
    private void onCreateWorld() {
        showFormCreate();
    }

    private void loadForm() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("world-form-view.fxml"));
            Node form = loader.load();
            formController = loader.getController();
            formController.init(worldRepository, this::showArchives);
            formHost.getChildren().setAll(form);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load world-form-view.fxml", ex);
        }
    }

    private void showFormCreate() {
        createButton.setVisible(false);
        createButton.setManaged(false);
        archivesScroll.setVisible(false);
        archivesScroll.setManaged(false);
        hidePlaceholder();
        formHost.setVisible(true);
        formHost.setManaged(true);
        formController.openCreate();
    }

    private void showFormEdit(World world) {
        createButton.setVisible(false);
        createButton.setManaged(false);
        archivesScroll.setVisible(false);
        archivesScroll.setManaged(false);
        hidePlaceholder();
        formHost.setVisible(true);
        formHost.setManaged(true);
        formController.openEdit(world);
    }

    private void showArchives() {
        setActiveNav(navArchives);
        formHost.setVisible(false);
        formHost.setManaged(false);
        hidePlaceholder();
        archivesScroll.setVisible(true);
        archivesScroll.setManaged(true);
        createButton.setVisible(true);
        createButton.setManaged(true);
    }

    private void showPlaceholder(String title) {
        formHost.setVisible(false);
        formHost.setManaged(false);
        archivesScroll.setVisible(false);
        archivesScroll.setManaged(false);
        createButton.setVisible(false);
        createButton.setManaged(false);

        if (placeholderView == null) {
            VBox box = new VBox(12);
            box.getStyleClass().add("form-panel");
            box.setMaxWidth(720);
            StackPane.setAlignment(box, Pos.CENTER);
            Label h = new Label();
            h.getStyleClass().add("form-title");
            Label p = new Label("Interface en cours de développement.");
            p.getStyleClass().add("world-desc");
            box.getChildren().addAll(h, p);
            placeholderView = box;
        }

        if (placeholderView instanceof VBox box && !box.getChildren().isEmpty() && box.getChildren().get(0) instanceof Label h) {
            h.setText(title);
        }

        if (!contentStack.getChildren().contains(placeholderView)) {
            contentStack.getChildren().add(0, placeholderView);
        }
        placeholderView.setVisible(true);
        placeholderView.setManaged(true);
    }

    private void hidePlaceholder() {
        if (placeholderView != null) {
            placeholderView.setVisible(false);
            placeholderView.setManaged(false);
        }
    }

    private void setActiveNav(Button btn) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove(NAV_ACTIVE_STYLE);
        }
        activeNavButton = btn;
        if (activeNavButton != null && !activeNavButton.getStyleClass().contains(NAV_ACTIVE_STYLE)) {
            activeNavButton.getStyleClass().add(NAV_ACTIVE_STYLE);
        }
    }

    private void renderWorlds() {
        worldGrid.getChildren().setAll(worldRepository.worlds().stream().map(this::createWorldCard).toList());
    }

    private Node createWorldCard(World world) {
        VBox card = new VBox();
        card.getStyleClass().add("world-card");
        card.setPrefWidth(320);

        Label title = new Label(Optional.ofNullable(world.title()).orElse(""));
        title.getStyleClass().add("world-title");
        title.setWrapText(true);

        String descText = Optional.ofNullable(world.description()).orElse("");
        Label desc = new Label(descText.isBlank() ? "Aucune description." : descText);
        desc.getStyleClass().add("world-desc");
        desc.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button enter = new Button("Enter World ➔");
        enter.getStyleClass().add("enter-button");
        enter.setMaxWidth(Double.MAX_VALUE);
        enter.setOnAction(e -> enterWorld(world));

        Button edit = new Button("Éditer");
        edit.setOnAction(e -> showFormEdit(world));

        Button delete = new Button("Supprimer");
        delete.setOnAction(e -> worldRepository.delete(world.id()));

        HBox actions = new HBox(10, edit, delete);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        card.getChildren().addAll(title, desc, spacer, enter, actions);
        installHoverAnimation(card);
        return card;
    }

    private void enterWorld(World world) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("world-detail-view.fxml"));
            Node detailView = loader.load();
            WorldDetailController controller = loader.getController();
            controller.init(world, worldRepository, this::showArchives);
            
            contentStack.getChildren().add(detailView);
            
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.millis(300), detailView);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
            
            archivesScroll.setVisible(false);
            archivesScroll.setManaged(false);
            createButton.setVisible(false);
            createButton.setManaged(false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void installHoverAnimation(Node node) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(140), node);
        scaleUp.setToX(1.02);
        scaleUp.setToY(1.02);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition liftUp = new TranslateTransition(Duration.millis(140), node);
        liftUp.setToY(-3);
        liftUp.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition hoverIn = new ParallelTransition(scaleUp, liftUp);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(140), node);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition liftDown = new TranslateTransition(Duration.millis(140), node);
        liftDown.setToY(0);
        liftDown.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition hoverOut = new ParallelTransition(scaleDown, liftDown);

        node.setOnMouseEntered(e -> {
            hoverOut.stop();
            hoverIn.playFromStart();
        });
        node.setOnMouseExited(e -> {
            hoverIn.stop();
            hoverOut.playFromStart();
        });
    }
}
