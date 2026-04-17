package com.example.mythoriadesktop;

import com.example.mythoriadesktop.data.UserRepository;
import com.example.mythoriadesktop.data.WalletRepository;
import com.example.mythoriadesktop.model.User;
import com.example.mythoriadesktop.model.Wallet;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminController {
    @FXML private Label adminHeadline;
    @FXML private Label adminSummary;
    @FXML private Label adminMessage;

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userIdColumn;
    @FXML private TableColumn<User, String> userUsernameColumn;
    @FXML private TableColumn<User, String> userEmailColumn;
    @FXML private TableColumn<User, String> userFirstNameColumn;
    @FXML private TableColumn<User, String> userLastNameColumn;
    @FXML private TableColumn<User, String> userPhoneColumn;
    @FXML private TableColumn<User, String> userRoleColumn;
    @FXML private TableColumn<User, Number> userScoreColumn;
    @FXML private TextField userSearchField;
    @FXML private ComboBox<String> userSortComboBox;
    @FXML private TextField adminUserEmailField;
    @FXML private TextField adminUserPhoneField;
    @FXML private TextField adminUserFirstNameField;
    @FXML private TextField adminUserLastNameField;
    @FXML private TextField adminUserRoleField;
    @FXML private TextField adminUserScoreField;

    @FXML private TableView<Wallet> adminWalletTable;
    @FXML private TableColumn<Wallet, Number> adminWalletIdColumn;
    @FXML private TableColumn<Wallet, String> adminWalletUpdatedAtColumn;
    @FXML private TableColumn<Wallet, Number> adminWalletUserIdColumn;
    @FXML private TableColumn<Wallet, Number> adminWalletBalanceColumn;
    @FXML private TableColumn<Wallet, String> adminWalletStatusColumn;
    @FXML private TableColumn<Wallet, String> adminWalletCurrencyColumn;
    @FXML private TableColumn<Wallet, Number> adminWalletCeilingColumn;
    @FXML private TextField walletSearchField;
    @FXML private ComboBox<String> walletSortComboBox;
    @FXML private TextField adminWalletUserIdField;
    @FXML private TextField adminWalletBalanceField;
    @FXML private TextField adminWalletStatusField;
    @FXML private TextField adminWalletCurrencyField;
    @FXML private TextField adminWalletCeilingField;

    private final ObservableList<User> masterUsers = FXCollections.observableArrayList();
    private final ObservableList<Wallet> masterWallets = FXCollections.observableArrayList();
    private final AdminExportService exportService = new AdminExportService();

    private UserRepository userRepository;
    private WalletRepository walletRepository;
    private User currentUser;
    private FilteredList<User> filteredUsers;
    private SortedList<User> sortedUsers;
    private FilteredList<Wallet> filteredWallets;
    private SortedList<Wallet> sortedWallets;

    public void init(UserRepository userRepository, WalletRepository walletRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        configureUserTable();
        configureWalletTable();
        configureUserFiltering();
        configureWalletFiltering();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        renderHeader();
        refreshAll();
    }

    @FXML
    private void onUpdateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Selectionne un user a modifier.", true);
            return;
        }

        try {
            userRepository.adminUpdateUser(
                    selected.id(),
                    adminUserEmailField.getText(),
                    adminUserFirstNameField.getText(),
                    adminUserLastNameField.getText(),
                    adminUserPhoneField.getText(),
                    parseInteger(adminUserScoreField.getText(), "score"),
                    adminUserRoleField.getText()
            ).orElseThrow();
            refreshUsers();
            showMessage("User modifie.", false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Impossible de modifier le user.", ex), true);
        }
    }

    @FXML
    private void onDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Selectionne un user a supprimer.", true);
            return;
        }
        if (currentUser != null && currentUser.id().equals(selected.id())) {
            showMessage("Impossible de supprimer le compte connecte.", true);
            return;
        }
        if (userRepository.adminDeleteUser(selected.id())) {
            refreshUsers();
            clearUserForm();
            showMessage("User supprime.", false);
            return;
        }
        showMessage("Impossible de supprimer le user.", true);
    }

    @FXML
    private void onUpdateWallet() {
        Wallet selected = adminWalletTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Selectionne un wallet a modifier.", true);
            return;
        }

        try {
            walletRepository.update(
                    selected.id(),
                    parseInteger(adminWalletUserIdField.getText(), "user id"),
                    parseAmount(adminWalletBalanceField.getText(), "solde"),
                    adminWalletStatusField.getText(),
                    adminWalletCurrencyField.getText(),
                    parseAmount(adminWalletCeilingField.getText(), "plafond")
            ).orElseThrow();
            refreshWallets();
            showMessage("Wallet modifie.", false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Impossible de modifier le wallet.", ex), true);
        }
    }

    @FXML
    private void onDeleteWallet() {
        Wallet selected = adminWalletTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Selectionne un wallet a supprimer.", true);
            return;
        }
        if (walletRepository.delete(selected.id())) {
            refreshWallets();
            clearWalletForm();
            showMessage("Wallet supprime.", false);
            return;
        }
        showMessage("Impossible de supprimer le wallet.", true);
    }

    @FXML
    private void onClearUserForm() {
        clearUserForm();
        userTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onClearWalletForm() {
        clearWalletForm();
        adminWalletTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void onExportUsersPdf() {
        File target = chooseExportTarget("PDF files", "*.pdf", "admin-users", ".pdf");
        if (target == null) {
            return;
        }
        try {
            exportService.exportUsersPdf(List.copyOf(userTable.getItems()), target.toPath());
            showMessage("Export PDF users cree.", false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Impossible d'exporter les users.", ex), true);
        }
    }

    @FXML
    private void onExportUsersExcel() {
        File target = chooseExportTarget("Excel files", "*.xlsx", "admin-users", ".xlsx");
        if (target == null) {
            return;
        }
        try {
            exportService.exportUsersExcel(List.copyOf(userTable.getItems()), target.toPath());
            showMessage("Export Excel users cree.", false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Impossible d'exporter les users.", ex), true);
        }
    }

    @FXML
    private void onExportWalletsPdf() {
        File target = chooseExportTarget("PDF files", "*.pdf", "admin-wallets", ".pdf");
        if (target == null) {
            return;
        }
        try {
            exportService.exportWalletsPdf(List.copyOf(adminWalletTable.getItems()), target.toPath());
            showMessage("Export PDF wallets cree.", false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Impossible d'exporter les wallets.", ex), true);
        }
    }

    @FXML
    private void onExportWalletsExcel() {
        File target = chooseExportTarget("Excel files", "*.xlsx", "admin-wallets", ".xlsx");
        if (target == null) {
            return;
        }
        try {
            exportService.exportWalletsExcel(List.copyOf(adminWalletTable.getItems()), target.toPath());
            showMessage("Export Excel wallets cree.", false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Impossible d'exporter les wallets.", ex), true);
        }
    }

    private void configureUserTable() {
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userIdColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().id()));
        userUsernameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().username()));
        userEmailColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().email()));
        userFirstNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().firstName()));
        userLastNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().lastName()));
        userPhoneColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().phoneNumber()));
        userRoleColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().role()));
        userScoreColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().points()));
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateUserForm(newValue));
    }

    private void configureWalletTable() {
        adminWalletTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        adminWalletIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        adminWalletUpdatedAtColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().updatedAt()));
        adminWalletUserIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().userId()));
        adminWalletBalanceColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().balance()));
        adminWalletStatusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status()));
        adminWalletCurrencyColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().currency()));
        adminWalletCeilingColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ceiling()));
        adminWalletTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> populateWalletForm(newValue));
    }

    private void configureUserFiltering() {
        filteredUsers = new FilteredList<>(masterUsers, user -> true);
        sortedUsers = new SortedList<>(filteredUsers);
        userTable.setItems(sortedUsers);
        userSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyUserFilter(newValue));
        userSortComboBox.setItems(FXCollections.observableArrayList("Plus recent", "Username A-Z", "Role A-Z", "Score decroissant"));
        userSortComboBox.setValue("Plus recent");
        userSortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyUserSort(newValue));
        applyUserSort("Plus recent");
    }

    private void configureWalletFiltering() {
        filteredWallets = new FilteredList<>(masterWallets, wallet -> true);
        sortedWallets = new SortedList<>(filteredWallets);
        adminWalletTable.setItems(sortedWallets);
        walletSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyWalletFilter(newValue));
        walletSortComboBox.setItems(FXCollections.observableArrayList("Plus recent", "Solde decroissant", "Solde croissant", "Statut A-Z"));
        walletSortComboBox.setValue("Plus recent");
        walletSortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyWalletSort(newValue));
        applyWalletSort("Plus recent");
    }

    private void applyUserFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredUsers.setPredicate(user -> q.isBlank()
                || user.id().toLowerCase(Locale.ROOT).contains(q)
                || user.username().toLowerCase(Locale.ROOT).contains(q)
                || user.email().toLowerCase(Locale.ROOT).contains(q)
                || user.firstName().toLowerCase(Locale.ROOT).contains(q)
                || user.lastName().toLowerCase(Locale.ROOT).contains(q)
                || user.phoneNumber().toLowerCase(Locale.ROOT).contains(q)
                || user.role().toLowerCase(Locale.ROOT).contains(q)
                || String.valueOf(user.points()).contains(q));
    }

    private void applyWalletFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredWallets.setPredicate(wallet -> q.isBlank()
                || String.valueOf(wallet.id()).contains(q)
                || wallet.updatedAt().toLowerCase(Locale.ROOT).contains(q)
                || String.valueOf(wallet.userId()).contains(q)
                || String.valueOf(wallet.balance()).contains(q)
                || wallet.status().toLowerCase(Locale.ROOT).contains(q)
                || wallet.currency().toLowerCase(Locale.ROOT).contains(q)
                || String.valueOf(wallet.ceiling()).contains(q));
    }

    private void applyUserSort(String sort) {
        Comparator<User> comparator = switch (sort == null ? "Plus recent" : sort) {
            case "Username A-Z" -> Comparator.comparing(User::username, String.CASE_INSENSITIVE_ORDER);
            case "Role A-Z" -> Comparator.comparing(User::role, String.CASE_INSENSITIVE_ORDER);
            case "Score decroissant" -> Comparator.comparingInt(User::points).reversed();
            default -> Comparator.comparingInt((User user) -> Integer.parseInt(user.id())).reversed();
        };
        sortedUsers.setComparator(comparator);
    }

    private void applyWalletSort(String sort) {
        Comparator<Wallet> comparator = switch (sort == null ? "Plus recent" : sort) {
            case "Solde decroissant" -> Comparator.comparingDouble(Wallet::balance).reversed();
            case "Solde croissant" -> Comparator.comparingDouble(Wallet::balance);
            case "Statut A-Z" -> Comparator.comparing(Wallet::status, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Wallet::updatedAt, String.CASE_INSENSITIVE_ORDER).reversed()
                    .thenComparing(Comparator.comparingInt(Wallet::id).reversed());
        };
        sortedWallets.setComparator(comparator);
    }

    private void populateUserForm(User user) {
        if (user == null) {
            return;
        }
        adminUserEmailField.setText(user.email());
        adminUserPhoneField.setText(user.phoneNumber());
        adminUserFirstNameField.setText(user.firstName());
        adminUserLastNameField.setText(user.lastName());
        adminUserRoleField.setText(user.role());
        adminUserScoreField.setText(String.valueOf(user.points()));
    }

    private void populateWalletForm(Wallet wallet) {
        if (wallet == null) {
            return;
        }
        adminWalletUserIdField.setText(String.valueOf(wallet.userId()));
        adminWalletBalanceField.setText(String.valueOf(wallet.balance()));
        adminWalletStatusField.setText(wallet.status());
        adminWalletCurrencyField.setText(wallet.currency());
        adminWalletCeilingField.setText(String.valueOf(wallet.ceiling()));
    }

    private void clearUserForm() {
        adminUserEmailField.setText("");
        adminUserPhoneField.setText("");
        adminUserFirstNameField.setText("");
        adminUserLastNameField.setText("");
        adminUserRoleField.setText("user");
        adminUserScoreField.setText("0");
    }

    private void clearWalletForm() {
        adminWalletUserIdField.setText("");
        adminWalletBalanceField.setText("0");
        adminWalletStatusField.setText("actif");
        adminWalletCurrencyField.setText("TND");
        adminWalletCeilingField.setText("0");
    }

    private void refreshAll() {
        refreshUsers();
        refreshWallets();
    }

    private void refreshUsers() {
        masterUsers.setAll(userRepository.findAllUsers());
        applyUserFilter(userSearchField.getText());
        applyUserSort(userSortComboBox.getValue());
    }

    private void refreshWallets() {
        masterWallets.setAll(walletRepository.findAll());
        applyWalletFilter(walletSearchField.getText());
        applyWalletSort(walletSortComboBox.getValue());
    }

    private void renderHeader() {
        if (currentUser == null) {
            adminHeadline.setText("Admin Console");
            adminSummary.setText("Access reserved to admins.");
            return;
        }
        adminHeadline.setText("Admin Console");
        adminSummary.setText("Connected as @" + currentUser.username() + " (" + currentUser.role() + ")");
    }

    private int parseInteger(String value, String field) {
        try {
            return Integer.parseInt((value == null || value.isBlank()) ? "0" : value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valeur invalide pour " + field + ".");
        }
    }

    private double parseAmount(String value, String field) {
        try {
            String normalized = (value == null || value.isBlank()) ? "0" : value.trim().replace(',', '.');
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valeur invalide pour " + field + ".");
        }
    }

    private void showMessage(String message, boolean error) {
        adminMessage.setText(message);
        adminMessage.getStyleClass().removeAll("login-error", "login-success");
        if (!message.isBlank()) {
            adminMessage.getStyleClass().add(error ? "login-error" : "login-success");
        }
    }

    private File chooseExportTarget(String description, String extensionPattern, String prefix, String extension) {
        Window window = userTable == null || userTable.getScene() == null ? null : userTable.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save admin export");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensionPattern));
        chooser.setInitialFileName(prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + extension);
        File downloadsDir = Path.of(System.getProperty("user.home"), "Downloads").toFile();
        if (downloadsDir.exists()) {
            chooser.setInitialDirectory(downloadsDir);
        }
        return chooser.showSaveDialog(window);
    }

    private static String resolveErrorMessage(String fallback, Exception ex) {
        String detail = ex.getMessage();
        return detail == null || detail.isBlank() ? fallback : fallback + " " + detail;
    }
}
