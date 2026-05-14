CREATE TABLE IF NOT EXISTS otp_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expiration_time TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_codes_user_id (user_id),
    INDEX idx_otp_codes_expiration_time (expiration_time),
    CONSTRAINT fk_otp_codes_user
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE
);

ALTER TABLE user
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30);
