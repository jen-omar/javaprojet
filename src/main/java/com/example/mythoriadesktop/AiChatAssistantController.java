package com.example.mythoriadesktop;

import com.example.mythoriadesktop.model.ChatMessage;
import com.example.mythoriadesktop.model.User;
import com.example.mythoriadesktop.services.AiAssistantService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;

public class AiChatAssistantController {
    private static final long DEFAULT_THINKING_DELAY_MILLIS = 1200;

    @FXML
    private Label chatStatusLabel;

    @FXML
    private ListView<ChatMessage> conversationList;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    @FXML
    private Button clearButton;

    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    private final AiAssistantService aiAssistantService = new AiAssistantService();
    private final long thinkingDelayMillis = loadThinkingDelayMillis();

    private User currentUser;

    @FXML
    private void initialize() {
        conversationList.setItems(messages);
        conversationList.setCellFactory(listView -> new ChatMessageCell());
        addAiMessage("Hello, I am the Mythoria assistant. Ask me about features, wallet, security, roles, or fantasy writing.");
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    private void onSendMessage() {
        String message = Optional.ofNullable(messageField.getText()).orElse("").trim();
        if (message.isBlank()) {
            return;
        }

        addUserMessage(message);
        messageField.clear();
        setLoading(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws InterruptedException {
                pauseBeforeResponse();
                return aiAssistantService.generateResponse(message, currentUser);
            }
        };

        task.setOnSucceeded(event -> {
            setLoading(false);
            addAiMessage(task.getValue());
        });
        task.setOnFailed(event -> {
            setLoading(false);
            addAiMessage("I could not generate a response right now. Please try again.");
        });

        Thread thread = new Thread(task, "ai-chat-assistant");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onClearChat() {
        messages.clear();
        addAiMessage("Chat cleared. How can I help you in Mythoria?");
    }

    private void addUserMessage(String message) {
        messages.add(new ChatMessage("USER", message, LocalDateTime.now()));
        scrollToLast();
    }

    private void addAiMessage(String message) {
        messages.add(new ChatMessage("AI", message, LocalDateTime.now()));
        scrollToLast();
    }

    private void setLoading(boolean loading) {
        sendButton.setDisable(loading);
        clearButton.setDisable(loading);
        messageField.setDisable(loading);
        chatStatusLabel.setText(loading ? "Thinking..." : "Ready");
    }

    private void scrollToLast() {
        Platform.runLater(() -> conversationList.scrollTo(Math.max(0, messages.size() - 1)));
    }

    private void pauseBeforeResponse() throws InterruptedException {
        if (thinkingDelayMillis > 0) {
            Thread.sleep(thinkingDelayMillis);
        }
    }

    private static long loadThinkingDelayMillis() {
        Properties properties = new Properties();
        try (InputStream stream = AiChatAssistantController.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ex) {
            return DEFAULT_THINKING_DELAY_MILLIS;
        }

        String configuredDelay = properties.getProperty("ai.thinking.delay.millis");
        if (configuredDelay == null || configuredDelay.isBlank()) {
            return DEFAULT_THINKING_DELAY_MILLIS;
        }

        try {
            long delay = Long.parseLong(configuredDelay.trim());
            return Math.max(0, Math.min(delay, 5000));
        } catch (NumberFormatException ex) {
            return DEFAULT_THINKING_DELAY_MILLIS;
        }
    }

    private static final class ChatMessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                return;
            }

            Label bubble = new Label(item.message());
            bubble.setWrapText(true);
            bubble.setMaxWidth(620);
            bubble.getStyleClass().add(item.fromUser() ? "chat-bubble-user" : "chat-bubble-ai");

            HBox row = new HBox(bubble);
            row.setAlignment(item.fromUser() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.getStyleClass().add("chat-row");

            VBox wrapper = new VBox(row);
            wrapper.setFillWidth(true);
            setGraphic(wrapper);
        }
    }
}
