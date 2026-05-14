package tn.esprit.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseBootstrap {
    private DatabaseBootstrap() {
    }

    public static void ensureApplicationSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
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
                        level VARCHAR(30) DEFAULT 'Debutant'
                    )
                    """);
            addColumnIfMissing(statement, "user", "phone_number", "phone_number VARCHAR(30)");
            addColumnIfMissing(statement, "user", "level", "level VARCHAR(30) DEFAULT 'Debutant'");
            addColumnIfMissing(statement, "user", "contribution_points", "contribution_points INT NOT NULL DEFAULT 0");
            addColumnIfMissing(statement, "user", "current_rank", "current_rank VARCHAR(30) NOT NULL DEFAULT 'Commoner'");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS portefeuille (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        user_id INT NOT NULL,
                        solde DOUBLE DEFAULT 0,
                        statut VARCHAR(30) DEFAULT 'active',
                        devise VARCHAR(10) DEFAULT 'TND',
                        plafond DOUBLE DEFAULT 0,
                        INDEX idx_portefeuille_user_id (user_id),
                        CONSTRAINT fk_portefeuille_user
                            FOREIGN KEY (user_id) REFERENCES user(id)
                            ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS otp_codes (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        otp_code VARCHAR(10) NOT NULL,
                        expiration_time TIMESTAMP NOT NULL,
                        is_used BOOLEAN DEFAULT FALSE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_otp_codes_user_id (user_id),
                        CONSTRAINT fk_otp_codes_user
                            FOREIGN KEY (user_id) REFERENCES user(id)
                            ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
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
                        CONSTRAINT fk_email_notifications_user
                            FOREIGN KEY (user_id) REFERENCES user(id)
                            ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS identity_verifications (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        document_type VARCHAR(50) NOT NULL,
                        extracted_text LONGTEXT NOT NULL,
                        verification_status VARCHAR(20) NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_identity_verifications_user
                            FOREIGN KEY (user_id) REFERENCES user(id)
                            ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
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
                    )
                    """);
            statement.execute("""
                    INSERT INTO user (email, username, roles, password, prenom, nom, phone_number, score, level)
                    SELECT 'admin@mythoria.com', 'admin', '["ROLE_ADMIN"]',
                           '$2a$10$OenWl2cMadJdS4UudpAc/eMNrNviegffoQa8S2RZSa6Tkzk30nbE.',
                           'System', 'Admin', '+21699000001', 1000, 'Legende'
                    WHERE NOT EXISTS (SELECT 1 FROM user WHERE username = 'admin' OR email = 'admin@mythoria.com')
                    """);
            statement.execute("""
                    INSERT INTO portefeuille (solde, statut, devise, plafond, updated_at, user_id)
                    SELECT 0, 'actif', 'TND', 1000, NOW(), u.id
                    FROM user u
                    LEFT JOIN portefeuille p ON p.user_id = u.id
                    WHERE p.id IS NULL
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS collaboration (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        requester_id INT NOT NULL,
                        receiver_id INT NOT NULL,
                        world_id INT NOT NULL,
                        INDEX idx_collaboration_world_requester (world_id, requester_id)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS collaboration_request (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        world_id INT NOT NULL,
                        requester_id INT NOT NULL,
                        owner_id INT NOT NULL,
                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_collab_request_owner_status (owner_id, status),
                        INDEX idx_collab_request_world_requester_status (world_id, requester_id, status)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS world_collaborator (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        world_id INT NOT NULL,
                        user_id INT NOT NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_world_collaborator_world_user (world_id, user_id),
                        INDEX idx_world_collaborator_world_id (world_id),
                        INDEX idx_world_collaborator_user_id (user_id)
                    )
                    """);
            statement.execute("""
                    INSERT IGNORE INTO world_collaborator (world_id, user_id, created_at)
                    SELECT world_id, requester_id, created_at
                    FROM collaboration
                    WHERE status = 'ACCEPTED'
                    """);
        }
    }

    public static void ensureMarketplaceSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS product (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(150) NOT NULL,
                        description TEXT,
                        price DECIMAL(10,2) NOT NULL DEFAULT 0,
                        artist_name VARCHAR(150),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        image_url TEXT,
                        type VARCHAR(50),
                        category VARCHAR(100),
                        sale_type VARCHAR(50),
                        status VARCHAR(30) DEFAULT 'available',
                        min_bid_increment DECIMAL(10,2) DEFAULT 0.01,
                        current_bid DECIMAL(10,2) DEFAULT 0,
                        current_bidder VARCHAR(150),
                        buyer VARCHAR(150),
                        auction_end_time TIMESTAMP NULL
                    )
                    """);
            addColumnIfMissing(statement, "product", "min_bid_increment", "min_bid_increment DECIMAL(10,2) DEFAULT 0.01");
            addColumnIfMissing(statement, "product", "current_bid", "current_bid DECIMAL(10,2) DEFAULT 0");
            addColumnIfMissing(statement, "product", "current_bidder", "current_bidder VARCHAR(150)");
            addColumnIfMissing(statement, "product", "buyer", "buyer VARCHAR(150)");
            addColumnIfMissing(statement, "product", "auction_end_time", "auction_end_time TIMESTAMP NULL");
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS bid (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        bidder_name VARCHAR(150) NOT NULL,
                        amount DECIMAL(10,2) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        product_id INT NOT NULL,
                        INDEX idx_bid_product_id (product_id),
                        CONSTRAINT fk_bid_product
                            FOREIGN KEY (product_id) REFERENCES product(id)
                            ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS review (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        reviewer_name VARCHAR(150) NOT NULL,
                        rating INT NOT NULL,
                        comment TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        product_id INT NOT NULL,
                        INDEX idx_review_product_id (product_id),
                        CONSTRAINT fk_review_product
                            FOREIGN KEY (product_id) REFERENCES product(id)
                            ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS wishlist (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        client_name VARCHAR(150) NOT NULL,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        product_id INT NOT NULL,
                        INDEX idx_wishlist_product_id (product_id),
                        CONSTRAINT fk_wishlist_product
                            FOREIGN KEY (product_id) REFERENCES product(id)
                            ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS `order` (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        buyer_name VARCHAR(150) NOT NULL,
                        price DECIMAL(10,2) NOT NULL DEFAULT 0,
                        order_type VARCHAR(50),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        product_id INT NOT NULL,
                        INDEX idx_order_product_id (product_id),
                        CONSTRAINT fk_order_product
                            FOREIGN KEY (product_id) REFERENCES product(id)
                            ON DELETE CASCADE
                    )
                    """);
        }
    }

    public static void ensureEventSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS local (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(150) NOT NULL,
                        description TEXT,
                        price DOUBLE DEFAULT 0,
                        address VARCHAR(255),
                        capacity INT DEFAULT 0,
                        image TEXT,
                        status VARCHAR(50) DEFAULT 'available'
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS event (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        title VARCHAR(150) NOT NULL,
                        description TEXT,
                        image TEXT,
                        date DATETIME,
                        location VARCHAR(255),
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                        max_tickets INT DEFAULT 0,
                        max_vip_tickets INT DEFAULT 0,
                        max_normal_tickets INT DEFAULT 0,
                        creator_id INT,
                        local_id INT,
                        INDEX idx_event_local_id (local_id),
                        CONSTRAINT fk_event_local
                            FOREIGN KEY (local_id) REFERENCES local(id)
                            ON DELETE SET NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS ticket (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        event_id INT NOT NULL,
                        type VARCHAR(30),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        INDEX idx_ticket_user_id (user_id),
                        INDEX idx_ticket_event_id (event_id),
                        CONSTRAINT fk_ticket_user
                            FOREIGN KEY (user_id) REFERENCES user(id)
                            ON DELETE CASCADE,
                        CONSTRAINT fk_ticket_event
                            FOREIGN KEY (event_id) REFERENCES event(id)
                            ON DELETE CASCADE
                    )
                    """);
        }
    }

    private static void addColumnIfMissing(Statement statement, String tableName, String columnName, String definition) {
        try {
            statement.execute("ALTER TABLE `" + tableName + "` ADD COLUMN " + definition);
        } catch (SQLException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (!message.contains("duplicate column")) {
                System.err.println("Unable to ensure column " + tableName + "." + columnName + ": " + ex.getMessage());
            }
        }
    }
}
