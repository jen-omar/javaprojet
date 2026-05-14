package tn.esprit.controllers;

import tn.esprit.data.WorldRepository;
import tn.esprit.Models.Book;
import tn.esprit.Models.World;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class WorldDetailController {
    @FXML
    private Label worldTitle;
    @FXML
    private Label worldLore;
    @FXML
    private FlowPane booksGrid;

    private World world;
    private WorldRepository repository;
    private Runnable onBack;

    public void init(World world, WorldRepository repository, Runnable onBack) {
        this.world = world;
        this.repository = repository;
        this.onBack = onBack;

        worldTitle.setText(world.title());
        worldLore.setText(Optional.ofNullable(world.loreSnapshot()).orElse("No lore available."));

        renderBooks();
    }

    @FXML
    private void onBack() {
        if (onBack != null) {
            onBack.run();
        }
    }

    @FXML
    private void onAddBook() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Book");
        dialog.setHeaderText("Create a new book for " + world.title());
        dialog.setContentText("Book Title:");

        dialog.showAndWait().ifPresent(title -> {
            if (!title.isBlank()) {
                Book newBook = Book.createNew(world.id(), title, "");
                repository.addBookToWorld(world.id(), newBook);
                // Reload world from repository to get updated list
                this.world = repository.getWorld(world.id()).orElse(world);
                renderBooks();
            }
        });
    }

    private void renderBooks() {
        booksGrid.getChildren().clear();
        if (world.books() == null || world.books().isEmpty()) {
            VBox empty = new VBox(8);
            empty.getStyleClass().add("form-panel");
            empty.setMaxWidth(520);

            Label title = new Label("No books in this world yet");
            title.getStyleClass().add("form-title");

            Label body = new Label("Use New Book to begin building stories inside this setting.");
            body.getStyleClass().add("world-desc");
            body.setWrapText(true);

            empty.getChildren().addAll(title, body);
            booksGrid.getChildren().add(empty);
            return;
        }

        for (Book book : world.books()) {
            booksGrid.getChildren().add(createBookCard(book));
        }
    }

    private VBox createBookCard(Book book) {
        VBox card = new VBox(8);
        card.getStyleClass().add("book-card");
        card.setPrefWidth(160);
        card.setPrefHeight(220);

        Label title = new Label(book.title());
        title.getStyleClass().add("book-title");
        title.setWrapText(true);

        card.getChildren().add(title);
        return card;
    }
}
