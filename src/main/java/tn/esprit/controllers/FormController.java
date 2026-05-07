package tn.esprit.controllers;

import tn.esprit.data.WorldRepository;
import tn.esprit.Models.World;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.Optional;
import java.util.UUID;

public class FormController {
    @FXML
    private Label formTitle;

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextArea loreArea;

    @FXML
    private Button deleteButton;

    @FXML
    private Button saveButton;

    private WorldRepository worldRepository;
    private Runnable onClose;
    private UUID editingWorldId;

    public void init(WorldRepository worldRepository, Runnable onClose) {
        this.worldRepository = worldRepository;
        this.onClose = onClose;
    }

    public void openCreate() {
        editingWorldId = null;
        formTitle.setText("Créer un Monde");
        saveButton.setText("Créer");
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
        titleField.setText("");
        descriptionArea.setText("");
        loreArea.setText("");
        titleField.requestFocus();
    }

    public void openEdit(World world) {
        if (world == null) {
            openCreate();
            return;
        }
        editingWorldId = world.id();
        formTitle.setText("Éditer un Monde");
        saveButton.setText("Enregistrer");
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);
        titleField.setText(Optional.ofNullable(world.title()).orElse(""));
        descriptionArea.setText(Optional.ofNullable(world.description()).orElse(""));
        loreArea.setText(Optional.ofNullable(world.loreSnapshot()).orElse(""));
        titleField.requestFocus();
    }

    @FXML
    private void onCancel() {
        if (onClose != null) {
            onClose.run();
        }
    }

    @FXML
    private void onSave() {
        requireRepo();
        String title = Optional.ofNullable(titleField.getText()).orElse("").trim();
        if (title.isBlank()) {
            titleField.requestFocus();
            return;
        }

        String description = Optional.ofNullable(descriptionArea.getText()).orElse("").trim();
        String lore = Optional.ofNullable(loreArea.getText()).orElse("").trim();

        if (editingWorldId == null) {
            worldRepository.create(title, description, lore);
        } else {
            worldRepository.update(editingWorldId, title, description, lore);
        }

        if (onClose != null) {
            onClose.run();
        }
    }

    @FXML
    private void onDelete() {
        requireRepo();
        if (editingWorldId != null) {
            worldRepository.delete(editingWorldId);
        }
        if (onClose != null) {
            onClose.run();
        }
    }

    private void requireRepo() {
        if (worldRepository == null) {
            throw new IllegalStateException("FormController not initialized");
        }
    }
}
