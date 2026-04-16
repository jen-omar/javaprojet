package tn.esprit.mythoria.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.mythoria.entity.Event;
import tn.esprit.mythoria.entity.Local;
import tn.esprit.mythoria.service.EventService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FormEventController {

    @FXML
    private Label lblLocal;

    @FXML
    private TextField tfTitle;

    @FXML
    private TextArea taDescription;

    @FXML
    private DatePicker dpDate;

    @FXML
    private TextField tfLocation;

    @FXML
    private TextField tfMaxTickets;

    @FXML
    private TextField tfMaxVipTickets;

    @FXML
    private TextField tfMaxNormalTickets;

    @FXML
    private Button btnSave;

    private Local localSelectionne;
    private Event eventActuel;
    private boolean modeModification = false;

    private EventService eventService = new EventService();

    public void setLocalSelectionne(Local local) {
        this.localSelectionne = local;
        lblLocal.setText("Local sélectionné : " + local.getName() + " - " + local.getAddress());
    }

    public void setModeAjout() {
        modeModification = false;
        eventActuel = null;
        if (btnSave != null) {
            btnSave.setText("Enregistrer");
        }
    }

    public void setModeModification(Event event) {
        modeModification = true;
        eventActuel = event;

        tfTitle.setText(event.getTitle());
        taDescription.setText(event.getDescription());
        dpDate.setValue(event.getDate().toLocalDate());
        tfLocation.setText(event.getLocation());
        tfMaxTickets.setText(String.valueOf(event.getMaxTickets()));
        tfMaxVipTickets.setText(String.valueOf(event.getMaxVipTickets()));
        tfMaxNormalTickets.setText(String.valueOf(event.getMaxNormalTickets()));

        if (btnSave != null) {
            btnSave.setText("Modifier");
        }
    }

    @FXML
    public void saveEvent() {
        if (!controleSaisie()) {
            return;
        }

        try {
            LocalDate dateChoisie = dpDate.getValue();
            int maxTickets = Integer.parseInt(tfMaxTickets.getText().trim());
            int maxVipTickets = Integer.parseInt(tfMaxVipTickets.getText().trim());
            int maxNormalTickets = Integer.parseInt(tfMaxNormalTickets.getText().trim());

            if (modeModification) {
                if (eventService.existeAutreEventPourLocalEtDate(localSelectionne.getId(), dateChoisie, eventActuel.getId())) {
                    showAlert(Alert.AlertType.WARNING, "Conflit",
                            "Impossible de modifier : il existe déjà un event dans ce local à cette date.");
                    return;
                }

                eventActuel.setTitle(tfTitle.getText().trim());
                eventActuel.setDescription(taDescription.getText().trim());
                eventActuel.setDate(dateChoisie.atStartOfDay());
                eventActuel.setLocation(tfLocation.getText().trim());
                eventActuel.setMaxTickets(maxTickets);
                eventActuel.setMaxVipTickets(maxVipTickets);
                eventActuel.setMaxNormalTickets(maxNormalTickets);
                eventActuel.setLocalId(localSelectionne.getId());

                eventService.modifier(eventActuel);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Event modifié avec succès.");

            } else {
                if (eventService.existeEventPourLocalEtDate(localSelectionne.getId(), dateChoisie)) {
                    showAlert(Alert.AlertType.WARNING, "Conflit",
                            "Impossible d'ajouter un événement au même local le même jour.");
                    return;
                }

                Event event = new Event();
                event.setTitle(tfTitle.getText().trim());
                event.setDescription(taDescription.getText().trim());
                event.setDate(dateChoisie.atStartOfDay());
                event.setLocation(tfLocation.getText().trim());
                event.setCreatedAt(LocalDateTime.now());
                event.setMaxTickets(maxTickets);
                event.setMaxVipTickets(maxVipTickets);
                event.setMaxNormalTickets(maxNormalTickets);
                event.setCreatorId(1);
                event.setLocalId(localSelectionne.getId());

                eventService.ajouter(event);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Event ajouté avec succès.");
            }

            retourListe();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur SQL", e.getMessage());
        }
    }

    private boolean controleSaisie() {
        String erreurs = "";

        if (tfTitle.getText() == null || tfTitle.getText().trim().isEmpty()) {
            erreurs += "- Le titre est obligatoire.\n";
        }

        if (taDescription.getText() == null || taDescription.getText().trim().isEmpty()) {
            erreurs += "- La description est obligatoire.\n";
        }

        if (dpDate.getValue() == null) {
            erreurs += "- La date est obligatoire.\n";
        }

        if (tfLocation.getText() == null || tfLocation.getText().trim().isEmpty()) {
            erreurs += "- Le lieu est obligatoire.\n";
        }

        if (tfMaxTickets.getText() == null || tfMaxTickets.getText().trim().isEmpty()) {
            erreurs += "- Le nombre max de tickets est obligatoire.\n";
        } else {
            try {
                Integer.parseInt(tfMaxTickets.getText().trim());
            } catch (NumberFormatException e) {
                erreurs += "- Le nombre max de tickets doit être un entier valide.\n";
            }
        }

        if (tfMaxVipTickets.getText() == null || tfMaxVipTickets.getText().trim().isEmpty()) {
            erreurs += "- Le nombre max de tickets VIP est obligatoire.\n";
        } else {
            try {
                Integer.parseInt(tfMaxVipTickets.getText().trim());
            } catch (NumberFormatException e) {
                erreurs += "- Le nombre max de tickets VIP doit être un entier valide.\n";
            }
        }

        if (tfMaxNormalTickets.getText() == null || tfMaxNormalTickets.getText().trim().isEmpty()) {
            erreurs += "- Le nombre max de tickets normaux est obligatoire.\n";
        } else {
            try {
                Integer.parseInt(tfMaxNormalTickets.getText().trim());
            } catch (NumberFormatException e) {
                erreurs += "- Le nombre max de tickets normaux doit être un entier valide.\n";
            }
        }

        if (erreurs.isEmpty()) {
            int maxTickets = Integer.parseInt(tfMaxTickets.getText().trim());
            int maxVipTickets = Integer.parseInt(tfMaxVipTickets.getText().trim());
            int maxNormalTickets = Integer.parseInt(tfMaxNormalTickets.getText().trim());

            if (maxTickets <= 0) {
                erreurs += "- Le nombre max de tickets doit être supérieur à 0.\n";
            }

            if (maxVipTickets < 0) {
                erreurs += "- Le nombre max de tickets VIP ne peut pas être négatif.\n";
            }

            if (maxNormalTickets < 0) {
                erreurs += "- Le nombre max de tickets normaux ne peut pas être négatif.\n";
            }

            if (maxVipTickets + maxNormalTickets > maxTickets) {
                erreurs += "- La somme des tickets VIP et normaux ne doit pas dépasser le total.\n";
            }
        }

        if (!erreurs.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Erreurs de saisie", erreurs);
            return false;
        }

        return true;
    }

    @FXML
    public void retourListe() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/mythoria/GestionLocalEvent.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) tfTitle.getScene().getWindow();
            stage.setTitle("Gestion des Events par Local");
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à la liste : " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}