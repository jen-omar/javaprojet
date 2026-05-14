CREATE TABLE identity_verifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    extracted_text LONGTEXT NOT NULL,
    verification_status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_identity_verifications_user
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE
);
