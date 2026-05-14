-- phpMyAdmin SQL Dump
-- version 5.2.1deb3
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Dec 17, 2025 at 12:22 AM
-- Server version: 8.0.44-0ubuntu0.24.04.2
-- PHP Version: 8.3.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `vanhanh`
--

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `_id` char(24) NOT NULL,
  `userId` char(24) NOT NULL,
  `vouhcerId` char(24) DEFAULT NULL,
  `isFollow` tinyint(1) DEFAULT '0',
  `cancelledAfterPackaged` tinyint(1) DEFAULT '0',
  `shippingFeeChange` tinyint(1) DEFAULT '0',
  `paymentAdjust` tinyint(1) DEFAULT '0',
  `pointCount` varchar(50) DEFAULT 'normal',
  `thirdPartyId` varchar(255) NOT NULL,
  `warehouseStatus` json DEFAULT NULL,
  `buyerId` varchar(255) DEFAULT NULL,
  `ref` varchar(255) DEFAULT NULL,
  `buyerUserName` varchar(255) DEFAULT NULL,
  `orderId` varchar(255) NOT NULL,
  `returnId` varchar(255) DEFAULT NULL,
  `platform` varchar(50) DEFAULT NULL,
  `note` mediumtext,
  `noteAddress` text,
  `tag` varchar(255) DEFAULT NULL,
  `data` json DEFAULT NULL,
  `weight` decimal(10,3) DEFAULT NULL,
  `rating` int DEFAULT NULL,
  `ratingComments` text,
  `status` varchar(50) DEFAULT NULL,
  `amount` decimal(18,2) DEFAULT NULL,
  `fee` decimal(18,2) DEFAULT NULL,
  `serviceFee` decimal(18,2) DEFAULT NULL,
  `sellerTransactionFee` decimal(18,2) DEFAULT NULL,
  `revenue` decimal(18,2) DEFAULT NULL,
  `revenueAdjust` decimal(18,2) DEFAULT NULL,
  `profit` decimal(18,2) DEFAULT NULL,
  `profitAdjust` decimal(18,2) DEFAULT NULL,
  `voucherFromMarket` decimal(18,2) DEFAULT NULL,
  `voucherFromSeller` decimal(18,2) DEFAULT NULL,
  `totalCostOfGoods` decimal(18,2) DEFAULT NULL,
  `tax` decimal(18,2) DEFAULT NULL,
  `deliveryFee` decimal(18,2) DEFAULT NULL,
  `deliveryFeePlatformRebate` decimal(18,2) DEFAULT NULL,
  `phone` varchar(50) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `district` varchar(255) DEFAULT NULL,
  `ward` varchar(255) DEFAULT NULL,
  `address` text,
  `paymentMethod` varchar(50) DEFAULT NULL,
  `time` datetime DEFAULT NULL,
  `returnLastTimeChecking` datetime DEFAULT NULL,
  `shipByDate` datetime DEFAULT NULL,
  `pickUpDoneTime` datetime DEFAULT NULL,
  `shippedAt` datetime DEFAULT NULL,
  `fastDeliveryTime` datetime DEFAULT NULL,
  `billUrl` varchar(255) DEFAULT NULL,
  `trackingNumber` varchar(255) DEFAULT NULL,
  `trackingReturnNumber` varchar(255) DEFAULT NULL,
  `statusDelivery` varchar(50) DEFAULT NULL,
  `statusDeliveryLastText` text,
  `walletReceived` decimal(18,2) DEFAULT NULL,
  `walletReceivedDiff` tinyint(1) DEFAULT '0',
  `packagedAt` json DEFAULT NULL,
  `deliveryPartner` varchar(255) DEFAULT NULL,
  `firstDeliveryPartner` varchar(255) DEFAULT NULL,
  `firstPackagedAt` datetime DEFAULT NULL,
  `returnedAt` json DEFAULT NULL,
  `firstReturnedAt` datetime DEFAULT NULL,
  `warehouse` varchar(255) DEFAULT NULL,
  `notify` json DEFAULT NULL,
  `notCaculateMoney` tinyint(1) DEFAULT '0',
  `videoPackaged` json DEFAULT NULL,
  `proofDelivery` json DEFAULT NULL,
  `createdAt` datetime DEFAULT NULL,
  `updatedAt` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `order_items`
--

CREATE TABLE `order_items` (
  `id` int NOT NULL,
  `orderId` char(24) NOT NULL,
  `itemId` varchar(255) DEFAULT NULL,
  `modelId` varchar(255) DEFAULT NULL,
  `sku` varchar(255) DEFAULT NULL,
  `points` int DEFAULT '0',
  `productName` mediumtext,
  `skuName` text,
  `productImage` text,
  `quantity` int DEFAULT NULL,
  `price` decimal(18,2) DEFAULT NULL,
  `cost` decimal(10,2) DEFAULT '0.00'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `products`
--

CREATE TABLE `products` (
  `_id` char(24) NOT NULL,
  `userId` char(24) NOT NULL,
  `categoryId` char(24) DEFAULT NULL,
  `metaTitle` varchar(255) DEFAULT NULL,
  `metaDesciption` text,
  `metaOgImage` varchar(255) DEFAULT NULL,
  `displayType` varchar(50) DEFAULT 'normal',
  `activeOnWeb` tinyint(1) DEFAULT '1',
  `data` json DEFAULT NULL,
  `images` json DEFAULT NULL,
  `weight` decimal(10,3) DEFAULT NULL,
  `brand` varchar(255) DEFAULT NULL,
  `customDescription` mediumtext,
  `description` text,
  `descriptionInfo` json DEFAULT NULL,
  `descriptionType` varchar(50) DEFAULT NULL,
  `thirdPartyId` varchar(255) NOT NULL,
  `itemId` varchar(255) DEFAULT NULL,
  `productName` varchar(255) DEFAULT NULL,
  `platform` varchar(50) DEFAULT NULL,
  `productImage` text,
  `itemSku` varchar(255) DEFAULT NULL,
  `price` decimal(18,2) DEFAULT NULL,
  `priceOnWebsite` decimal(18,2) DEFAULT NULL,
  `listedPrice` decimal(18,2) DEFAULT NULL,
  `stock` int DEFAULT NULL,
  `cost` decimal(18,2) DEFAULT NULL,
  `totalRevenue` decimal(18,2) DEFAULT NULL,
  `totalProfit` decimal(18,2) DEFAULT NULL,
  `totalOrders` int DEFAULT NULL,
  `lifeTimeDate` int DEFAULT NULL,
  `isBoostItem` tinyint(1) DEFAULT '0',
  `extraGiftId` char(24) DEFAULT NULL,
  `points` int DEFAULT '0',
  `giftId` char(24) DEFAULT NULL,
  `guaranteeId` char(24) DEFAULT NULL,
  `createdAt` datetime DEFAULT NULL,
  `updatedAt` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`_id`),
  ADD UNIQUE KEY `unq_order` (`userId`,`thirdPartyId`,`orderId`),
  ADD KEY `idx_userId` (`userId`),
  ADD KEY `idx_buyerId` (`buyerId`),
  ADD KEY `idx_orderId` (`orderId`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_phone` (`phone`),
  ADD KEY `idx_time` (`time`),
  ADD KEY `idx_trackingNumber` (`trackingNumber`),
  ADD KEY `idx_trackingReturnNumber` (`trackingReturnNumber`),
  ADD KEY `idx_firstPackagedAt` (`firstPackagedAt`);

--
-- Indexes for table `order_items`
--
ALTER TABLE `order_items`
  ADD PRIMARY KEY (`id`),
  ADD KEY `orderId` (`orderId`);

--
-- Indexes for table `products`
--
ALTER TABLE `products`
  ADD PRIMARY KEY (`_id`),
  ADD KEY `idx_userId` (`userId`),
  ADD KEY `idx_thirdPartyId` (`thirdPartyId`),
  ADD KEY `idx_itemId` (`itemId`),
  ADD KEY `idx_productName` (`productName`),
  ADD KEY `idx_isBoostItem` (`isBoostItem`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `order_items`
--
ALTER TABLE `order_items`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`userId`) REFERENCES `users` (`_id`);

--
-- Constraints for table `order_items`
--
ALTER TABLE `order_items`
  ADD CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`orderId`) REFERENCES `orders` (`_id`);

--
-- Constraints for table `products`
--
ALTER TABLE `products`
  ADD CONSTRAINT `products_ibfk_1` FOREIGN KEY (`userId`) REFERENCES `users` (`_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
