-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : jeu. 09 avr. 2026 à 00:36
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `marketplace`
--

-- --------------------------------------------------------

--
-- Structure de la table `bid`
--

CREATE TABLE `bid` (
  `id` int(11) NOT NULL,
  `bidder_name` varchar(255) NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `created_at` datetime NOT NULL,
  `product_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `bid`
--

INSERT INTO `bid` (`id`, `bidder_name`, `amount`, `created_at`, `product_id`) VALUES
(1, 'hamdi', 50.00, '2026-02-27 09:29:23', 4),
(2, 'azziz', 1000.00, '2026-02-27 09:30:24', 4),
(3, 'asser', 10000000.00, '2026-03-04 00:15:02', 9),
(4, 'majed', 40000.00, '2026-03-04 00:33:24', 11),
(5, 'majed', 40000.03, '2026-03-04 00:34:33', 11),
(6, 'majed', 40000.09, '2026-03-04 00:34:43', 11),
(7, 'majed', 40000.13, '2026-03-04 00:34:59', 11),
(8, 'asser', 40000.18, '2026-03-04 00:35:48', 11),
(9, 'asser', 400.00, '2026-03-04 01:17:28', 14);

-- --------------------------------------------------------

--
-- Structure de la table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Structure de la table `enchere`
--

CREATE TABLE `enchere` (
  `id` int(11) NOT NULL,
  `titre` varchar(255) NOT NULL,
  `prix_de_depart` decimal(10,2) NOT NULL,
  `prix_courant` decimal(10,2) NOT NULL,
  `date_fin` datetime NOT NULL,
  `description` text DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `statut` enum('en_cours','terminee','annulee') DEFAULT 'en_cours',
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;

-- --------------------------------------------------------

--
-- Structure de la table `messenger_messages`
--

CREATE TABLE `messenger_messages` (
  `id` bigint(20) NOT NULL,
  `body` longtext NOT NULL,
  `headers` longtext NOT NULL,
  `queue_name` varchar(190) NOT NULL,
  `created_at` datetime NOT NULL,
  `available_at` datetime NOT NULL,
  `delivered_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `order`
--

CREATE TABLE `order` (
  `id` int(11) NOT NULL,
  `buyer_name` varchar(255) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `order_type` varchar(50) NOT NULL,
  `created_at` datetime NOT NULL,
  `product_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `order`
--

INSERT INTO `order` (`id`, `buyer_name`, `price`, `order_type`, `created_at`, `product_id`) VALUES
(1, 'asser', 4500.00, 'purchase', '2026-02-27 09:31:55', 3),
(2, 'asser', 200.00, 'purchase', '2026-02-27 09:39:01', 5),
(3, 'asser', 1000.00, 'purchase', '2026-03-04 00:11:18', 1),
(4, 'asser', 6000.00, 'purchase', '2026-03-04 00:31:34', 8),
(5, 'majed', 5000.00, 'purchase', '2026-03-04 00:41:39', 10),
(6, 'asser', 20.00, 'purchase', '2026-03-04 01:06:37', 13),
(7, 'asser', 200.00, 'purchase', '2026-03-04 01:17:55', 12),
(8, 'majed', 3000.00, 'purchase', '2026-03-04 01:19:29', 15),
(9, 'kkk', 300.00, 'purchase', '2026-03-04 01:25:25', 7),
(10, 'majed', 3000.00, 'purchase', '2026-03-04 01:37:05', 16),
(11, 'majed', 0.01, 'purchase', '2026-03-04 01:38:41', 2);

-- --------------------------------------------------------

--
-- Structure de la table `product`
--

CREATE TABLE `product` (
  `id` int(11) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` longtext DEFAULT NULL,
  `price` decimal(10,2) NOT NULL,
  `artist_name` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `type` varchar(100) NOT NULL,
  `category` varchar(100) DEFAULT NULL,
  `sale_type` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `auction_end_time` datetime DEFAULT NULL,
  `current_bid` decimal(10,2) DEFAULT NULL,
  `current_bidder` varchar(255) DEFAULT NULL,
  `buyer` varchar(255) DEFAULT NULL,
  `reserve_price` decimal(10,2) DEFAULT NULL,
  `min_bid_increment` decimal(10,2) DEFAULT NULL,
  `auction_start_time` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `product`
--

INSERT INTO `product` (`id`, `name`, `description`, `price`, `artist_name`, `created_at`, `image_url`, `type`, `category`, `sale_type`, `status`, `auction_end_time`, `current_bid`, `current_bidder`, `buyer`, `reserve_price`, `min_bid_increment`, `auction_start_time`) VALUES
(1, 'auction', '', 1000.00, 'aziz', '2026-02-25 21:41:22', '/uploads/products/aaaaa-699f5e7223bd8.webp', 'Digital Art', 'Still Life', 'fixed', 'sold', NULL, NULL, NULL, 'asser', NULL, 0.01, NULL),
(2, 'uiygku', 'oidgfioudf', 0.01, 'jen', '2026-02-25 22:59:34', '/uploads/products/629336363-804489845995067-2401566082243735697-n-699f70c6419e6.jpg', 'Drawing', 'Minimalist', 'fixed', 'sold', NULL, NULL, NULL, 'majed', NULL, 0.01, NULL),
(3, 'hgjhn', 'hhhh', 4500.00, 'jen', '2026-02-25 23:00:12', '/uploads/products/608754240-1598091944539218-5029855443694363943-n-699f70ecac49c.png', 'Other', 'Surrealist', 'fixed', 'sold', NULL, NULL, NULL, 'asser', NULL, 0.01, NULL),
(4, 'ana', '', 20.00, 'omar', '2026-02-27 09:27:20', '/uploads/products/Screenshot-2024-12-07-002320-69a155687deeb.png', 'Photography', 'Pop Art', 'auction', 'available', '2026-02-28 09:27:20', 1000.00, 'azziz', NULL, NULL, 0.01, NULL),
(5, 'walhman3ref', 'good louled', 200.00, 'omar', '2026-02-27 09:38:02', '/uploads/products/Screenshot-2024-12-07-021328-69a157ea9b932.png', 'Digital Art', 'Modern', 'fixed', 'sold', NULL, NULL, NULL, 'asser', NULL, 0.01, NULL),
(6, 'jdid', 'hoia', 200.00, 'omar', '2026-03-03 23:29:26', '/uploads/products/Capture-d-ecran-2026-03-03-015548-69a760c6e3a8a.png', 'Painting', 'Landscape', 'auction', 'available', '2026-03-04 23:29:26', 200.00, NULL, NULL, NULL, 0.01, NULL),
(7, 'ggdhd', 'gghhd', 300.00, 'hffh', '2026-03-04 00:12:11', '/uploads/products/Capture-d-ecran-2026-03-03-024916-69a76acb12926.png', 'Painting', 'Landscape', 'fixed', 'sold', NULL, NULL, NULL, 'kkk', NULL, 0.01, NULL),
(8, 'dfgdg', 'dfgdfg', 6000.00, 'hffh', '2026-03-04 00:12:43', '/uploads/products/Capture-d-ecran-2026-03-03-024302-69a76aeb547e8.png', 'Sculpture', 'Classical', 'fixed', 'sold', NULL, NULL, NULL, 'asser', NULL, 0.01, NULL),
(9, 'trgf', 'ereety', 5000.00, 'hffh', '2026-03-04 00:13:03', '/uploads/products/Capture-d-ecran-2026-03-03-022551-69a76aff7288c.png', 'Sculpture', 'Landscape', 'auction', 'available', '2026-03-05 00:13:03', 10000000.00, 'asser', NULL, NULL, 0.01, NULL),
(10, 'trgf', 'dffds', 5000.00, 'omar', '2026-03-04 00:25:32', NULL, 'Digital Art', '', 'fixed', 'sold', NULL, NULL, NULL, 'majed', NULL, 0.01, NULL),
(11, 'mnrech', 'dddd', 3000.00, 'omarjen', '2026-03-04 00:32:32', '/uploads/products/Screenshot-2024-12-07-002320-69a76f90792ba.png', 'Digital Art', 'Portrait', 'auction', 'available', '2026-03-05 00:32:32', 40000.18, 'asser', NULL, NULL, 0.01, NULL),
(12, 'fbfxb', 'vbcvb', 200.00, 'aziz', '2026-03-04 00:42:20', '/uploads/products/Capture-d-ecran-2026-02-24-111333-69a771dc80ed8.png', 'Sculpture', 'Still Life', 'fixed', 'sold', NULL, NULL, NULL, 'asser', NULL, 0.01, NULL),
(13, 'dhdfh', 'fgdf', 20.00, 'aziz', '2026-03-04 00:47:23', '/uploads/products/Screenshot-2024-12-06-235942-69a7730b916fd.png', 'Digital Art', 'Portrait', 'fixed', 'sold', NULL, NULL, NULL, 'asser', NULL, 0.01, NULL),
(14, 'sdgc', 'gsdgc', 300.00, 'aziz', '2026-03-04 00:47:42', '/uploads/products/Screenshot-2024-12-07-021511-69a7731ec7b01.png', 'Mixed Media', 'Impressionist', 'auction', 'available', '2026-03-05 00:47:42', 400.00, 'asser', NULL, NULL, 0.01, NULL),
(15, 'trer', 'reter', 3000.00, 'omar', '2026-03-04 01:19:04', '/uploads/products/Capture-d-ecran-2026-03-03-022551-69a77a78db157.png', 'Painting', 'Landscape', 'fixed', 'sold', NULL, NULL, NULL, 'majed', NULL, 0.01, NULL),
(16, 'trer', 'jjjjj', 3000.00, 'omar', '2026-03-04 01:27:53', '/uploads/products/Capture-d-ecran-2026-03-03-022551-69a77c8926a79.png', 'Sculpture', 'Modern', 'fixed', 'sold', NULL, NULL, NULL, 'majed', NULL, 0.01, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `review`
--

CREATE TABLE `review` (
  `id` int(11) NOT NULL,
  `reviewer_name` varchar(255) NOT NULL,
  `rating` int(11) NOT NULL,
  `comment` longtext DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `product_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `wishlist`
--

CREATE TABLE `wishlist` (
  `id` int(11) NOT NULL,
  `client_name` varchar(255) NOT NULL,
  `created_at` datetime NOT NULL,
  `product_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `wishlist`
--

INSERT INTO `wishlist` (`id`, `client_name`, `created_at`, `product_id`) VALUES
(1, 'majed', '2026-02-27 09:28:45', 4),
(2, 'jen', '2026-03-03 23:30:44', 6),
(3, 'azziz', '2026-03-04 00:13:40', 8),
(5, 'asser', '2026-03-04 00:14:10', 8),
(6, 'asser', '2026-03-04 00:26:11', 10),
(7, 'majed', '2026-03-04 00:40:32', 10),
(8, 'asser', '2026-03-04 01:05:34', 13),
(9, 'majed', '2026-03-04 01:18:59', 14);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `bid`
--
ALTER TABLE `bid`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_4AF2B3F34584665A` (`product_id`);

--
-- Index pour la table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

--
-- Index pour la table `enchere`
--
ALTER TABLE `enchere`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_75EA56E0FB7336F0E3BD61CE16BA31DBBF396750` (`queue_name`,`available_at`,`delivered_at`,`id`);

--
-- Index pour la table `order`
--
ALTER TABLE `order`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_F52993984584665A` (`product_id`);

--
-- Index pour la table `product`
--
ALTER TABLE `product`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `review`
--
ALTER TABLE `review`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_794381C64584665A` (`product_id`);

--
-- Index pour la table `wishlist`
--
ALTER TABLE `wishlist`
  ADD PRIMARY KEY (`id`),
  ADD KEY `IDX_9CE12A314584665A` (`product_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `bid`
--
ALTER TABLE `bid`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `enchere`
--
ALTER TABLE `enchere`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `messenger_messages`
--
ALTER TABLE `messenger_messages`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `order`
--
ALTER TABLE `order`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT pour la table `product`
--
ALTER TABLE `product`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT pour la table `review`
--
ALTER TABLE `review`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `wishlist`
--
ALTER TABLE `wishlist`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `bid`
--
ALTER TABLE `bid`
  ADD CONSTRAINT `FK_4AF2B3F34584665A` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`);

--
-- Contraintes pour la table `order`
--
ALTER TABLE `order`
  ADD CONSTRAINT `FK_F52993984584665A` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`);

--
-- Contraintes pour la table `review`
--
ALTER TABLE `review`
  ADD CONSTRAINT `FK_794381C64584665A` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`);

--
-- Contraintes pour la table `wishlist`
--
ALTER TABLE `wishlist`
  ADD CONSTRAINT `FK_9CE12A314584665A` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
