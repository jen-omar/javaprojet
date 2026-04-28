package com.marketplace.controllers;

import com.marketplace.models.Product;
import com.marketplace.services.*;
import com.marketplace.util.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HomeController {

        private final ProductService productService = new ProductService();
        private final OrderService orderService = new OrderService();
        private final BidService bidService = new BidService();
        private final ReviewService reviewService = new ReviewService();
        private final WishlistService wishlistService = new WishlistService();
        private final SessionManager session = SessionManager.getInstance();

        public Node buildView() {
                VBox root = new VBox(32);
                root.setStyle("-fx-background-color: #1a1a1a;");
                root.setPadding(new Insets(36, 40, 40, 40));

                if (session.isArtist()) {
                        buildArtistDashboard(root);
                } else {
                        buildAdminDashboard(root);
                }

                ScrollPane scroll = new ScrollPane(root);
                scroll.setFitToWidth(true);
                scroll.setStyle("-fx-background: #1a1a1a; -fx-background-color: #1a1a1a; -fx-border-color: transparent;");
                return scroll;
        }

        // ── ADMIN dashboard ───────────────────────────────────────────
        private void buildAdminDashboard(VBox root) {
                Label welcome = new Label("TABLEAU DE BORD");
                welcome.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 26; -fx-font-weight: bold;");
                Label sub = new Label("Vue globale du Marketplace — Administrateur");
                sub.setStyle("-fx-text-fill: #666; -fx-font-size: 13;");
                root.getChildren().addAll(new VBox(4, welcome, sub));

                int nProducts = productService.getAll().size();
                int nOrders = orderService.getAll().size();
                int nBids = bidService.getAll().size();
                int nReviews = reviewService.getAll().size();
                int nWishlist = wishlistService.getAll().size();

                // Revenue (sum of all order totals)
                BigDecimal revenue = orderService.getAll().stream()
                                .map((com.marketplace.models.Order o) -> o.getPrice() != null ? o.getPrice()
                                                : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                HBox stats = new HBox(14);
                stats.getChildren().addAll(
                                statCard("Produits", String.valueOf(nProducts), "≡", "#c0c0c0"),
                                statCard("Commandes", String.valueOf(nOrders), "◉", "#7ec8e3"),
                                statCard("Enchères", String.valueOf(nBids), "⚡", "#e3c07e"),
                                statCard("Avis", String.valueOf(nReviews), "★", "#a8d5a2"),
                                statCard("Revenus", revenue.toPlainString() + " €", "€", "#d4a5f5"));
                root.getChildren().add(stats);

                Label sectionTitle = new Label("Navigation rapide");
                sectionTitle.setStyle("-fx-text-fill: #666; -fx-font-size: 13; -fx-font-weight: bold;");
                root.getChildren().add(sectionTitle);

                FlowPane sections = new FlowPane();
                sections.setHgap(16);
                sections.setVgap(16);
                sections.getChildren().addAll(
                                descCard("≡", "PRODUITS", "Gérer toutes les œuvres d'art. CRUD complet."),
                                descCard("◉", "COMMANDES", "Historique et gestion des achats."),
                                descCard("⚡", "ENCHÈRES", "Offres et enchères sur les produits."),
                                descCard("★", "AVIS", "Évaluations et notes clients."),
                                descCard("♥", "LISTE DE SOUHAITS", "Produits mis en favoris."));
                root.getChildren().add(sections);
        }

        // ── ARTIST dashboard ──────────────────────────────────────────
        private void buildArtistDashboard(VBox root) {
                Label welcome = new Label("BONJOUR, " + session.getName().toUpperCase() + " !");
                welcome.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 24; -fx-font-weight: bold;");
                Label sub = new Label("Tableau de bord Artiste — vos statistiques personnelles");
                sub.setStyle("-fx-text-fill: #666; -fx-font-size: 13;");
                root.getChildren().add(new VBox(4, welcome, sub));

                // Artist's own products
                List<Product> myProducts = productService.getAll().stream()
                                .filter(p -> session.getName()
                                                .equalsIgnoreCase(p.getArtistName() != null ? p.getArtistName() : ""))
                                .collect(Collectors.toList());

                long available = myProducts.stream().filter(p -> "available".equals(p.getStatus())).count();
                long sold = myProducts.stream().filter(p -> "sold".equals(p.getStatus())).count();

                Set<Integer> myProductIds = myProducts.stream().map(Product::getId).collect(Collectors.toSet());

                int nOrders = (int) orderService.getAll().stream()
                                .filter(o -> myProductIds.contains(o.getProductId())).count();
                int nBids = (int) bidService.getAll().stream()
                                .filter(b -> myProductIds.contains(b.getProductId())).count();

                BigDecimal revenue = orderService.getAll().stream()
                                .filter(o -> myProductIds.contains(o.getProductId()))
                                .map((com.marketplace.models.Order o) -> o.getPrice() != null ? o.getPrice()
                                                : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Stats row
                HBox stats = new HBox(14);
                stats.getChildren().addAll(
                                statCard("Mes Produits", String.valueOf(myProducts.size()), "≡", "#c0c0c0"),
                                statCard("Disponibles", String.valueOf(available), "✓", "#a8d5a2"),
                                statCard("Vendus", String.valueOf(sold), "✕", "#e3a0a0"),
                                statCard("Commandes reçues", String.valueOf(nOrders), "◉", "#7ec8e3"),
                                statCard("Enchères reçues", String.valueOf(nBids), "⚡", "#e3c07e"),
                                statCard("Revenu total", revenue.toPlainString() + " €", "€", "#d4a5f5"));
                root.getChildren().add(stats);

                // Product breakdown table
                if (!myProducts.isEmpty()) {
                        Label prodTitle = new Label("Mes produits récents");
                        prodTitle.setStyle("-fx-text-fill: #888; -fx-font-size: 13; -fx-font-weight: bold;");
                        root.getChildren().add(prodTitle);

                        VBox prodList = new VBox(8);
                        // Header
                        HBox header = prodRow("Nom", "Prix", "Type", "Statut", true);
                        prodList.getChildren().add(header);
                        // Rows (max 10)
                        myProducts.stream().limit(10).forEach(p -> {
                                String price = p.getPrice() != null ? p.getPrice().toPlainString() + " €" : "—";
                                prodList.getChildren()
                                                .add(prodRow(p.getName(), price, p.getType(), p.getStatus(), false));
                        });
                        root.getChildren().add(prodList);
                }

                // Quick tips
                Label tips = new Label(
                                "💡  Cliquez sur « PRODUITS » dans le menu pour modifier vos œuvres.   " +
                                                "Vos commandes sont sous « COMMANDES ».");
                tips.setStyle("-fx-text-fill: #555; -fx-font-size: 12;");
                root.getChildren().add(tips);
        }

        // ── Product row ────────────────────────────────────────────────
        private HBox prodRow(String name, String price, String type, String status, boolean isHeader) {
                String color = isHeader ? "#888" : "#ccc";
                String bg = isHeader ? "#222" : "#2a2a2a";
                HBox row = new HBox();
                row.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 6; -fx-padding: 8 14 8 14;");
                Label lName = col(name, color, 260);
                Label lPrice = col(price, isHeader ? "#888" : "#c0c0c0", 100);
                Label lType = col(type != null ? type : "—", color, 120);
                String statText = "available".equals(status) ? "Disponible"
                                : "sold".equals(status) ? "Vendu" : (status != null ? status : "—");
                String statColor = "available".equals(status) ? "#a8d5a2" : "sold".equals(status) ? "#e3a0a0" : "#888";
                Label lStat = col(statText, isHeader ? "#888" : statColor, 100);
                row.getChildren().addAll(lName, lPrice, lType, lStat);
                return row;
        }

        private Label col(String text, String color, double minW) {
                Label l = new Label(text != null ? text : "—");
                l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12;");
                l.setMinWidth(minW);
                return l;
        }

        // ── Stat card ──────────────────────────────────────────────────
        private VBox statCard(String label, String count, String icon, String color) {
                VBox card = new VBox(6);
                card.setPrefWidth(140);
                card.setPadding(new Insets(18));
                card.setAlignment(Pos.CENTER_LEFT);
                card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; " +
                                "-fx-border-color: #333; -fx-border-radius: 10; -fx-border-width: 1;");
                Label iconLbl = new Label(icon);
                iconLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 18;");
                Label countLbl = new Label(count);
                countLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22; -fx-font-weight: bold;");
                countLbl.setWrapText(true);
                Label nameLbl = new Label(label);
                nameLbl.setStyle("-fx-text-fill: #777; -fx-font-size: 11;");
                card.getChildren().addAll(iconLbl, countLbl, nameLbl);
                return card;
        }

        // ── Description card ───────────────────────────────────────────
        private VBox descCard(String icon, String title, String desc) {
                VBox card = new VBox(10);
                card.setPrefWidth(250);
                card.setPadding(new Insets(18));
                card.setStyle("-fx-background-color: #222; -fx-background-radius: 10; " +
                                "-fx-border-color: #2e2e2e; -fx-border-radius: 10; -fx-border-width: 1;");
                HBox titleRow = new HBox(10);
                Label iconLbl = new Label(icon);
                iconLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-size: 14;");
                Label titleLbl = new Label(title);
                titleLbl.setStyle("-fx-text-fill: #c0c0c0; -fx-font-weight: bold; -fx-font-size: 13;");
                titleRow.getChildren().addAll(iconLbl, titleLbl);
                titleRow.setAlignment(Pos.CENTER_LEFT);
                Label descLbl = new Label(desc);
                descLbl.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
                descLbl.setWrapText(true);
                card.getChildren().addAll(titleRow, descLbl);
                return card;
        }
}
