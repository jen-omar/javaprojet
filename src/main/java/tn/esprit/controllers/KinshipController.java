package tn.esprit.controllers;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.text.Text;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.Models.Brief;
import tn.esprit.Models.Proposal;
import tn.esprit.services.BriefService;
import tn.esprit.services.ProposalService;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

public class KinshipController {

    // --- Services ---
    private final BriefService briefService = new BriefService();
    private final ProposalService proposalService = new ProposalService();

    // --- Session Info ---
    private String currentRole;
    private int currentUserId;

    // --- UI Layouts ---
    // --- Header & Content Areas ---
    @FXML private VBox mainDashboardHeader;
    @FXML private ScrollPane mainDashboardContent;
    @FXML private VBox briefDetailHeader;
    @FXML private ScrollPane briefDetailContent;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    
    @FXML private FlowPane briefCardsContainer;
    
    @FXML private FlowPane proposalCardsContainer;

    @FXML private StackPane briefModalOverlay;
    @FXML private StackPane proposalModalOverlay;
    @FXML private Button btnPostRequest;
    
    // --- Detail View ---
    @FXML private Label detailBriefTitle;
    @FXML private Label detailBriefAuthor;
    @FXML private Label detailBriefBudget;
    @FXML private Label detailBriefDeadline;
    @FXML private Label detailBriefStatus;
    @FXML private Text detailBriefDesc;
    @FXML private Button btnForgeProposalDetail;

    // --- Forms ---
    @FXML private TextField titleField;
    @FXML private TextArea descArea;
    @FXML private TextField budgetField;
    @FXML private DatePicker deadlinePicker;
    @FXML private Button btnSaveBrief;

    @FXML private TextField priceField;
    @FXML private TextField daysField;
    @FXML private TextArea coverArea;
    @FXML private Button btnSaveProp;

    // --- Data ---
    private ObservableList<Brief> briefData = FXCollections.observableArrayList();
    private ObservableList<Proposal> proposalData = FXCollections.observableArrayList();
    private FilteredList<Brief> filteredBriefs;
    private FilteredList<Proposal> filteredProposals;

    private Brief selectedBriefForEdit = null;
    private Brief activeViewingBrief = null;
    private Proposal selectedProposalForEdit = null;

    @FXML
    public void initialize() {
        tn.esprit.Models.User loggedUser = tn.esprit.util.UserSession.getInstance().getUser();
        if (loggedUser != null) {
            currentUserId = loggedUser.getId();
            currentRole = loggedUser.getPrimaryRole();
        } else {
            currentUserId = -1;
            currentRole = "ROLE_USER"; 
        }

        // Setup Filtering and Sorting
        filteredBriefs = new FilteredList<>(briefData, b -> true);
        filteredProposals = new FilteredList<>(proposalData, p -> true);
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters(newValue, sortCombo.getValue());
        });

        sortCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters(searchField.getText(), newValue);
        });

        // Initialize display
        switchRole(currentRole);
    }

    private void switchRole(String role) {
        currentRole = role;
        refreshData();
        
        btnPostRequest.setVisible("ROLE_CLIENT".equals(role) || "ROLE_AUTHOR".equals(role));
        btnPostRequest.setManaged(btnPostRequest.isVisible());
    }

    @FXML
    public void onRefresh() {
        refreshData();
    }

    private void refreshData() {
        briefCardsContainer.getChildren().clear();
        proposalCardsContainer.getChildren().clear();

        if ("ROLE_CLIENT".equals(currentRole)) {
            briefData.setAll(briefService.getByClient(currentUserId));
        } else if ("ROLE_AUTHOR".equals(currentRole)) {
            briefData.setAll(briefService.getByStatus("OPEN"));
            // Authors also view their proposal history initially
            proposalData.setAll(proposalService.getByArtistId(currentUserId));
            renderProposals();
        } else {
            briefData.setAll(briefService.getAll());
        }

        filterBriefs(searchField.getText(), sortCombo.getValue());
        applyFilters(searchField.getText(), sortCombo.getValue());
    }

    private void applyFilters(String query, String sortMode) {
        if (briefDetailContent != null && briefDetailContent.isVisible()) {
            filterProposals(query, sortMode);
        } else {
            filterBriefs(query, sortMode);
        }
    }

    private void filterProposals(String query, String sortMode) {
        // 1. Filter
        filteredProposals.setPredicate(p -> {
            if (query == null || query.isBlank()) return true;
            String low = query.toLowerCase();
            if (p.getArtistUsername() != null && p.getArtistUsername().toLowerCase().contains(low)) return true;
            if (p.getCoverLetter() != null && p.getCoverLetter().toLowerCase().contains(low)) return true;
            return false;
        });

        // 2. Sort
        SortedList<Proposal> sorted = new SortedList<>(filteredProposals);
        sorted.setComparator((p1, p2) -> {
            if ("Budget (High to Low)".equals(sortMode)) return Double.compare(p2.getPrice(), p1.getPrice());
            if ("Budget (Low to High)".equals(sortMode)) return Double.compare(p1.getPrice(), p2.getPrice());
            return p2.getSubmittedAt().compareTo(p1.getSubmittedAt()); // Default newest
        });

        // 3. Render
        proposalCardsContainer.getChildren().clear();
        for (Proposal p : sorted) {
            proposalCardsContainer.getChildren().add(createProposalCard(p));
        }
    }

    // --- Card Rendering ---

    private void filterBriefs(String query, String sortMode) {
        // 1. Filter
        filteredBriefs.setPredicate(brief -> {
            if (query == null || query.isBlank()) return true;
            String lowerCaseFilter = query.toLowerCase();
            if (brief.getTitle().toLowerCase().contains(lowerCaseFilter)) return true;
            if (brief.getDescription().toLowerCase().contains(lowerCaseFilter)) return true;
            return false;
        });

        // 2. Sort
        SortedList<Brief> sortedData = new SortedList<>(filteredBriefs);
        if (sortMode != null) {
            sortedData.setComparator((b1, b2) -> {
                switch (sortMode) {
                    case "Budget (High to Low)": return Double.compare(b2.getBudgetMax(), b1.getBudgetMax());
                    case "Budget (Low to High)": return Double.compare(b1.getBudgetMax(), b2.getBudgetMax());
                    case "Deadline (Urgent)": return b1.getDeadline().compareTo(b2.getDeadline());
                    case "Newest First":
                    default: return b2.getCreatedAt().compareTo(b1.getCreatedAt());
                }
            });
        }

        // 3. Render
        briefCardsContainer.getChildren().clear();
        for (Brief b : sortedData) {
            briefCardsContainer.getChildren().add(createBriefCard(b));
        }
    }

    private void renderProposals() {
        proposalCardsContainer.getChildren().clear();
        proposalCardsContainer.setVisible(true);
        proposalCardsContainer.setManaged(true);
        for (Proposal p : proposalData) {
            proposalCardsContainer.getChildren().add(createProposalCard(p));
        }
    }

    private VBox createBriefCard(Brief b) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card-container");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);
        card.setMinHeight(160);

        Label title = new Label(b.getTitle());
        title.setStyle("-fx-font-family: 'Cinzel', serif; -fx-font-size: 16; -fx-text-fill: -mythoria-gold;");

        Label statusBadge = new Label(b.getStatus());
        statusBadge.getStyleClass().add("pill-text");
        if ("OPEN".equals(b.getStatus())) statusBadge.setStyle("-fx-text-fill: #4cff4c; -fx-border-color: #4cff4c;");
        else if ("IN_PROGRESS".equals(b.getStatus())) statusBadge.setStyle("-fx-text-fill: #4caaff; -fx-border-color: #4caaff;");

        Label budget = new Label("Budget: " + b.getBudgetMax() + " Gold");
        budget.setStyle("-fx-text-fill: -mythoria-bone;");
        
        Label deadline = new Label("Deadline: " + (b.getDeadline() != null ? b.getDeadline().toLocalDate() : "None"));
        deadline.setStyle("-fx-text-fill: -mythoria-silver; -fx-font-size: 11;");

        Label creator = new Label("Request by: " + (b.getClientUsername() != null ? b.getClientUsername() : "User " + b.getClientId()));
        creator.setStyle("-fx-text-fill: -mythoria-silver; -fx-font-size: 10; -fx-font-style: italic;");

        HBox header = new HBox(10, title, new Region(), statusBadge);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        // Buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnView = new Button("View Quest");
        btnView.getStyleClass().add("nav-button");
        btnView.setOnAction(e -> onViewQuest(b));

        Button btnUpdate = new Button("Update");
        btnUpdate.getStyleClass().add("nav-button");
        btnUpdate.setOnAction(e -> openBriefModal(b));
        
        Button btnDelete = new Button("Delete");
        btnDelete.getStyleClass().add("nav-button");
        btnDelete.setStyle("-fx-text-fill: #ff4c4c;");
        btnDelete.setOnAction(e -> triggerDeleteBrief(b));

        boolean isAdmin = "ROLE_ADMIN".equals(currentRole);
        boolean isOwner = b.getClientId() == currentUserId;
        boolean isAuthor = "ROLE_AUTHOR".equals(currentRole);

        if (isAdmin || isOwner) {
            actions.getChildren().addAll(btnView, btnUpdate, btnDelete);
        } else if (isAuthor) {
            // Authors mostly view or add proposals
            actions.getChildren().add(btnView);
        }

        card.getChildren().addAll(header, budget, deadline, creator, new Region(), actions);
        VBox.setVgrow(card.getChildren().get(4), Priority.ALWAYS); // Spacer
        return card;
    }

    private VBox createProposalCard(Proposal p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card-container");
        card.setPadding(new Insets(15));
        card.setPrefWidth(280);
        card.setMinHeight(160);

        Label artist = new Label(p.getArtistUsername() != null ? p.getArtistUsername() : "Artist " + p.getArtistId());
        artist.setStyle("-fx-font-family: 'Cinzel', serif; -fx-font-size: 16; -fx-text-fill: -mythoria-gold;");

        Label stateBadge = new Label(p.isAccepted() ? "ACCEPTED" : "PENDING");
        stateBadge.getStyleClass().add("pill-text");
        if (p.isAccepted()) stateBadge.setStyle("-fx-text-fill: #4cff4c; -fx-border-color: #4cff4c;");
        else stateBadge.setStyle("-fx-text-fill: -mythoria-silver; -fx-border-color: -mythoria-silver;");

        Label info = new Label("Price: " + p.getPrice() + " Gold\nDays to Complete: " + p.getDaysToComplete());
        info.setStyle("-fx-text-fill: -mythoria-bone;");
        
        Label cover = new Label(p.getCoverLetter());
        cover.setStyle("-fx-text-fill: -mythoria-silver; -fx-font-size: 12;");
        cover.setWrapText(true);

        HBox header = new HBox(10, artist, new Region(), stateBadge);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        boolean isAdmin = "ROLE_ADMIN".equals(currentRole);
        boolean isOwnerAuthor = p.getArtistId() == currentUserId && "ROLE_AUTHOR".equals(currentRole);
        boolean isClientViewing = activeViewingBrief != null && activeViewingBrief.getClientId() == currentUserId;

        if (isOwnerAuthor || isAdmin) {
            Button btnUpdate = new Button("Update");
            btnUpdate.getStyleClass().add("nav-button");
            btnUpdate.setOnAction(e -> openProposalModal(p, activeViewingBrief));

            Button btnDelete = new Button("Delete");
            btnDelete.getStyleClass().add("nav-button");
            btnDelete.setStyle("-fx-text-fill: #ff4c4c;");
            btnDelete.setOnAction(e -> triggerDeleteProposal(p));
            
            actions.getChildren().addAll(btnUpdate, btnDelete);
        }

        if (isClientViewing && !p.isAccepted() && "OPEN".equals(activeViewingBrief.getStatus())) {
            Button btnAccept = new Button("Accept Offer");
            btnAccept.getStyleClass().add("create-button");
            btnAccept.setOnAction(e -> triggerAcceptProposal(p));
            actions.getChildren().add(btnAccept);
        }

        card.getChildren().addAll(header, info, cover, new Region(), actions);
        VBox.setVgrow(card.getChildren().get(3), Priority.ALWAYS);
        return card;
    }

    // --- State Handlers ---

    @FXML
    public void onCloseProposals() {
        briefDetailHeader.setVisible(false);
        briefDetailHeader.setManaged(false);
        briefDetailContent.setVisible(false);
        briefDetailContent.setManaged(false);

        mainDashboardHeader.setVisible(true);
        mainDashboardHeader.setManaged(true);
        mainDashboardContent.setVisible(true);
        mainDashboardContent.setManaged(true);

        activeViewingBrief = null;
        if ("ROLE_AUTHOR".equals(currentRole)) {
            // Restore Author's own proposals
            proposalData.setAll(proposalService.getByArtistId(currentUserId));
            renderProposals();
        }
    }

    private void onViewQuest(Brief b) {
        activeViewingBrief = b;
        
        // Switch Views
        mainDashboardHeader.setVisible(false);
        mainDashboardHeader.setManaged(false);
        mainDashboardContent.setVisible(false);
        mainDashboardContent.setManaged(false);

        briefDetailHeader.setVisible(true);
        briefDetailHeader.setManaged(true);
        briefDetailContent.setVisible(true);
        briefDetailContent.setManaged(true);

        // Populate header
        detailBriefTitle.setText(b.getTitle());
        detailBriefAuthor.setText("Quest giver: " + (b.getClientUsername() != null ? b.getClientUsername() : "ID #" + b.getClientId()));
        detailBriefBudget.setText("Bounty: " + b.getBudgetMax() + " Gold");
        detailBriefDeadline.setText("Ends: " + (b.getDeadline() != null ? b.getDeadline().toLocalDate() : "Eternal"));
        detailBriefStatus.setText(b.getStatus());
        detailBriefDesc.setText(b.getDescription());

        // Load proposals for this brief
        proposalData.setAll(proposalService.getByBriefId(b.getId()));
        renderProposals();

        // Author logic
        if ("ROLE_AUTHOR".equals(currentRole) && "OPEN".equals(b.getStatus())) {
            btnForgeProposalDetail.setVisible(true);
        } else {
            btnForgeProposalDetail.setVisible(false);
        }
        
        // Initial filter application for proposals
        applyFilters(searchField.getText(), sortCombo.getValue());
    }

    @FXML
    public void onForgeProposalInDetail() {
        if (activeViewingBrief != null) {
            openProposalModal(null, activeViewingBrief);
        }
    }

    // --- Modals ---
    @FXML
    public void onOpenPostModal() {
        selectedBriefForEdit = null;
        titleField.clear();
        descArea.clear();
        budgetField.clear();
        deadlinePicker.setValue(LocalDate.now().plusDays(15));
        
        briefModalOverlay.setVisible(true);
    }

    private void openBriefModal(Brief b) {
        selectedBriefForEdit = b;
        titleField.setText(b.getTitle());
        descArea.setText(b.getDescription());
        budgetField.setText(String.valueOf(b.getBudgetMax()));
        deadlinePicker.setValue(b.getDeadline() != null ? b.getDeadline().toLocalDate() : LocalDate.now());
        
        briefModalOverlay.setVisible(true);
    }

    private void openProposalModal(Proposal p, Brief targetBrief) {
        selectedProposalForEdit = p;
        activeViewingBrief = targetBrief;
        
        if (p == null) {
            priceField.clear();
            daysField.clear();
            coverArea.clear();
        } else {
            priceField.setText(String.valueOf(p.getPrice()));
            daysField.setText(String.valueOf(p.getDaysToComplete()));
            coverArea.setText(p.getCoverLetter());
        }
        
        proposalModalOverlay.setVisible(true);
    }

    @FXML
    public void onCloseModal() {
        briefModalOverlay.setVisible(false);
        proposalModalOverlay.setVisible(false);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Validation & Saving ---
    @FXML
    public void onSaveBrief() {
        try {
            String title = titleField.getText();
            String desc = descArea.getText();
            
            if (title.isBlank() || desc.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Title and Description cannot be empty.");
                return;
            }
            
            double budget = Double.parseDouble(budgetField.getText());
            if (budget <= 0) {
                showAlert(Alert.AlertType.ERROR, "Invalid Budget", "Budget must be greater than zero.");
                return;
            }

            LocalDate dl = deadlinePicker.getValue();
            if (dl == null || dl.isBefore(LocalDate.now())) {
                showAlert(Alert.AlertType.ERROR, "Invalid Deadline", "Deadline must be in the future.");
                return;
            }

            if (selectedBriefForEdit == null) {
                Brief b = new Brief(title, desc, budget, dl.atStartOfDay(), "OPEN", currentUserId, LocalDateTime.now());
                briefService.add(b);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Quest Posted!");
            } else {
                selectedBriefForEdit.setTitle(title);
                selectedBriefForEdit.setDescription(desc);
                selectedBriefForEdit.setBudgetMax(budget);
                selectedBriefForEdit.setDeadline(dl.atStartOfDay());
                briefService.update(selectedBriefForEdit);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Quest Updated!");
            }
            
            onCloseModal();
            refreshData();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Number", "Budget must be a valid number.");
        }
    }

    @FXML
    public void onSaveProposal() {
        try {
            if (activeViewingBrief == null) return;
            
            String cover = coverArea.getText();
            if (cover.isBlank()) {
                showAlert(Alert.AlertType.ERROR, "Invalid Input", "Cover Letter cannot be empty.");
                return;
            }

            double price = Double.parseDouble(priceField.getText());
            int days = Integer.parseInt(daysField.getText());
            
            if (price <= 0 || days <= 0) {
                showAlert(Alert.AlertType.ERROR, "Invalid Values", "Price and Days must be greater than zero.");
                return;
            }

            if (selectedProposalForEdit == null) {
                Proposal p = new Proposal(price, days, cover, LocalDateTime.now(), false, currentUserId, activeViewingBrief.getId());
                proposalService.add(p);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Proposal Sent!");
            } else {
                selectedProposalForEdit.setPrice(price);
                selectedProposalForEdit.setDaysToComplete(days);
                selectedProposalForEdit.setCoverLetter(cover);
                proposalService.update(selectedProposalForEdit);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Proposal Updated!");
            }
            
            onCloseModal();
            onViewQuest(activeViewingBrief); // refresh the proposals view
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Invalid Number", "Price and Days must be valid numbers.");
        }
    }

    // --- Deletion and Sub-actions ---
    private void triggerDeleteBrief(Brief b) {
        if (!"OPEN".equals(b.getStatus()) && !"ROLE_ADMIN".equals(currentRole)) {
            showAlert(Alert.AlertType.ERROR, "Action Denied", "Cannot delete a quest that is " + b.getStatus());
            return;
        }
        briefService.delete(b.getId());
        refreshData();
        if (activeViewingBrief != null && activeViewingBrief.getId() == b.getId()) {
            onCloseProposals();
        }
    }

    private void triggerDeleteProposal(Proposal p) {
        if (p.isAccepted() && !"ROLE_ADMIN".equals(currentRole)) {
            showAlert(Alert.AlertType.ERROR, "Action Denied", "Cannot delete an accepted proposal.");
            return;
        }
        proposalService.delete(p.getId());
        if (activeViewingBrief != null) {
            onViewQuest(activeViewingBrief);
        } else {
            refreshData();
        }
    }

    private void triggerAcceptProposal(Proposal p) {
        if (activeViewingBrief != null) {
            boolean success = briefService.acceptProposal(activeViewingBrief.getId(), p.getId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Pact Sealed", "Proposal has been accepted!");
                refreshData();
                onViewQuest(activeViewingBrief); // Reload to see accepted badge
            }
        }
    }

    // --- PDF Export ---
    @FXML
    public void onGenerateReport() {
        try {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Save Kinship Report");
            fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Document", "*.pdf"));
            fileChooser.setInitialFileName("Mythoria_Kinship_Report.pdf");
            
            java.io.File file = fileChooser.showSaveDialog(mainDashboardContent.getScene().getWindow());
            if (file == null) {
                return; // User canceled
            }

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.DARK_GRAY);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.BLACK);

            document.add(new Paragraph("Mythoria Kinship Report", titleFont));
            document.add(new Paragraph("Scope: " + (activeViewingBrief != null ? "Quest Details [" + activeViewingBrief.getTitle() + "]" : "Full Dashboard"), normalFont));
            document.add(new Paragraph("Generated by: " + currentRole, normalFont));
            document.add(new Paragraph("Date: " + LocalDateTime.now() + "\n\n", normalFont));

            if (activeViewingBrief != null) {
                document.add(new Paragraph("== SELECTED QUEST ==", headerFont));
                document.add(new Paragraph("Title: " + activeViewingBrief.getTitle(), normalFont));
                document.add(new Paragraph("Creator: " + activeViewingBrief.getClientUsername(), normalFont));
                document.add(new Paragraph("Budget: " + activeViewingBrief.getBudgetMax() + " Gold", normalFont));
                document.add(new Paragraph("Status: " + activeViewingBrief.getStatus(), normalFont));
                document.add(new Paragraph("Description:\n" + activeViewingBrief.getDescription() + "\n", normalFont));
            }

            document.add(new Paragraph("== Briefs Summary ==", headerFont));
            for (Brief b : briefData) {
                document.add(new Paragraph(b.getId() + " | " + b.getTitle() + " | Creator: " + b.getClientUsername() + " | Budget: " + b.getBudgetMax(), normalFont));
            }

            document.add(new Paragraph("\n== Proposals Summary ==", headerFont));
            for (Proposal p : proposalData) {
                document.add(new Paragraph(p.getId() + " | From: " + p.getArtistUsername() + " | Price: " + p.getPrice() + " | Accepted: " + p.isAccepted(), normalFont));
            }

            document.close();
            showAlert(Alert.AlertType.INFORMATION, "Report Generated", "Successfully saved to: " + file.getAbsolutePath());
            System.out.println("✅ PDF Export Successful: " + file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ PDF Export Error: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Export Failed", "Error creating PDF Report: " + e.getMessage());
        }
    }
}
