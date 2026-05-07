CREATE TABLE IF NOT EXISTS chat_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_message TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_history_user_id (user_id),
    CONSTRAINT fk_chat_history_user
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE
);
