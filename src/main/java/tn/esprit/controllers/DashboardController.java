package tn.esprit.controllers;

import tn.esprit.data.UserRepository;
import tn.esprit.data.WalletRepository;
import tn.esprit.data.WorldRepository;
import tn.esprit.Models.World;
import tn.esprit.Models.User;
import tn.esprit.controllers.services.EmailNotificationService;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;

public class DashboardController {
    private static final String NAV_ACTIVE_STYLE = "nav-active";

    @FXML
    private Label rankLabel;

    @FXML
    private Label pcLabel;

    @FXML
    private StackPane contentStack;

    @FXML
    private StackPane profileHost;

    @FXML
    private StackPane adminHost;

    @FXML
    private StackPane kinshipHost;

    @FXML
    private Button navHome;

    @FXML
    private Button navProfile;

    @FXML
    private Button navWorlds;

    @FXML
    private Button navVenues;

    @FXML
    private Button navHappenings;

    @FXML
    private Button navKinship;

    @FXML
    private Button navBazaar;

    @FXML
    private Button navAdmin;

    @FXML
    private Button logoutButton;

    private final UserRepository userRepository = new UserRepository();
    private final WalletRepository walletRepository = new WalletRepository();
    private final WorldRepository worldRepository = new WorldRepository();

    private ProfileController profileController;
    private AdminController adminController;
    private KinshipController kinshipController;
    private FormController worldFormController;
    private EmailNotificationService emailNotificationService;
    private Runnable onLogout;
    private Node placeholderView;
    private Label placeholderTitle;
    private Label placeholderSubtitle;
    private Label placeholderNote;
    private Button activeNavButton;
    private User currentUser;
    private Node marketplaceView;
    private Node homeView;
    private Node venuesView;
    private Node happeningsView;
    private ScrollPane worldsView;
    private FlowPane worldGrid;
    private StackPane worldFormHost;
    private Button createWorldButton;
    private Node worldDetailView;

    @FXML
    public void initialize() {
        worldRepository.worlds().addListener((ListChangeListener<World>) change -> renderWorlds());
        setActiveNav(navHome);
        showHome();
    }

    public void init(User user, Runnable onLogout, EmailNotificationService emailNotificationService) {
        currentUser = user;
        this.onLogout = onLogout;
        this.emailNotificationService = emailNotificationService;
        if (adminController != null) {
            adminController.setEmailNotificationService(emailNotificationService);
        }
        applyCurrentUser();
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Object source = event.getSource();
        if (!(source instanceof Button button)) {
            return;
        }

        String route = button.getUserData() == null ? "" : button.getUserData().toString();
        switch (route) {
            case "HOME" -> {
                setActiveNav(navHome);
                showHome();
            }
            case "PROFILE" -> {
                setActiveNav(navProfile);
                showProfile();
            }
            case "WORLDS" -> {
                setActiveNav(navWorlds);
                showWorlds();
            }
            case "VENUES" -> {
                setActiveNav(navVenues);
                showVenues();
            }
            case "HAPPENINGS" -> {
                setActiveNav(navHappenings);
                showHappenings();
            }
            case "KINSHIP" -> {
                setActiveNav(navKinship);
                showKinship();
            }
            case "BAZAAR" -> {
                setActiveNav(navBazaar);
                showMarketplace();
            }
            case "ADMIN" -> {
                setActiveNav(navAdmin);
                showAdmin();
            }
            default -> {
                setActiveNav(navHome);
                showHome();
            }
        }
    }

    @FXML
    private void onLogout() {
        if (onLogout != null) {
            onLogout.run();
        }
    }

    private void loadProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("profile-view.fxml"));
            Node profileView = loader.load();
            profileController = loader.getController();
            profileController.init(userRepository, this::handleUserUpdated);
            profileHost.getChildren().setAll(profileView);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load profile-view.fxml", ex);
        }
    }

    private void loadAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("admin-view.fxml"));
            Node adminView = loader.load();
            adminController = loader.getController();
            adminController.init(userRepository, walletRepository);
            adminController.setEmailNotificationService(emailNotificationService);
            adminHost.getChildren().setAll(adminView);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load admin-view.fxml", ex);
        }
    }

    private void loadKinship() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("kinship-view.fxml"));
            Node kinshipView = loader.load();
            kinshipController = loader.getController();
            kinshipHost.getChildren().setAll(kinshipView);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load kinship-view.fxml", ex);
        }
    }

    private void showHome() {
        hidePlaceholder();
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        hideMergedFeatureViews();

        if (homeView == null) {
            javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(20);
            box.setPadding(new javafx.geometry.Insets(40));
            box.setAlignment(Pos.TOP_LEFT);

            Label title = new Label("Bienvenue sur Mythoria Dashboard");
            title.setStyle("-fx-text-fill: white; -fx-font-size: 32; -fx-font-weight: bold;");

            Label desc = new Label("Votre hub central pour gérer votre collection d'art, " +
                    "explorer de nouveaux mondes et interagir avec la communauté.\n\n" +
                    "✓ Accédez à vos commandes et enchères récentes.\n" +
                    "✓ Découvrez les œuvres d'art du Bazaar.\n" +
                    "✓ Discutez avec notre Assistant IA.");
            desc.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 16;");
            desc.setWrapText(true);

            try {
                // Use File to get absolute URI which works better than getResource for Media
                java.io.File file = new java.io.File("src/main/resources/videos/animation.mp4");
                Media media = new Media(file.toURI().toString());
                MediaPlayer mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.setAutoPlay(true);
                mediaPlayer.setOnError(() -> System.err.println("Media error: " + mediaPlayer.getError()));
                mediaPlayer.play(); // Explicit play to fix black screen
                
                MediaView mediaView = new MediaView(mediaPlayer);
                mediaView.setFitWidth(650);
                mediaView.setPreserveRatio(true);
                
                javafx.scene.layout.VBox videoContainer = new javafx.scene.layout.VBox(mediaView);
                videoContainer.setAlignment(Pos.CENTER);
                videoContainer.setPadding(new javafx.geometry.Insets(10));
                videoContainer.setStyle("-fx-border-color: #c9a84c; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-color: #1a1a1a; -fx-background-radius: 10;");
                videoContainer.setMaxWidth(670);
                
                box.getChildren().addAll(title, desc, videoContainer);
            } catch (Exception e) {
                System.err.println("Could not load home video: " + e.getMessage());
                box.getChildren().addAll(title, desc);
            }

            homeView = box;
            contentStack.getChildren().add(homeView);
        }
        
        homeView.setVisible(true);
        homeView.setManaged(true);
    }

    private void showProfile() {
        if (profileController == null) {
            try {
                loadProfile();
            } catch (Exception ex) {
                showPlaceholder("Profile", "Impossible d'ouvrir le profil.", describeError(ex));
                return;
            }
        }

        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        hidePlaceholder();
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        hideMergedFeatureViews();
        hideWorldFeatureViews();
        profileHost.setVisible(true);
        profileHost.setManaged(true);
        if (profileController != null) {
            profileController.setUser(currentUser);
        }
    }

    private void showAdmin() {
        if (currentUser == null || !currentUser.isAdmin()) {
            setActiveNav(navHome);
            showHome();
            return;
        }

        if (adminController == null) {
            try {
                loadAdmin();
                adminController.setEmailNotificationService(emailNotificationService);
            } catch (Exception ex) {
                showPlaceholder("Admin", "Impossible d'ouvrir l'administration.", describeError(ex));
                return;
            }
        }

        hidePlaceholder();
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(true);
        adminHost.setManaged(true);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        hideMergedFeatureViews();
        hideWorldFeatureViews();
        if (adminController != null) {
            adminController.setCurrentUser(currentUser);
        }
    }

    private void showKinship() {
        if (kinshipController == null) {
            try {
                loadKinship();
            } catch (Exception ex) {
                showPlaceholder("Kinship", "Impossible d'ouvrir Kinship.", describeError(ex));
                return;
            }
        }

        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        hidePlaceholder();
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(true);
            kinshipHost.setManaged(true);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        hideMergedFeatureViews();
        hideWorldFeatureViews();
    }

    private void showWorlds() {
        hideDashboardViews();
        ensureWorldsView();
        clearWorldDetail();
        hideWorldForm();
        worldsView.setVisible(true);
        worldsView.setManaged(true);
        createWorldButton.setVisible(true);
        createWorldButton.setManaged(true);
        renderWorlds();
    }

    private void ensureWorldsView() {
        if (worldsView != null) {
            return;
        }

        VBox container = new VBox(18);
        container.getStyleClass().add("archives-container");
        container.setPadding(new Insets(28, 32, 88, 32));

        HBox header = new HBox(14);
        header.getStyleClass().add("archives-header");
        header.setPadding(Insets.EMPTY);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBlock = new VBox(6);
        Label title = new Label("Worlds");
        title.getStyleClass().add("archives-title");
        Label subtitle = new Label("Create, edit, and enter your fantasy worlds.");
        subtitle.getStyleClass().add("world-desc");
        subtitle.setWrapText(true);
        titleBlock.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button newWorld = new Button("New World");
        newWorld.getStyleClass().add("enter-button");
        newWorld.setOnAction(e -> showWorldFormCreate());
        header.getChildren().addAll(titleBlock, spacer, newWorld);

        worldGrid = new FlowPane();
        worldGrid.getStyleClass().add("world-grid");
        worldGrid.setPrefWrapLength(980);

        container.getChildren().addAll(header, worldGrid);

        worldsView = new ScrollPane(container);
        worldsView.setFitToWidth(true);
        worldsView.setPannable(true);
        worldsView.getStyleClass().add("archives-scroll");
        worldsView.setVisible(false);
        worldsView.setManaged(false);

        worldFormHost = new StackPane();
        worldFormHost.setPadding(new Insets(26));
        worldFormHost.setVisible(false);
        worldFormHost.setManaged(false);

        createWorldButton = new Button("+");
        createWorldButton.getStyleClass().add("create-button");
        createWorldButton.setOnAction(e -> showWorldFormCreate());
        createWorldButton.setVisible(false);
        createWorldButton.setManaged(false);
        StackPane.setAlignment(createWorldButton, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(createWorldButton, new Insets(0, 30, 30, 0));

        contentStack.getChildren().addAll(worldsView, worldFormHost, createWorldButton);
    }

    private void loadWorldForm() {
        if (worldFormController != null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("world-form-view.fxml"));
            Node form = loader.load();
            worldFormController = loader.getController();
            worldFormController.init(worldRepository, this::showWorlds);
            worldFormHost.getChildren().setAll(form);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load world-form-view.fxml", ex);
        }
    }

    private void showWorldFormCreate() {
        hideDashboardViews();
        ensureWorldsView();
        loadWorldForm();
        clearWorldDetail();
        worldsView.setVisible(false);
        worldsView.setManaged(false);
        createWorldButton.setVisible(false);
        createWorldButton.setManaged(false);
        worldFormHost.setVisible(true);
        worldFormHost.setManaged(true);
        worldFormController.openCreate();
    }

    private void showWorldFormEdit(World world) {
        hideDashboardViews();
        ensureWorldsView();
        loadWorldForm();
        clearWorldDetail();
        worldsView.setVisible(false);
        worldsView.setManaged(false);
        createWorldButton.setVisible(false);
        createWorldButton.setManaged(false);
        worldFormHost.setVisible(true);
        worldFormHost.setManaged(true);
        worldFormController.openEdit(world);
    }

    private void hideWorldForm() {
        if (worldFormHost != null) {
            worldFormHost.setVisible(false);
            worldFormHost.setManaged(false);
        }
    }

    private void renderWorlds() {
        if (worldGrid == null) {
            return;
        }

        if (worldRepository.worlds().isEmpty()) {
            VBox empty = new VBox(10);
            empty.getStyleClass().add("form-panel");
            empty.setMaxWidth(560);
            Label title = new Label("No worlds yet");
            title.getStyleClass().add("form-title");
            Label body = new Label("Start a new archive to track lore, books, and story notes.");
            body.getStyleClass().add("world-desc");
            body.setWrapText(true);
            Button create = new Button("Create World");
            create.getStyleClass().add("enter-button");
            create.setOnAction(e -> showWorldFormCreate());
            empty.getChildren().addAll(title, body, create);
            worldGrid.getChildren().setAll(empty);
            return;
        }

        worldGrid.getChildren().setAll(worldRepository.worlds().stream().map(this::createWorldCard).toList());
    }

    private Node createWorldCard(World world) {
        VBox card = new VBox(10);
        card.getStyleClass().add("world-card");
        card.setPrefWidth(300);
        card.setMinHeight(210);

        Label title = new Label(Optional.ofNullable(world.title()).orElse("Untitled World"));
        title.getStyleClass().add("world-title");
        title.setWrapText(true);

        String descText = Optional.ofNullable(world.description()).orElse("");
        Label desc = new Label(descText.isBlank() ? "No description yet." : descText);
        desc.getStyleClass().add("world-desc");
        desc.setWrapText(true);
        desc.setMinHeight(54);

        String loreText = Optional.ofNullable(world.loreSnapshot()).orElse("");
        Label lore = new Label(loreText.isBlank() ? "Lore snapshot empty." : loreText);
        lore.getStyleClass().add("world-meta");
        lore.setWrapText(true);
        lore.setMaxHeight(44);

        int bookCount = world.books() == null ? 0 : world.books().size();
        Label count = new Label(bookCount + (bookCount == 1 ? " book" : " books"));
        count.getStyleClass().add("world-count");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button enter = new Button("Enter World");
        enter.getStyleClass().add("enter-button");
        enter.setMaxWidth(Double.MAX_VALUE);
        enter.setOnAction(e -> enterWorld(world));

        Button edit = new Button("Edit");
        edit.setOnAction(e -> showWorldFormEdit(world));

        Button delete = new Button("Delete");
        delete.setOnAction(e -> {
            worldRepository.delete(world.id());
            showWorlds();
        });

        HBox actions = new HBox(10, edit, delete);
        actions.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(title, desc, lore, count, spacer, enter, actions);
        installHoverAnimation(card);
        return card;
    }

    private void enterWorld(World world) {
        hideDashboardViews();
        ensureWorldsView();
        hideWorldForm();
        createWorldButton.setVisible(false);
        createWorldButton.setManaged(false);
        worldsView.setVisible(false);
        worldsView.setManaged(false);

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("world-detail-view.fxml"));
            worldDetailView = loader.load();
            WorldDetailController controller = loader.getController();
            controller.init(world, worldRepository, this::showWorlds);
            contentStack.getChildren().add(worldDetailView);
        } catch (IOException ex) {
            showPlaceholder("Worlds", "Impossible d'ouvrir ce monde.", describeError(ex));
        }
    }

    private void clearWorldDetail() {
        if (worldDetailView != null) {
            contentStack.getChildren().remove(worldDetailView);
            worldDetailView = null;
        }
    }

    private void showPlaceholder(String title, String subtitle, String note) {
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        hideMergedFeatureViews();
        hideWorldFeatureViews();
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }

        if (placeholderView == null) {
            VBox box = new VBox(12);
            box.getStyleClass().add("form-panel");
            box.setMaxWidth(780);
            box.setAlignment(Pos.CENTER_LEFT);

            placeholderTitle = new Label();
            placeholderTitle.getStyleClass().add("profile-title");

            placeholderSubtitle = new Label();
            placeholderSubtitle.getStyleClass().add("profile-subtitle");
            placeholderSubtitle.setWrapText(true);

            placeholderNote = new Label();
            placeholderNote.getStyleClass().add("world-desc");
            placeholderNote.setWrapText(true);

            box.getChildren().addAll(placeholderTitle, placeholderSubtitle, placeholderNote);
            placeholderView = box;
            contentStack.getChildren().add(0, placeholderView);
        }

        placeholderTitle.setText(title);
        placeholderSubtitle.setText(subtitle);
        placeholderNote.setText(note);
        placeholderView.setVisible(true);
        placeholderView.setManaged(true);
    }

    private void hidePlaceholder() {
        if (placeholderView != null) {
            placeholderView.setVisible(false);
            placeholderView.setManaged(false);
        }
    }

    private void showMarketplace() {
        hidePlaceholder();
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        hideMergedFeatureViews();
        hideWorldFeatureViews();
        
        if (marketplaceView == null) {
            com.marketplace.MainApp marketplaceApp = new com.marketplace.MainApp();
            marketplaceView = marketplaceApp.getMarketplaceView(() -> {
                setActiveNav(navHome);
                showHome();
            });
            contentStack.getChildren().add(marketplaceView);
        }
        marketplaceView.setVisible(true);
        marketplaceView.setManaged(true);
    }

    private void showVenues() {
        if (currentUser == null || !currentUser.isAdmin()) {
            setActiveNav(navHome);
            showHome();
            return;
        }

        hideDashboardViews();
        if (venuesView == null) {
            venuesView = loadMergedFeature("/tn/esprit/mythoria/GestionLocal.fxml", "GestionLocal.fxml");
            contentStack.getChildren().add(venuesView);
        }
        venuesView.setVisible(true);
        venuesView.setManaged(true);
    }

    private void showHappenings() {
        hideDashboardViews();
        if (happeningsView == null) {
            happeningsView = loadMergedFeature("/tn/esprit/mythoria/GestionLocalEvent.fxml", "GestionLocalEvent.fxml");
            contentStack.getChildren().add(happeningsView);
        }
        happeningsView.setVisible(true);
        happeningsView.setManaged(true);
    }

    private Node loadMergedFeature(String resourcePath, String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Node view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof tn.esprit.mythoria.controller.GestionLocalEventController eventsController) {
                eventsController.setEmbeddedNavigator(this::showEmbeddedHappeningsView);
            } else if (controller instanceof tn.esprit.mythoria.controller.FormEventController eventFormController) {
                eventFormController.setEmbeddedNavigator(this::showEmbeddedHappeningsView);
            } else if (controller instanceof tn.esprit.mythoria.controller.ListeEventsController eventListController) {
                eventListController.setEmbeddedNavigator(this::showEmbeddedHappeningsView);
            }
            return view;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load " + viewName, ex);
        }
    }

    private void showEmbeddedHappeningsView(Node view) {
        hideDashboardViews();
        if (happeningsView != null) {
            contentStack.getChildren().remove(happeningsView);
        }
        happeningsView = view;
        if (!contentStack.getChildren().contains(happeningsView)) {
            contentStack.getChildren().add(happeningsView);
        }
        happeningsView.setVisible(true);
        happeningsView.setManaged(true);
    }

    private void hideDashboardViews() {
        hidePlaceholder();
        if (homeView != null) {
            homeView.setVisible(false);
            homeView.setManaged(false);
        }
        profileHost.setVisible(false);
        profileHost.setManaged(false);
        adminHost.setVisible(false);
        adminHost.setManaged(false);
        if (kinshipHost != null) {
            kinshipHost.setVisible(false);
            kinshipHost.setManaged(false);
        }
        if (marketplaceView != null) {
            marketplaceView.setVisible(false);
            marketplaceView.setManaged(false);
        }
        hideMergedFeatureViews();
        hideWorldFeatureViews();
    }

    private void hideMergedFeatureViews() {
        if (venuesView != null) {
            venuesView.setVisible(false);
            venuesView.setManaged(false);
        }
        if (happeningsView != null) {
            happeningsView.setVisible(false);
            happeningsView.setManaged(false);
        }
    }

    private void hideWorldFeatureViews() {
        if (worldsView != null) {
            worldsView.setVisible(false);
            worldsView.setManaged(false);
        }
        if (worldFormHost != null) {
            worldFormHost.setVisible(false);
            worldFormHost.setManaged(false);
        }
        if (createWorldButton != null) {
            createWorldButton.setVisible(false);
            createWorldButton.setManaged(false);
        }
        clearWorldDetail();
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

    private void setActiveNav(Button button) {
        if (activeNavButton != null) {
            activeNavButton.getStyleClass().remove(NAV_ACTIVE_STYLE);
        }
        activeNavButton = button;
        if (activeNavButton != null && !activeNavButton.getStyleClass().contains(NAV_ACTIVE_STYLE)) {
            activeNavButton.getStyleClass().add(NAV_ACTIVE_STYLE);
        }
    }

    private void applyCurrentUser() {
        if (rankLabel == null || pcLabel == null) {
            return;
        }

        if (currentUser == null) {
            rankLabel.setText("Rank: Journeyman");
            pcLabel.setText("100 PC");
            navVenues.setVisible(false);
            navVenues.setManaged(false);
            navAdmin.setVisible(false);
            navAdmin.setManaged(false);
            return;
        }

        rankLabel.setText("Rank: " + currentUser.rank());
        pcLabel.setText(currentUser.points() + " PC");
        navVenues.setVisible(currentUser.isAdmin());
        navVenues.setManaged(currentUser.isAdmin());
        navAdmin.setVisible(currentUser.isAdmin());
        navAdmin.setManaged(currentUser.isAdmin());

        if (profileController != null) {
            profileController.setUser(currentUser);
        }
        if (adminController != null) {
            adminController.setCurrentUser(currentUser);
        }
    }

    private String describeError(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return cause.getClass().getSimpleName();
        }
        return message;
    }

    private void handleUserUpdated(User updatedUser) {
        currentUser = updatedUser;
        applyCurrentUser();
    }
}
