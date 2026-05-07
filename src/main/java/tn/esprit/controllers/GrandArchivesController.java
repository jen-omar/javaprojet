package tn.esprit.controllers;

import tn.esprit.data.WorldRepository;
import tn.esprit.Models.World;
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

    @FXML private VBox sidebarNav;

    @FXML private Button navArchives;
    @FXML private Button navFellowship;
    @FXML private Button navVenues;
    @FXML private Button navHappenings;
    @FXML private Button navKinship;
    @FXML private Button navBazaar;
    @FXML private Button navUsers;
    @FXML private Button navProfile;
    @FXML private Button navLogout;

    private final WorldRepository worldRepository = new WorldRepository();
    private FormController formController;

    private Node placeholderView;
    private Button activeNavButton;

    @FXML
    public void initialize() {
        tn.esprit.Models.User user = tn.esprit.util.UserSession.getInstance().getUser();
        String role = user != null ? user.role() : "user";

        rankLabel.setText(user != null ? user.username() : "Guest");
        pcLabel.setText(role);

        worldRepository.worlds().addListener((ListChangeListener<World>) c -> renderWorlds());
        renderWorlds();

        loadForm();
        applyRoleConstraints(role);

        showPlaceholder("Home Dashboard");
    }

    private void applyRoleConstraints(String role) {
        if (!"ROLE_ADMIN".equals(role)) {
            // Non-Admins don't see User Management
            if (navUsers != null) {
                navUsers.setVisible(false);
                navUsers.setManaged(false);
            }
        }
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
                case "KINSHIP" -> showKinship();
                case "BAZAAR" -> showPlaceholder("The Bazaar");
                case "USERS" -> showUsers();
                case "PROFILE" -> showPlaceholder("Profile");
                case "LOGOUT" -> handleLogout();
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

    private void clearDynamicViews() {
        contentStack.getChildren().removeIf(node ->
            node != archivesScroll &&
            node != formHost &&
            node != createButton &&
            node != placeholderView
        );
    }

    private void showUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-users-view.fxml"));
            Node usersView = loader.load();

            clearDynamicViews();
            archivesScroll.setVisible(false);
            archivesScroll.setManaged(false);
            createButton.setVisible(false);
            createButton.setManaged(false);
            hidePlaceholder();
            formHost.setVisible(false);
            formHost.setManaged(false);

            contentStack.getChildren().add(usersView);
        } catch (IOException ex) {
            ex.printStackTrace();
            showPlaceholder("Users Interface Missing");
        }
    }

    private void handleLogout() {
        tn.esprit.util.UserSession.getInstance().cleanUserSession();
        try {
            javafx.scene.Scene scene = contentStack.getScene();
            javafx.stage.Stage stage = (javafx.stage.Stage) scene.getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
            scene.setRoot(fxmlLoader.load());
            stage.setTitle("Mythoria - Login");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void showArchives() {
        if (navArchives != null) {
            setActiveNav(navArchives);
        }
        clearDynamicViews();
        formHost.setVisible(false);
        formHost.setManaged(false);
        hidePlaceholder();
        archivesScroll.setVisible(true);
        archivesScroll.setManaged(true);
        createButton.setVisible(true);
        createButton.setManaged(true);
    }

    private void showKinship() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("kinship-view.fxml"));
            Node kinshipView = loader.load();

            clearDynamicViews();
            archivesScroll.setVisible(false);
            archivesScroll.setManaged(false);
            createButton.setVisible(false);
            createButton.setManaged(false);
            hidePlaceholder();
            formHost.setVisible(false);
            formHost.setManaged(false);

            contentStack.getChildren().add(kinshipView);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void showPlaceholder(String title) {
        clearDynamicViews();
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

            clearDynamicViews();
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
