-- mythoria_db1.sql
CREATE DATABASE IF NOT EXISTS mythoria_db1;
USE mythoria_db1;

-- User table
CREATE TABLE IF NOT EXISTS user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    roles LONGTEXT NOT NULL,
    password VARCHAR(255) NOT NULL,
    prenom VARCHAR(50),
    nom VARCHAR(50),
    phone_number VARCHAR(30),
    est_valide TINYINT(1) DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    score INT DEFAULT 0,
    level VARCHAR(30) DEFAULT 'Débutant'
);

-- Supporting tables
CREATE TABLE IF NOT EXISTS otp_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expiration_time TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_codes_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS email_notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(150) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'SENT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_email_notifications_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS identity_verifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    extracted_text LONGTEXT NOT NULL,
    verification_status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_identity_verifications_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    user_message TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_history_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- Sample Data (Password is: password123)
INSERT INTO user (email, username, roles, password, prenom, nom, phone_number, score, level)
VALUES 
('admin@mythoria.com', 'admin', '["ROLE_ADMIN"]', '$2a$10$OenWl2cMadJdS4UudpAc/eMNrNviegffoQa8S2RZSa6Tkzk30nbE.', 'System', 'Admin', '+21699000001', 1000, 'Légende'),
('artist@mythoria.com', 'scribe', '["ROLE_AUTHOR"]', '$2a$10$OenWl2cMadJdS4UudpAc/eMNrNviegffoQa8S2RZSa6Tkzk30nbE.', 'Grand', 'Scribe', '+21699000002', 500, 'Expert'),
('majd@mythoria.com', 'majd', '["ROLE_CLIENT"]', '$2a$10$OenWl2cMadJdS4UudpAc/eMNrNviegffoQa8S2RZSa6Tkzk30nbE.', 'Majd', 'Ben Abdallah', '+21698169003', 0, 'Débutant');
