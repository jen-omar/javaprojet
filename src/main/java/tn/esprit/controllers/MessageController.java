package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.Models.Message;
import tn.esprit.Models.User;
import tn.esprit.services.AiBridge;
import tn.esprit.services.MessageDAO;
import tn.esprit.data.UserRepository;
import tn.esprit.util.UserSession;

import tn.esprit.util.AudioRecorder;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class MessageController {

    @FXML private Label chatTitle;
    @FXML private VBox messageContainer;
    @FXML private ScrollPane messageScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button btnRecord;
    @FXML private Button btnOpenPortal;

    // ── AI Toolbar elements ──────────────────────────────────────
    @FXML private HBox aiToolbar;
    @FXML private Button btnSummary;
    @FXML private Button btnAutoReply;
    @FXML private Label aiStatusLabel;
    @FXML private HBox suggestionContainer;

    private final MessageDAO messageDAO = new MessageDAO();
    private final UserRepository userRepository = new UserRepository();
    private final AudioRecorder recorder = new AudioRecorder();
    private boolean isRecording = false;
    private String currentAudioPath = null;

    private int currentBriefId;
    private int recipientId;
    private User currentUser;

    /** Number of recent messages sent to the AI for context. */
    private static final int AI_CONTEXT_WINDOW = 15;

    public void initChat(int briefId, int recipientId, String title) {
        this.currentBriefId = briefId;
        this.recipientId = recipientId;
        this.currentUser = UserSession.getInstance().getUser();

        chatTitle.setText("Chambre des Contrats: " + title);
        loadMessages();

        // Auto scroll to bottom
        messageScrollPane.vvalueProperty().bind(messageContainer.heightProperty());

        // Automatically fetch suggestions if the last message is not ours
        fetchSuggestionsAutomatically();
    }

    private void loadMessages() {
        messageContainer.getChildren().clear();
        List<Message> messages = messageDAO.getByBriefId(currentBriefId);
        for (Message m : messages) {
            displayMessage(m);
        }
    }

    private void displayMessage(Message m) {
        boolean isMine = m.getSenderId() == parseId(currentUser.id());

        VBox wrapper = new VBox(5);
        wrapper.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(5, 10, 5, 10));

        Label senderLabel = new Label(isMine ? "You" : m.getSenderUsername());
        senderLabel.setStyle("-fx-font-size: 10; -fx-text-fill: -mythoria-silver;");

        if (m.getAudioPath() != null && !m.getAudioPath().isEmpty()) {
            HBox voiceBubble = new HBox(12);
            voiceBubble.setAlignment(Pos.CENTER_LEFT);
            voiceBubble.setPadding(new Insets(12, 18, 12, 18));
            voiceBubble.setMaxWidth(Region.USE_PREF_SIZE);

            if (isMine) {
                voiceBubble.setStyle("-fx-background-color: #0084FF; -fx-background-radius: 25;");
            } else {
                voiceBubble.setStyle("-fx-background-color: #3E4042; -fx-background-radius: 25;");
            }

            StackPane playContainer = new StackPane();
            playContainer.setPrefSize(28, 50);
            playContainer.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20;");

            Button playBtn = new Button("▶");
            playBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-padding: 0; -fx-cursor: hand;");

            playBtn.setOnAction(e -> {
                if (playBtn.getText().equals("▶")) {
                    playBtn.setText("⏹");
                    playContainer.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 20; -fx-border-color: -mythoria-gold; -fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, -mythoria-gold, 10, 0, 0, 0);");
                    AudioRecorder.playAudio(m.getAudioPath(), () -> Platform.runLater(() -> {
                        playBtn.setText("▶");
                        playContainer.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 20; -fx-border-color: rgba(255,255,255,0.3); -fx-border-radius: 20;");
                    }));
                }
            });
            playContainer.getChildren().add(playBtn);

            // Waveform representation (White bars)
            HBox waveform = new HBox(3);
            waveform.setAlignment(Pos.CENTER);
            for (int i = 0; i < 18; i++) {
                Region bar = new Region();
                double height = 4 + Math.random() * 22;
                bar.setPrefSize(3, height);
                bar.setStyle("-fx-background-color: white; -fx-background-radius: 4; -fx-opacity: 0.9;");
                waveform.getChildren().add(bar);
            }

            Label duration = new Label(AudioRecorder.getFormattedDuration(m.getAudioPath()));
            duration.setStyle("-fx-font-size: 14; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 0 0 0 5;");

            voiceBubble.getChildren().addAll(playContainer, waveform, duration);

            Label transcriptLabel = new Label("📜 " + m.getContent());
            transcriptLabel.setWrapText(true);
            transcriptLabel.setMaxWidth(350);
            transcriptLabel.setPadding(new Insets(10, 15, 10, 15));
            transcriptLabel.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: #C0C0C0; -fx-font-family: 'Georgia', serif; -fx-font-style: italic; -fx-font-size: 13; -fx-background-radius: 8; -fx-border-color: rgba(192,192,192,0.5); -fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 4);");

            VBox audioWithTranscript = new VBox(8);
            audioWithTranscript.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            audioWithTranscript.getChildren().addAll(voiceBubble, transcriptLabel);

            wrapper.getChildren().add(audioWithTranscript);
        } else {
            Label contentLabel = new Label(m.getContent());
            contentLabel.setWrapText(true);
            contentLabel.setMaxWidth(400);
            contentLabel.setPadding(new Insets(10, 15, 10, 15));

            if (isMine) {
                contentLabel.setStyle("-fx-background-color: linear-gradient(to right, #2b2311, #362c16); -fx-text-fill: #e8d08c; -fx-background-radius: 16 16 4 16; -fx-border-color: #c5a54e; -fx-border-width: 1; -fx-border-radius: 16 16 4 16; -fx-font-size: 14px; -fx-padding: 12 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);");
            } else {
                contentLabel.setStyle("-fx-background-color: linear-gradient(to right, #1e1e1e, #2a2a2a); -fx-text-fill: #e0e0e0; -fx-background-radius: 16 16 16 4; -fx-border-color: #555555; -fx-border-width: 1; -fx-border-radius: 16 16 16 4; -fx-font-size: 14px; -fx-padding: 12 16; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 2);");
            }
            wrapper.getChildren().add(contentLabel);
        }

        messageContainer.getChildren().add(wrapper);
    }

    @FXML
    public void onToggleRecord() {
        if (!isRecording) {
            // Start
            String fileName = "voice_" + UUID.randomUUID().toString() + ".wav";
            String directory = System.getProperty("user.home") + "/Mythoria/audio/";
            File dir = new File(directory);
            if (!dir.exists()) dir.mkdirs();

            currentAudioPath = directory + fileName;
            recorder.startRecording(currentAudioPath);
            btnRecord.setText("🛑");
            btnRecord.setStyle("-fx-text-fill: #ff4c4c; -fx-border-color: #ff4c4c;");
            isRecording = true;
        } else {
            // Stop and Send
            recorder.stopRecording();
            btnRecord.setText("🎤");
            btnRecord.setStyle("");
            isRecording = false;

            if (currentAudioPath != null) {
                String finalAudioPath = currentAudioPath;
                Message m = new Message(parseId(currentUser.id()), currentBriefId, "Transcribing Voice...", finalAudioPath, LocalDateTime.now());
                messageDAO.add(m);
                loadMessages();

                new Thread(() -> {
                    System.out.println("🎤 Starting transcription thread for: " + finalAudioPath);
                    String transcribed = tn.esprit.services.VoiceService.transcribeAudio(finalAudioPath);
                    System.out.println("📝 Transcription returned: " + transcribed);

                    Platform.runLater(() -> {
                        List<Message> msgs = messageDAO.getByBriefId(currentBriefId);
                        boolean found = false;
                        for (Message msg : msgs) {
                            if (finalAudioPath.equals(msg.getAudioPath())) {
                                System.out.println("💾 Updating message ID " + msg.getId() + " in DB with transcription.");
                                msg.setContent(transcribed);
                                messageDAO.update(msg);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            System.err.println("⚠️ Could not find message with audio path: " + finalAudioPath + " to update!");
                        }
                        loadMessages();
                    });
                }).start();
                currentAudioPath = null;
            }
        }
    }

    @FXML
    public void onSendMessage() {
        String text = messageInput.getText().trim();
        if (text.isEmpty()) return;

        Message m = new Message(parseId(currentUser.id()), currentBriefId, text, null, LocalDateTime.now());
        messageDAO.add(m);
        messageInput.clear();
        loadMessages();

        // Check for AI Auto-reply (Bot Mode) — isBotEnabled not in User record, disabled
        User recipient = userRepository.findById(String.valueOf(recipientId)).orElse(null);
        if (false) { // Bot mode disabled: User record has no isBotEnabled field
            triggerBotAutoReply(recipient);
        } else {
            // Always fetch suggestions even if no bot reply
            fetchSuggestionsAutomatically();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AI BUTTON HANDLERS
    // ══════════════════════════════════════════════════════════════

    /**
     * 📜 Scroll of Summary — Analyses the last N messages
     * and shows a summary in an in-app overlay popup.
     */
    @FXML
    public void onAiSummary() {
        List<Message> messages = getRecentMessages();
        if (messages.isEmpty()) {
            showAiStatus("⚠ No messages to summarize.");
            return;
        }

        setAiButtonsDisabled(true);
        showAiStatus("📜 The Scroll is being written…");

        AiBridge.summarizeAsync(messages).thenAccept(summary -> {
            Platform.runLater(() -> {
                setAiButtonsDisabled(false);
                clearAiStatus();
                showSummaryPopup(summary);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                setAiButtonsDisabled(false);
                showAiStatus("❌ " + extractErrorMessage(ex));
            });
            return null;
        });
    }

    /**
     * Automatically fetches reply suggestions if the last message was sent by someone else.
     */
    private void fetchSuggestionsAutomatically() {
        List<Message> all = messageDAO.getByBriefId(currentBriefId);
        if (all.isEmpty()) return;

        List<Message> messages = AiBridge.lastN(all, AI_CONTEXT_WINDOW);

        showAiStatus("🔮 The Artisan is preparing suggestions…");

        AiBridge.suggestAsync(messages).thenAccept(suggestions -> {
            Platform.runLater(() -> {
                clearAiStatus();
                showSuggestionChips(suggestions);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> this.clearAiStatus());
            return null;
        });
    }

    /**
     * ⚒ Forge Reply — Generates a full auto-reply based on
     * the user's role (Author / Client) and places it in the input.
     */
    @FXML
    public void onAiAutoReply() {
        List<Message> messages = getRecentMessages();
        if (messages.isEmpty()) {
            showAiStatus("⚠ No messages to forge a reply from.");
            return;
        }

        // Determine persona from user role
        String role = currentUser.role();
        String persona;
        switch (role) {
            case "ROLE_AUTHOR":
                persona = "a creative author in the Mythoria realm who writes with eloquence and professionalism";
                break;
            case "ROLE_CLIENT":
                persona = "a discerning client seeking quality creative work, direct and clear in communication";
                break;
            case "ROLE_ADMIN":
                persona = "an authoritative realm administrator who mediates fairly between authors and clients";
                break;
            default:
                persona = "a friendly professional assistant in the Mythoria creative marketplace";
        }

        setAiButtonsDisabled(true);
        showAiStatus("⚒ The Forge heats… crafting your reply…");

        AiBridge.autoReplyAsync(messages, persona).thenAccept(reply -> {
            Platform.runLater(() -> {
                setAiButtonsDisabled(false);
                clearAiStatus();
                // Place the forged reply into the text field for review before sending
                messageInput.setText(reply);
                messageInput.requestFocus();
                showAiStatus("✅ Reply forged! Review and press SEND.");
                fadeOutAiStatus(5);
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                setAiButtonsDisabled(false);
                showAiStatus("❌ " + extractErrorMessage(ex));
            });
            return null;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  BOT MODE — Auto-reply when recipient has is_bot_enabled = 1
    // ══════════════════════════════════════════════════════════════

    /**
     * When the recipient is a bot-enabled user, automatically generate
     * an AI reply and send it on their behalf after a short delay.
     */
    private void triggerBotAutoReply(User bot) {
        List<Message> messages = getRecentMessages();
        if (messages.isEmpty()) return;

        showAiStatus("🤖 " + bot.username() + " is thinking…");

        String persona = "a helpful and concise AI assistant acting as '" + bot.username()
                       + "' within the Mythoria creative marketplace";

        AiBridge.autoReplyAsync(messages, persona).thenAccept(reply -> {
            Platform.runLater(() -> {
                clearAiStatus();
                // Send the bot reply automatically
                Message botMsg = new Message(parseId(bot.id()), currentBriefId, reply, null, LocalDateTime.now());
                messageDAO.add(botMsg);
                loadMessages();
                showAiStatus("🤖 " + bot.username() + " has responded.");
                fadeOutAiStatus(4);

                // Fetch suggestions for the new bot message
                fetchSuggestionsAutomatically();
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                clearAiStatus();
                // Fallback to the static reply if Ollama is unreachable
                System.err.println("⚠️ Bot auto-reply failed: " + ex.getMessage());
                Message fallback = new Message(parseId(bot.id()), currentBriefId,
                        "The oracle has received your message. 'Through the forge, greatness is tempered.' How else may I assist your quest?",
                        null, LocalDateTime.now());
                messageDAO.add(fallback);
                loadMessages();
            });
            return null;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  UI HELPERS — Popups, Chips, Status
    // ══════════════════════════════════════════════════════════════

    /**
     * Show the AI summary in a styled overlay popup within the chat.
     */
    private void showSummaryPopup(String summaryText) {
        // Create overlay
        VBox popup = new VBox(20);
        popup.getStyleClass().add("ai-popup-overlay");
        popup.setPrefWidth(700);
        popup.setPrefHeight(500);
        popup.setMaxWidth(850);
        popup.setMaxHeight(650);

        Label title = new Label("📜  SCROLL OF SUMMARY");
        title.getStyleClass().add("ai-popup-title");

        Label body = new Label(summaryText);
        body.getStyleClass().add("ai-popup-body");
        body.setWrapText(true);

        ScrollPane scrollBody = new ScrollPane(body);
        scrollBody.setFitToWidth(true);
        scrollBody.setStyle("-fx-background-color: transparent;");
        scrollBody.setMinHeight(300);
        scrollBody.setPrefHeight(450);
        scrollBody.setMaxHeight(500);
        VBox.setVgrow(scrollBody, Priority.ALWAYS);

        Button closeBtn = new Button("✕  Close Scroll");
        closeBtn.getStyleClass().add("ai-popup-close");

        popup.getChildren().addAll(title, scrollBody, closeBtn);
        popup.setAlignment(Pos.CENTER);

        // Wrap in a dark semi-transparent overlay
        StackPane overlay = new StackPane(popup);
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlay.setAlignment(Pos.CENTER);

        // Add to the chatRoomContainer (covers the whole chat area)
        chatRoomContainer.getChildren().add(overlay);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), overlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        closeBtn.setOnAction(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), overlay);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> chatRoomContainer.getChildren().remove(overlay));
            fadeOut.play();
        });
    }

    /**
     * Display suggestion chips as clickable buttons in the suggestion bar.
     * Clicking a chip auto-fills the message input.
     */
    private void showSuggestionChips(List<String> suggestions) {
        suggestionContainer.getChildren().clear();
        suggestionContainer.setVisible(true);
        suggestionContainer.setManaged(true);

        Label chipLabel = new Label("🔮 Pick a suggestion:");
        chipLabel.setStyle("-fx-text-fill: #c5a54e; -fx-font-size: 12; -fx-font-weight: bold;");
        suggestionContainer.getChildren().add(chipLabel);

        for (int i = 0; i < suggestions.size(); i++) {
            String suggestion = suggestions.get(i);
            Button chip = new Button((i + 1) + ". " + suggestion);
            chip.getStyleClass().add("ai-suggestion-chip");
            chip.setWrapText(true);
            chip.setOnAction(e -> {
                messageInput.setText(suggestion);
                messageInput.requestFocus();
                hideSuggestionChips();
                showAiStatus("✅ Suggestion selected. Press SEND to deliver.");
                fadeOutAiStatus(4);
            });
            suggestionContainer.getChildren().add(chip);
        }

        // Add dismiss button
        Button dismissBtn = new Button("✕");
        dismissBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #888; -fx-font-size: 14; -fx-cursor: hand; -fx-padding: 4 8;");
        dismissBtn.setOnAction(e -> hideSuggestionChips());
        suggestionContainer.getChildren().add(dismissBtn);

        // Fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), suggestionContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    /**
     * Hide the suggestion chips bar.
     */
    private void hideSuggestionChips() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), suggestionContainer);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            suggestionContainer.setVisible(false);
            suggestionContainer.setManaged(false);
            suggestionContainer.getChildren().clear();
        });
        fadeOut.play();
    }

    /**
     * Get the last N messages for AI context.
     */
    private List<Message> getRecentMessages() {
        List<Message> all = messageDAO.getByBriefId(currentBriefId);
        return AiBridge.lastN(all, AI_CONTEXT_WINDOW);
    }

    /**
     * Show a status message in the AI toolbar.
     */
    private void showAiStatus(String text) {
        if (aiStatusLabel != null) {
            aiStatusLabel.setText(text);
        }
    }

    /**
     * Clear the AI status label.
     */
    private void clearAiStatus() {
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("");
        }
    }

    /**
     * Auto-clear the AI status after N seconds with a fade effect.
     */
    private void fadeOutAiStatus(int seconds) {
        PauseTransition pause = new PauseTransition(Duration.seconds(seconds));
        pause.setOnFinished(e -> {
            FadeTransition fade = new FadeTransition(Duration.millis(500), aiStatusLabel);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(ev -> {
                aiStatusLabel.setText("");
                aiStatusLabel.setOpacity(1);
            });
            fade.play();
        });
        pause.play();
    }

    /**
     * Disable / enable the three AI buttons (prevents double-clicks during generation).
     */
    private void setAiButtonsDisabled(boolean disabled) {
        if (btnSummary != null)   btnSummary.setDisable(disabled);
        if (btnAutoReply != null) btnAutoReply.setDisable(disabled);
    }

    /**
     * Extract a clean error message from a CompletableFuture exception.
     */
    private String extractErrorMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String msg = cause.getMessage();
        if (msg == null || msg.isEmpty()) {
            msg = "AI engine error. Is Ollama running?";
        }
        return msg;
    }

    // ══════════════════════════════════════════════════════════════
    //  EXISTING ACTIONS
    // ══════════════════════════════════════════════════════════════

    @FXML private VBox chatRoomContainer;

    @FXML
    public void onOpenPortal() {
        // Instantiate and open the Chromium-powered Kinship Portal
        KinshipPortal kinshipPortal = new KinshipPortal();
        kinshipPortal.openPortal(currentBriefId, currentUser.username());
    }

    @FXML
    public void onBack() {
        StackPane contentStack = (StackPane) chatRoomContainer.getScene().lookup("#contentStack");
        if (contentStack != null) {
            contentStack.getChildren().remove(chatRoomContainer);
        }
    }
    /** Helper to safely parse a User record's String id into an int. */
    private static int parseId(String id) {
        try { return Integer.parseInt(id); } catch (NumberFormatException e) { return -1; }
    }
}
