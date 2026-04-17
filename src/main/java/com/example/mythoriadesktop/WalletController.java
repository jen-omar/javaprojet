package com.example.mythoriadesktop;

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

public class WalletController {
    private static final String SORT_RECENT = "Plus recent";
    private static final String SORT_BALANCE_ASC = "Solde croissant";
    private static final String SORT_BALANCE_DESC = "Solde decroissant";
    private static final String SORT_CEILING_ASC = "Plafond croissant";
    private static final String SORT_CEILING_DESC = "Plafond decroissant";
    private static final String SORT_STATUS = "Statut A-Z";

    @FXML
    private Label walletHeadline;

    @FXML
    private Label walletSummary;

    @FXML
    private Label walletMessage;

    @FXML
    private TableView<Wallet> walletTable;

    @FXML
    private TableColumn<Wallet, Number> idColumn;

    @FXML
    private TableColumn<Wallet, String> updatedAtColumn;

    @FXML
    private TableColumn<Wallet, Number> balanceColumn;

    @FXML
    private TableColumn<Wallet, String> statusColumn;

    @FXML
    private TableColumn<Wallet, String> currencyColumn;

    @FXML
    private TableColumn<Wallet, Number> ceilingColumn;

    @FXML
    private TextField balanceField;

    @FXML
    private TextField statusField;

    @FXML
    private TextField currencyField;

    @FXML
    private TextField ceilingField;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortComboBox;

    private final ObservableList<Wallet> masterWallets = FXCollections.observableArrayList();
    private final WalletExportService walletExportService = new WalletExportService();

    private WalletRepository walletRepository;
    private Runnable onBack;
    private User currentUser;
    private FilteredList<Wallet> filteredWallets;
    private SortedList<Wallet> sortedWallets;

    public void init(WalletRepository walletRepository, Runnable onBack) {
        this.walletRepository = walletRepository;
        this.onBack = onBack;
        configureTable();
        configureFilteringAndSorting();
    }

    public void setUser(User user) {
        currentUser = user;
        renderHeader();
        refreshWallets();
        clearForm();
    }

    @FXML
    private void onCreateWallet() {
        if (!canManageWallets()) {
            return;
        }

        try {
            int userId = ValidationUtils.requirePositiveInt(currentUser.id(), "user id");
            double balance = ValidationUtils.requireNonNegativeAmount(balanceField.getText(), "solde");
            String status = ValidationUtils.requireStatus(statusField.getText());
            String currency = ValidationUtils.requireCurrency(currencyField.getText());
            double ceiling = ValidationUtils.requireNonNegativeAmount(ceilingField.getText(), "plafond");
            ValidationUtils.validateWalletAmounts(balance, ceiling);
            walletRepository.create(
                    userId,
                    balance,
                    status,
                    currency,
                    ceiling
            ).orElseThrow();

            refreshWallets();
            clearForm();
            showMessage("Wallet created successfully.", false);
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Unable to create the wallet.", ex), true);
        }
    }

    @FXML
    private void onUpdateWallet() {
        if (!canManageWallets()) {
            return;
        }

        Wallet selectedWallet = walletTable.getSelectionModel().getSelectedItem();
        if (selectedWallet == null) {
            showMessage("Select a wallet to update.", true);
            return;
        }

        try {
            int userId = ValidationUtils.requirePositiveInt(currentUser.id(), "user id");
            double balance = ValidationUtils.requireNonNegativeAmount(balanceField.getText(), "solde");
            String status = ValidationUtils.requireStatus(statusField.getText());
            String currency = ValidationUtils.requireCurrency(currencyField.getText());
            double ceiling = ValidationUtils.requireNonNegativeAmount(ceilingField.getText(), "plafond");
            ValidationUtils.validateWalletAmounts(balance, ceiling);
            walletRepository.update(
                    selectedWallet.id(),
                    userId,
                    balance,
                    status,
                    currency,
                    ceiling
            ).orElseThrow();

            refreshWallets();
            selectWalletById(selectedWallet.id());
            showMessage("Wallet updated successfully.", false);
        } catch (IllegalArgumentException ex) {
            showMessage(ex.getMessage(), true);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Unable to update the wallet.", ex), true);
        }
    }

    @FXML
    private void onDeleteWallet() {
        if (!canManageWallets()) {
            return;
        }

        Wallet selectedWallet = walletTable.getSelectionModel().getSelectedItem();
        if (selectedWallet == null) {
            showMessage("Select a wallet to delete.", true);
            return;
        }

        if (walletRepository.delete(selectedWallet.id())) {
            refreshWallets();
            clearForm();
            showMessage("Wallet deleted successfully.", false);
            return;
        }

        showMessage("Unable to delete the wallet.", true);
    }

    @FXML
    private void onRefreshWallets() {
        if (!canManageWallets()) {
            return;
        }

        refreshWallets();
        showMessage("Wallet list refreshed.", false);
    }

    @FXML
    private void onExportPdf() {
        if (!canManageWallets()) {
            return;
        }

        List<Wallet> wallets = visibleWallets();
        if (wallets.isEmpty()) {
            showMessage("No wallet rows to export.", true);
            return;
        }

        File target = chooseExportTarget("PDF files", "*.pdf", "wallet-export", ".pdf");
        if (target == null) {
            return;
        }

        try {
            walletExportService.exportPdf(wallets, target.toPath());
            showMessage("PDF exported to " + target.getAbsolutePath(), false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Unable to export PDF.", ex), true);
        }
    }

    @FXML
    private void onExportExcel() {
        if (!canManageWallets()) {
            return;
        }

        List<Wallet> wallets = visibleWallets();
        if (wallets.isEmpty()) {
            showMessage("No wallet rows to export.", true);
            return;
        }

        File target = chooseExportTarget("Excel files", "*.xlsx", "wallet-export", ".xlsx");
        if (target == null) {
            return;
        }

        try {
            walletExportService.exportExcel(wallets, target.toPath());
            showMessage("Excel exported to " + target.getAbsolutePath(), false);
        } catch (Exception ex) {
            showMessage(resolveErrorMessage("Unable to export Excel.", ex), true);
        }
    }

    @FXML
    private void onClearWalletForm() {
        clearForm();
        walletTable.getSelectionModel().clearSelection();
        showMessage("", false);
    }

    @FXML
    private void onBackToProfile() {
        if (onBack != null) {
            onBack.run();
        }
    }

    private void configureTable() {
        if (walletTable == null) {
            return;
        }

        walletTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().id()));
        updatedAtColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().updatedAt()));
        balanceColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().balance()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().status()));
        currencyColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().currency()));
        ceilingColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().ceiling()));
        walletTable.getSelectionModel().selectedItemProperty().addListener((obs, oldWallet, newWallet) -> populateForm(newWallet));
    }

    private void configureFilteringAndSorting() {
        if (walletTable == null) {
            return;
        }

        filteredWallets = new FilteredList<>(masterWallets, wallet -> true);
        sortedWallets = new SortedList<>(filteredWallets);
        walletTable.setItems(sortedWallets);

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter(newValue));
        }

        if (sortComboBox != null) {
            sortComboBox.setItems(FXCollections.observableArrayList(
                    SORT_RECENT,
                    SORT_BALANCE_ASC,
                    SORT_BALANCE_DESC,
                    SORT_CEILING_ASC,
                    SORT_CEILING_DESC,
                    SORT_STATUS
            ));
            sortComboBox.setValue(SORT_RECENT);
            sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applySort(newValue));
            applySort(SORT_RECENT);
        }
    }

    private void populateForm(Wallet wallet) {
        if (wallet == null) {
            return;
        }

        balanceField.setText(formatNumber(wallet.balance()));
        statusField.setText(wallet.status());
        currencyField.setText(wallet.currency());
        ceilingField.setText(formatNumber(wallet.ceiling()));
    }

    private void refreshWallets() {
        if (walletTable == null) {
            return;
        }

        if (currentUser == null || !currentUser.databaseBacked()) {
            masterWallets.setAll(List.of());
            return;
        }

        List<Wallet> wallets = walletRepository.findByUserId(Integer.parseInt(currentUser.id()));
        masterWallets.setAll(wallets);
        applyFilter(searchField == null ? "" : searchField.getText());
        applySort(sortComboBox == null ? SORT_RECENT : sortComboBox.getValue());
    }

    private void renderHeader() {
        if (walletHeadline == null) {
            return;
        }

        if (currentUser == null) {
            walletHeadline.setText("Wallet");
            walletSummary.setText("No user is connected.");
            return;
        }

        walletHeadline.setText(currentUser.displayName().isBlank()
                ? currentUser.username() + "'s wallet"
                : currentUser.displayName() + " wallet");
        if (currentUser.databaseBacked()) {
            walletSummary.setText("Manage the portefeuille records linked to this user.");
        } else {
            walletSummary.setText("Wallet CRUD is available only for MySQL-backed users.");
        }
    }

    private void applyFilter(String query) {
        if (filteredWallets == null) {
            return;
        }

        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        filteredWallets.setPredicate(wallet -> {
            if (normalizedQuery.isBlank()) {
                return true;
            }

            return String.valueOf(wallet.id()).contains(normalizedQuery)
                    || wallet.updatedAt().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || String.valueOf(wallet.userId()).contains(normalizedQuery)
                    || formatNumber(wallet.balance()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || wallet.status().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || wallet.currency().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || formatNumber(wallet.ceiling()).toLowerCase(Locale.ROOT).contains(normalizedQuery);
        });
    }

    private void applySort(String selectedSort) {
        if (sortedWallets == null) {
            return;
        }

        Comparator<Wallet> comparator = switch (selectedSort == null ? SORT_RECENT : selectedSort) {
            case SORT_BALANCE_ASC -> Comparator.comparingDouble(Wallet::balance);
            case SORT_BALANCE_DESC -> Comparator.comparingDouble(Wallet::balance).reversed();
            case SORT_CEILING_ASC -> Comparator.comparingDouble(Wallet::ceiling);
            case SORT_CEILING_DESC -> Comparator.comparingDouble(Wallet::ceiling).reversed();
            case SORT_STATUS -> Comparator.comparing(Wallet::status, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Wallet::updatedAt, String.CASE_INSENSITIVE_ORDER).reversed()
                    .thenComparing(Comparator.comparingInt(Wallet::id).reversed());
        };
        sortedWallets.setComparator(comparator);
    }

    private boolean canManageWallets() {
        if (walletRepository == null) {
            throw new IllegalStateException("WalletController not initialized");
        }

        if (currentUser == null) {
            showMessage("No profile is loaded.", true);
            return false;
        }

        if (!currentUser.databaseBacked()) {
            showMessage("Wallet CRUD is available only for MySQL-backed users.", true);
            return false;
        }

        return true;
    }

    private void clearForm() {
        if (balanceField == null) {
            return;
        }

        balanceField.setText("0");
        statusField.setText("actif");
        currencyField.setText("TND");
        ceilingField.setText("0");
    }

    private void selectWalletById(int walletId) {
        if (walletTable == null) {
            return;
        }

        walletTable.getItems().stream()
                .filter(wallet -> wallet.id() == walletId)
                .findFirst()
                .ifPresent(wallet -> walletTable.getSelectionModel().select(wallet));
    }

    private void showMessage(String message, boolean error) {
        walletMessage.setText(message);
        walletMessage.getStyleClass().removeAll("login-error", "login-success");
        if (!message.isBlank()) {
            walletMessage.getStyleClass().add(error ? "login-error" : "login-success");
        }
    }

    private List<Wallet> visibleWallets() {
        return List.copyOf(walletTable.getItems());
    }

    private File chooseExportTarget(String description, String extensionPattern, String filePrefix, String extension) {
        Window window = walletTable == null || walletTable.getScene() == null ? null : walletTable.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save wallet export");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extensionPattern));
        chooser.setInitialFileName(filePrefix + "-" + timestampForFileName() + extension);

        File downloadsDir = defaultDownloadsDirectory().toFile();
        if (downloadsDir.exists()) {
            chooser.setInitialDirectory(downloadsDir);
        }

        return chooser.showSaveDialog(window);
    }

    private Path defaultDownloadsDirectory() {
        return Path.of(System.getProperty("user.home"), "Downloads");
    }

    private String timestampForFileName() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.format("%.0f", value);
        }
        return String.format("%.2f", value);
    }

    private static String resolveErrorMessage(String fallback, Exception ex) {
        String detail = ex.getMessage();
        if (detail == null || detail.isBlank()) {
            return fallback;
        }
        return fallback + " " + detail;
    }
}
