CREATE TABLE IF NOT EXISTS email_notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'SENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_notifications_user_id (user_id),
    INDEX idx_email_notifications_type (notification_type),
    CONSTRAINT fk_email_notifications_user
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE
);
