-- MySQL dump 10.13  Distrib 26.7.0, for Linux (x86_64)
--
-- Host: localhost    Database: GiupViecDB
-- ------------------------------------------------------
-- Server version	26.7.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `KhuVuc`
--

DROP TABLE IF EXISTS `KhuVuc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `KhuVuc` (
  `id` int NOT NULL AUTO_INCREMENT,
  `MaKhuVuc` varchar(20) NOT NULL,
  `TenKhuVuc` varchar(150) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `QuanHuyen` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `TinhThanh` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `TrangThai` varchar(20) NOT NULL DEFAULT 'HoatDong',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_KV_ma` (`MaKhuVuc`),
  CONSTRAINT `chk_KV_trangThai` CHECK ((`TrangThai` in (_utf8mb4'HoatDong',_utf8mb4'TamNgung')))
) ENGINE=InnoDB AUTO_INCREMENT=431 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KhuVuc`
--

LOCK TABLES `KhuVuc` WRITE;
/*!40000 ALTER TABLE `KhuVuc` DISABLE KEYS */;
INSERT INTO `KhuVuc` VALUES (1,'KV-26743','Phường Bến Thành','Phường Bến Thành','Thành phố Hồ Chí Minh','TamNgung'),(52,'KV-27343','Phường Chợ Lớn','Phường Chợ Lớn','Thành phố Hồ Chí Minh','TamNgung'),(103,'KV-25978','Phường Thuận An','Phường Thuận An','Thành phố Hồ Chí Minh','TamNgung'),(154,'KV-26833','Phường Long Bình','Phường Long Bình','Thành phố Hồ Chí Minh','TamNgung'),(205,'KV-25747','Phường Thủ Dầu Một','Phường Thủ Dầu Một','Thành phố Hồ Chí Minh','TamNgung'),(206,'KV-25750','Phường Phú Lợi','Phường Phú Lợi','Thành phố Hồ Chí Minh','TamNgung'),(207,'KV-25760','Phường Bình Dương','Phường Bình Dương','Thành phố Hồ Chí Minh','TamNgung'),(208,'KV-25768','Phường Phú An','Phường Phú An','Thành phố Hồ Chí Minh','TamNgung'),(209,'KV-25771','Phường Chánh Hiệp','Phường Chánh Hiệp','Thành phố Hồ Chí Minh','TamNgung'),(210,'KV-25777','Xã Dầu Tiếng','Xã Dầu Tiếng','Thành phố Hồ Chí Minh','TamNgung'),(211,'KV-25780','Xã Minh Thạnh','Xã Minh Thạnh','Thành phố Hồ Chí Minh','TamNgung'),(212,'KV-25792','Xã Long Hòa','Xã Long Hòa','Thành phố Hồ Chí Minh','TamNgung'),(213,'KV-25807','Xã Thanh An','Xã Thanh An','Thành phố Hồ Chí Minh','TamNgung'),(214,'KV-25813','Phường Bến Cát','Phường Bến Cát','Thành phố Hồ Chí Minh','TamNgung'),(215,'KV-25819','Xã Trừ Văn Thố','Xã Trừ Văn Thố','Thành phố Hồ Chí Minh','TamNgung'),(216,'KV-25822','Xã Bàu Bàng','Xã Bàu Bàng','Thành phố Hồ Chí Minh','TamNgung'),(217,'KV-25837','Phường Chánh Phú Hòa','Phường Chánh Phú Hòa','Thành phố Hồ Chí Minh','TamNgung'),(218,'KV-25840','Phường Long Nguyên','Phường Long Nguyên','Thành phố Hồ Chí Minh','TamNgung'),(219,'KV-25843','Phường Tây Nam','Phường Tây Nam','Thành phố Hồ Chí Minh','TamNgung'),(220,'KV-25846','Phường Thới Hòa','Phường Thới Hòa','Thành phố Hồ Chí Minh','TamNgung'),(221,'KV-25849','Phường Hòa Lợi','Phường Hòa Lợi','Thành phố Hồ Chí Minh','TamNgung'),(222,'KV-25858','Xã Phú Giáo','Xã Phú Giáo','Thành phố Hồ Chí Minh','TamNgung'),(223,'KV-25864','Xã Phước Thành','Xã Phước Thành','Thành phố Hồ Chí Minh','TamNgung'),(224,'KV-25867','Xã An Long','Xã An Long','Thành phố Hồ Chí Minh','TamNgung'),(225,'KV-25882','Xã Phước Hòa','Xã Phước Hòa','Thành phố Hồ Chí Minh','TamNgung'),(226,'KV-25888','Phường Tân Uyên','Phường Tân Uyên','Thành phố Hồ Chí Minh','TamNgung'),(227,'KV-25891','Phường Tân Khánh','Phường Tân Khánh','Thành phố Hồ Chí Minh','TamNgung'),(228,'KV-25906','Xã Bắc Tân Uyên','Xã Bắc Tân Uyên','Thành phố Hồ Chí Minh','TamNgung'),(229,'KV-25909','Xã Thường Tân','Xã Thường Tân','Thành phố Hồ Chí Minh','TamNgung'),(230,'KV-25912','Phường Vĩnh Tân','Phường Vĩnh Tân','Thành phố Hồ Chí Minh','TamNgung'),(231,'KV-25915','Phường Bình Cơ','Phường Bình Cơ','Thành phố Hồ Chí Minh','TamNgung'),(232,'KV-25920','Phường Tân Hiệp','Phường Tân Hiệp','Thành phố Hồ Chí Minh','TamNgung'),(233,'KV-25942','Phường Dĩ An','Phường Dĩ An','Thành phố Hồ Chí Minh','TamNgung'),(234,'KV-25945','Phường Tân Đông Hiệp','Phường Tân Đông Hiệp','Thành phố Hồ Chí Minh','TamNgung'),(235,'KV-25951','Phường Đông Hòa','Phường Đông Hòa','Thành phố Hồ Chí Minh','TamNgung'),(236,'KV-25966','Phường Lái Thiêu','Phường Lái Thiêu','Thành phố Hồ Chí Minh','TamNgung'),(237,'KV-25969','Phường Thuận Giao','Phường Thuận Giao','Thành phố Hồ Chí Minh','TamNgung'),(238,'KV-25975','Phường An Phú','Phường An Phú','Thành phố Hồ Chí Minh','TamNgung'),(239,'KV-25987','Phường Bình Hòa','Phường Bình Hòa','Thành phố Hồ Chí Minh','TamNgung'),(240,'KV-26506','Phường Vũng Tàu','Phường Vũng Tàu','Thành phố Hồ Chí Minh','TamNgung'),(241,'KV-26526','Phường Tam Thắng','Phường Tam Thắng','Thành phố Hồ Chí Minh','TamNgung'),(242,'KV-26536','Phường Rạch Dừa','Phường Rạch Dừa','Thành phố Hồ Chí Minh','TamNgung'),(243,'KV-26542','Phường Phước Thắng','Phường Phước Thắng','Thành phố Hồ Chí Minh','TamNgung'),(244,'KV-26545','Xã Long Sơn','Xã Long Sơn','Thành phố Hồ Chí Minh','TamNgung'),(245,'KV-26560','Phường Bà Rịa','Phường Bà Rịa','Thành phố Hồ Chí Minh','TamNgung'),(246,'KV-26566','Phường Long Hương','Phường Long Hương','Thành phố Hồ Chí Minh','TamNgung'),(247,'KV-26572','Phường Tam Long','Phường Tam Long','Thành phố Hồ Chí Minh','TamNgung'),(248,'KV-26575','Xã Ngãi Giao','Xã Ngãi Giao','Thành phố Hồ Chí Minh','TamNgung'),(249,'KV-26584','Xã Xuân Sơn','Xã Xuân Sơn','Thành phố Hồ Chí Minh','TamNgung'),(250,'KV-26590','Xã Bình Giã','Xã Bình Giã','Thành phố Hồ Chí Minh','TamNgung'),(251,'KV-26596','Xã Châu Đức','Xã Châu Đức','Thành phố Hồ Chí Minh','TamNgung'),(252,'KV-26608','Xã Kim Long','Xã Kim Long','Thành phố Hồ Chí Minh','TamNgung'),(253,'KV-26617','Xã Nghĩa Thành','Xã Nghĩa Thành','Thành phố Hồ Chí Minh','TamNgung'),(254,'KV-26620','Xã Hồ Tràm','Xã Hồ Tràm','Thành phố Hồ Chí Minh','TamNgung'),(255,'KV-26632','Xã Xuyên Mộc','Xã Xuyên Mộc','Thành phố Hồ Chí Minh','TamNgung'),(256,'KV-26638','Xã Bàu Lâm','Xã Bàu Lâm','Thành phố Hồ Chí Minh','TamNgung'),(257,'KV-26641','Xã Hòa Hội','Xã Hòa Hội','Thành phố Hồ Chí Minh','TamNgung'),(258,'KV-26647','Xã Hòa Hiệp','Xã Hòa Hiệp','Thành phố Hồ Chí Minh','TamNgung'),(259,'KV-26656','Xã Bình Châu','Xã Bình Châu','Thành phố Hồ Chí Minh','TamNgung'),(260,'KV-26659','Xã Long Điền','Xã Long Điền','Thành phố Hồ Chí Minh','TamNgung'),(261,'KV-26662','Xã Long Hải','Xã Long Hải','Thành phố Hồ Chí Minh','TamNgung'),(262,'KV-26680','Xã Đất Đỏ','Xã Đất Đỏ','Thành phố Hồ Chí Minh','TamNgung'),(263,'KV-26686','Xã Phước Hải','Xã Phước Hải','Thành phố Hồ Chí Minh','TamNgung'),(264,'KV-26704','Phường Phú Mỹ','Phường Phú Mỹ','Thành phố Hồ Chí Minh','TamNgung'),(265,'KV-26710','Phường Tân Hải','Phường Tân Hải','Thành phố Hồ Chí Minh','TamNgung'),(266,'KV-26713','Phường Tân Phước','Phường Tân Phước','Thành phố Hồ Chí Minh','TamNgung'),(267,'KV-26725','Phường Tân Thành','Phường Tân Thành','Thành phố Hồ Chí Minh','TamNgung'),(268,'KV-26728','Xã Châu Pha','Xã Châu Pha','Thành phố Hồ Chí Minh','TamNgung'),(269,'KV-26732','Đặc khu Côn Đảo','Đặc khu Côn Đảo','Thành phố Hồ Chí Minh','TamNgung'),(270,'KV-26737','Phường Tân Định','Phường Tân Định','Thành phố Hồ Chí Minh','TamNgung'),(271,'KV-26740','Phường Sài Gòn','Phường Sài Gòn','Thành phố Hồ Chí Minh','TamNgung'),(272,'KV-26758','Phường Cầu Ông Lãnh','Phường Cầu Ông Lãnh','Thành phố Hồ Chí Minh','TamNgung'),(273,'KV-26767','Phường An Phú Đông','Phường An Phú Đông','Thành phố Hồ Chí Minh','TamNgung'),(274,'KV-26773','Phường Thới An','Phường Thới An','Thành phố Hồ Chí Minh','TamNgung'),(275,'KV-26782','Phường Tân Thới Hiệp','Phường Tân Thới Hiệp','Thành phố Hồ Chí Minh','TamNgung'),(276,'KV-26785','Phường Trung Mỹ Tây','Phường Trung Mỹ Tây','Thành phố Hồ Chí Minh','TamNgung'),(277,'KV-26791','Phường Đông Hưng Thuận','Phường Đông Hưng Thuận','Thành phố Hồ Chí Minh','TamNgung'),(278,'KV-26800','Phường Linh Xuân','Phường Linh Xuân','Thành phố Hồ Chí Minh','TamNgung'),(279,'KV-26803','Phường Tam Bình','Phường Tam Bình','Thành phố Hồ Chí Minh','TamNgung'),(280,'KV-26809','Phường Hiệp Bình','Phường Hiệp Bình','Thành phố Hồ Chí Minh','TamNgung'),(281,'KV-26824','Phường Thủ Đức','Phường Thủ Đức','Thành phố Hồ Chí Minh','TamNgung'),(282,'KV-26842','Phường Tăng Nhơn Phú','Phường Tăng Nhơn Phú','Thành phố Hồ Chí Minh','TamNgung'),(283,'KV-26848','Phường Phước Long','Phường Phước Long','Thành phố Hồ Chí Minh','TamNgung'),(284,'KV-26857','Phường Long Phước','Phường Long Phước','Thành phố Hồ Chí Minh','TamNgung'),(285,'KV-26860','Phường Long Trường','Phường Long Trường','Thành phố Hồ Chí Minh','TamNgung'),(286,'KV-26876','Phường An Nhơn','Phường An Nhơn','Thành phố Hồ Chí Minh','TamNgung'),(287,'KV-26878','Phường An Hội Đông','Phường An Hội Đông','Thành phố Hồ Chí Minh','TamNgung'),(288,'KV-26882','Phường An Hội Tây','Phường An Hội Tây','Thành phố Hồ Chí Minh','TamNgung'),(289,'KV-26884','Phường Gò Vấp','Phường Gò Vấp','Thành phố Hồ Chí Minh','TamNgung'),(290,'KV-26890','Phường Hạnh Thông','Phường Hạnh Thông','Thành phố Hồ Chí Minh','TamNgung'),(291,'KV-26898','Phường Thông Tây Hội','Phường Thông Tây Hội','Thành phố Hồ Chí Minh','TamNgung'),(292,'KV-26905','Phường Bình Lợi Trung','Phường Bình Lợi Trung','Thành phố Hồ Chí Minh','TamNgung'),(293,'KV-26911','Phường Bình Quới','Phường Bình Quới','Thành phố Hồ Chí Minh','TamNgung'),(294,'KV-26929','Phường Bình Thạnh','Phường Bình Thạnh','Thành phố Hồ Chí Minh','TamNgung'),(295,'KV-26944','Phường Gia Định','Phường Gia Định','Thành phố Hồ Chí Minh','TamNgung'),(296,'KV-26956','Phường Thạnh Mỹ Tây','Phường Thạnh Mỹ Tây','Thành phố Hồ Chí Minh','TamNgung'),(297,'KV-26968','Phường Tân Sơn Nhất','Phường Tân Sơn Nhất','Thành phố Hồ Chí Minh','TamNgung'),(298,'KV-26977','Phường Tân Sơn Hòa','Phường Tân Sơn Hòa','Thành phố Hồ Chí Minh','TamNgung'),(299,'KV-26983','Phường Bảy Hiền','Phường Bảy Hiền','Thành phố Hồ Chí Minh','TamNgung'),(300,'KV-26995','Phường Tân Hòa','Phường Tân Hòa','Thành phố Hồ Chí Minh','TamNgung'),(301,'KV-27004','Phường Tân Bình','Phường Tân Bình','Thành phố Hồ Chí Minh','TamNgung'),(302,'KV-27007','Phường Tân Sơn','Phường Tân Sơn','Thành phố Hồ Chí Minh','TamNgung'),(303,'KV-27013','Phường Tây Thạnh','Phường Tây Thạnh','Thành phố Hồ Chí Minh','TamNgung'),(304,'KV-27019','Phường Tân Sơn Nhì','Phường Tân Sơn Nhì','Thành phố Hồ Chí Minh','TamNgung'),(305,'KV-27022','Phường Phú Thọ Hòa','Phường Phú Thọ Hòa','Thành phố Hồ Chí Minh','TamNgung'),(306,'KV-27028','Phường Phú Thạnh','Phường Phú Thạnh','Thành phố Hồ Chí Minh','TamNgung'),(307,'KV-27031','Phường Tân Phú','Phường Tân Phú','Thành phố Hồ Chí Minh','TamNgung'),(308,'KV-27043','Phường Đức Nhuận','Phường Đức Nhuận','Thành phố Hồ Chí Minh','TamNgung'),(309,'KV-27058','Phường Cầu Kiệu','Phường Cầu Kiệu','Thành phố Hồ Chí Minh','TamNgung'),(310,'KV-27073','Phường Phú Nhuận','Phường Phú Nhuận','Thành phố Hồ Chí Minh','TamNgung'),(311,'KV-27094','Phường An Khánh','Phường An Khánh','Thành phố Hồ Chí Minh','TamNgung'),(312,'KV-27097','Phường Bình Trưng','Phường Bình Trưng','Thành phố Hồ Chí Minh','TamNgung'),(313,'KV-27112','Phường Cát Lái','Phường Cát Lái','Thành phố Hồ Chí Minh','TamNgung'),(314,'KV-27139','Phường Xuân Hòa','Phường Xuân Hòa','Thành phố Hồ Chí Minh','TamNgung'),(315,'KV-27142','Phường Nhiêu Lộc','Phường Nhiêu Lộc','Thành phố Hồ Chí Minh','TamNgung'),(316,'KV-27154','Phường Bàn Cờ','Phường Bàn Cờ','Thành phố Hồ Chí Minh','TamNgung'),(317,'KV-27163','Phường Hòa Hưng','Phường Hòa Hưng','Thành phố Hồ Chí Minh','TamNgung'),(318,'KV-27169','Phường Diên Hồng','Phường Diên Hồng','Thành phố Hồ Chí Minh','TamNgung'),(319,'KV-27190','Phường Vườn Lài','Phường Vườn Lài','Thành phố Hồ Chí Minh','TamNgung'),(320,'KV-27211','Phường Hòa Bình','Phường Hòa Bình','Thành phố Hồ Chí Minh','TamNgung'),(321,'KV-27226','Phường Phú Thọ','Phường Phú Thọ','Thành phố Hồ Chí Minh','TamNgung'),(322,'KV-27232','Phường Bình Thới','Phường Bình Thới','Thành phố Hồ Chí Minh','TamNgung'),(323,'KV-27238','Phường Minh Phụng','Phường Minh Phụng','Thành phố Hồ Chí Minh','TamNgung'),(324,'KV-27259','Phường Xóm Chiếu','Phường Xóm Chiếu','Thành phố Hồ Chí Minh','TamNgung'),(325,'KV-27265','Phường Khánh Hội','Phường Khánh Hội','Thành phố Hồ Chí Minh','TamNgung'),(326,'KV-27286','Phường Vĩnh Hội','Phường Vĩnh Hội','Thành phố Hồ Chí Minh','TamNgung'),(327,'KV-27301','Phường Chợ Quán','Phường Chợ Quán','Thành phố Hồ Chí Minh','TamNgung'),(328,'KV-27316','Phường An Đông','Phường An Đông','Thành phố Hồ Chí Minh','TamNgung'),(329,'KV-27349','Phường Phú Lâm','Phường Phú Lâm','Thành phố Hồ Chí Minh','TamNgung'),(330,'KV-27364','Phường Bình Phú','Phường Bình Phú','Thành phố Hồ Chí Minh','TamNgung'),(331,'KV-27367','Phường Bình Tây','Phường Bình Tây','Thành phố Hồ Chí Minh','TamNgung'),(332,'KV-27373','Phường Bình Tiên','Phường Bình Tiên','Thành phố Hồ Chí Minh','TamNgung'),(333,'KV-27418','Phường Chánh Hưng','Phường Chánh Hưng','Thành phố Hồ Chí Minh','TamNgung'),(334,'KV-27424','Phường Bình Đông','Phường Bình Đông','Thành phố Hồ Chí Minh','TamNgung'),(335,'KV-27427','Phường Phú Định','Phường Phú Định','Thành phố Hồ Chí Minh','TamNgung'),(336,'KV-27439','Phường Bình Hưng Hòa','Phường Bình Hưng Hòa','Thành phố Hồ Chí Minh','TamNgung'),(337,'KV-27442','Phường Bình Tân','Phường Bình Tân','Thành phố Hồ Chí Minh','TamNgung'),(338,'KV-27448','Phường Bình Trị Đông','Phường Bình Trị Đông','Thành phố Hồ Chí Minh','TamNgung'),(339,'KV-27457','Phường Tân Tạo','Phường Tân Tạo','Thành phố Hồ Chí Minh','TamNgung'),(340,'KV-27460','Phường An Lạc','Phường An Lạc','Thành phố Hồ Chí Minh','TamNgung'),(341,'KV-27475','Phường Tân Hưng','Phường Tân Hưng','Thành phố Hồ Chí Minh','TamNgung'),(342,'KV-27478','Phường Tân Thuận','Phường Tân Thuận','Thành phố Hồ Chí Minh','TamNgung'),(343,'KV-27484','Phường Phú Thuận','Phường Phú Thuận','Thành phố Hồ Chí Minh','TamNgung'),(344,'KV-27487','Phường Tân Mỹ','Phường Tân Mỹ','Thành phố Hồ Chí Minh','TamNgung'),(345,'KV-27496','Xã Tân An Hội','Xã Tân An Hội','Thành phố Hồ Chí Minh','TamNgung'),(346,'KV-27508','Xã An Nhơn Tây','Xã An Nhơn Tây','Thành phố Hồ Chí Minh','TamNgung'),(347,'KV-27511','Xã Nhuận Đức','Xã Nhuận Đức','Thành phố Hồ Chí Minh','TamNgung'),(348,'KV-27526','Xã Thái Mỹ','Xã Thái Mỹ','Thành phố Hồ Chí Minh','TamNgung'),(349,'KV-27541','Xã Phú Hòa Đông','Xã Phú Hòa Đông','Thành phố Hồ Chí Minh','TamNgung'),(350,'KV-27544','Xã Bình Mỹ','Xã Bình Mỹ','Thành phố Hồ Chí Minh','TamNgung'),(351,'KV-27553','Xã Củ Chi','Xã Củ Chi','Thành phố Hồ Chí Minh','TamNgung'),(352,'KV-27559','Xã Hóc Môn','Xã Hóc Môn','Thành phố Hồ Chí Minh','TamNgung'),(353,'KV-27568','Xã Đông Thạnh','Xã Đông Thạnh','Thành phố Hồ Chí Minh','TamNgung'),(354,'KV-27577','Xã Xuân Thới Sơn','Xã Xuân Thới Sơn','Thành phố Hồ Chí Minh','TamNgung'),(355,'KV-27592','Xã Bà Điểm','Xã Bà Điểm','Thành phố Hồ Chí Minh','TamNgung'),(356,'KV-27595','Xã Tân Nhựt','Xã Tân Nhựt','Thành phố Hồ Chí Minh','TamNgung'),(357,'KV-27601','Xã Vĩnh Lộc','Xã Vĩnh Lộc','Thành phố Hồ Chí Minh','TamNgung'),(358,'KV-27604','Xã Tân Vĩnh Lộc','Xã Tân Vĩnh Lộc','Thành phố Hồ Chí Minh','TamNgung'),(359,'KV-27610','Xã Bình Lợi','Xã Bình Lợi','Thành phố Hồ Chí Minh','TamNgung'),(360,'KV-27619','Xã Bình Hưng','Xã Bình Hưng','Thành phố Hồ Chí Minh','TamNgung'),(361,'KV-27628','Xã Hưng Long','Xã Hưng Long','Thành phố Hồ Chí Minh','TamNgung'),(362,'KV-27637','Xã Bình Chánh','Xã Bình Chánh','Thành phố Hồ Chí Minh','TamNgung'),(363,'KV-27655','Xã Nhà Bè','Xã Nhà Bè','Thành phố Hồ Chí Minh','TamNgung'),(364,'KV-27658','Xã Hiệp Phước','Xã Hiệp Phước','Thành phố Hồ Chí Minh','TamNgung'),(365,'KV-27664','Xã Cần Giờ','Xã Cần Giờ','Thành phố Hồ Chí Minh','TamNgung'),(366,'KV-27667','Xã Bình Khánh','Xã Bình Khánh','Thành phố Hồ Chí Minh','TamNgung'),(367,'KV-27673','Xã An Thới Đông','Xã An Thới Đông','Thành phố Hồ Chí Minh','TamNgung'),(368,'KV-27676','Xã Thạnh An','Xã Thạnh An','Thành phố Hồ Chí Minh','TamNgung'),(409,'KV-Q760','Quận 1','Quận 1','Thành phố Hồ Chí Minh','HoatDong'),(410,'KV-Q770','Quận 3','Quận 3','Thành phố Hồ Chí Minh','HoatDong'),(411,'KV-Q773','Quận 4','Quận 4','Thành phố Hồ Chí Minh','HoatDong'),(412,'KV-Q774','Quận 5','Quận 5','Thành phố Hồ Chí Minh','HoatDong'),(413,'KV-Q775','Quận 6','Quận 6','Thành phố Hồ Chí Minh','HoatDong'),(414,'KV-Q778','Quận 7','Quận 7','Thành phố Hồ Chí Minh','HoatDong'),(415,'KV-Q776','Quận 8','Quận 8','Thành phố Hồ Chí Minh','HoatDong'),(416,'KV-Q771','Quận 10','Quận 10','Thành phố Hồ Chí Minh','HoatDong'),(417,'KV-Q772','Quận 11','Quận 11','Thành phố Hồ Chí Minh','HoatDong'),(418,'KV-Q761','Quận 12','Quận 12','Thành phố Hồ Chí Minh','HoatDong'),(419,'KV-Q765','Quận Bình Thạnh','Quận Bình Thạnh','Thành phố Hồ Chí Minh','HoatDong'),(420,'KV-Q777','Quận Bình Tân','Quận Bình Tân','Thành phố Hồ Chí Minh','HoatDong'),(421,'KV-Q764','Quận Gò Vấp','Quận Gò Vấp','Thành phố Hồ Chí Minh','HoatDong'),(422,'KV-Q768','Quận Phú Nhuận','Quận Phú Nhuận','Thành phố Hồ Chí Minh','HoatDong'),(423,'KV-Q766','Quận Tân Bình','Quận Tân Bình','Thành phố Hồ Chí Minh','HoatDong'),(424,'KV-Q767','Quận Tân Phú','Quận Tân Phú','Thành phố Hồ Chí Minh','HoatDong'),(425,'KV-Q769','Thành phố Thủ Đức','Thành phố Thủ Đức','Thành phố Hồ Chí Minh','HoatDong'),(426,'KV-Q785','Huyện Bình Chánh','Huyện Bình Chánh','Thành phố Hồ Chí Minh','HoatDong'),(427,'KV-Q787','Huyện Cần Giờ','Huyện Cần Giờ','Thành phố Hồ Chí Minh','HoatDong'),(428,'KV-Q783','Huyện Củ Chi','Huyện Củ Chi','Thành phố Hồ Chí Minh','HoatDong'),(429,'KV-Q784','Huyện Hóc Môn','Huyện Hóc Môn','Thành phố Hồ Chí Minh','HoatDong'),(430,'KV-Q786','Huyện Nhà Bè','Huyện Nhà Bè','Thành phố Hồ Chí Minh','HoatDong');
/*!40000 ALTER TABLE `KhuVuc` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DiaChiKhachHang`
--

DROP TABLE IF EXISTS `DiaChiKhachHang`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `DiaChiKhachHang` (
  `id` int NOT NULL AUTO_INCREMENT,
  `MaDiaChi` varchar(20) NOT NULL,
  `khachHangId` int NOT NULL,
  `khuVucId` int DEFAULT NULL,
  `DiaChiChiTiet` varchar(300) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `DienTichNha` decimal(8,2) DEFAULT NULL,
  `LuuYDacBiet` text,
  `LaMacDinh` bit(1) NOT NULL DEFAULT b'0',
  `TrangThai` varchar(20) NOT NULL DEFAULT 'HoatDong',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_DC_ma` (`MaDiaChi`),
  KEY `idx_DiaChi_KH` (`khachHangId`),
  KEY `idx_DiaChi_KV` (`khuVucId`),
  CONSTRAINT `fk_DC_KH` FOREIGN KEY (`khachHangId`) REFERENCES `KhachHang` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_DC_KV` FOREIGN KEY (`khuVucId`) REFERENCES `KhuVuc` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_DC_dienTich` CHECK (((`DienTichNha` is null) or (`DienTichNha` > 0))),
  CONSTRAINT `chk_DC_trangThai` CHECK ((`TrangThai` in (_utf8mb4'HoatDong',_utf8mb4'DaXoa')))
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DiaChiKhachHang`
--

LOCK TABLES `DiaChiKhachHang` WRITE;
/*!40000 ALTER TABLE `DiaChiKhachHang` DISABLE KEYS */;
INSERT INTO `DiaChiKhachHang` VALUES (1,'DC-625449',1,409,'72 Lê Thánh Tôn, Phường Bến Thành, Thành phố Hồ Chí Minh',85.00,'Nhà có nuôi mèo',_binary '','HoatDong'),(2,'DC-683463',2,409,'100 Nguyễn Huệ, Phường Bến Thành, Thành phố Hồ Chí Minh',NULL,'Giao ban ngày',_binary '','HoatDong'),(3,'DC-003',3,409,'120 Hai Bà Trưng, Phường Bến Nghé, Quận 1, TP.HCM',75.00,'Nh? c? nu?i m?o, c?n th?n c?a ra v?o',_binary '','HoatDong'),(4,'DC-004',4,410,'45 Lê Văn Sỹ, Phường 13, Quận 3, TP.HCM',90.00,'B?m chu?ng c?a t?ng 2',_binary '','HoatDong'),(5,'DC-005',5,410,'88 Nguyễn Thị Minh Khai, Phường 6, Quận 3, TP.HCM',60.00,'G?i xe d??i h?m chung c?',_binary '','HoatDong'),(55,'DC-HMW3OQFQ14',55,419,'23/44 Nguyen Huu Tien, Tay Thanh, Tan Phu, Quận Bình Thạnh, TP. Hồ Chí Minh',60.00,NULL,_binary '','HoatDong'),(104,'DC-HMYKMCS6E0',105,409,'12 Đường thử giao diện',NULL,NULL,_binary '\0','HoatDong');
/*!40000 ALTER TABLE `DiaChiKhachHang` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `KhuVucCTV`
--

DROP TABLE IF EXISTS `KhuVucCTV`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `KhuVucCTV` (
  `id` int NOT NULL AUTO_INCREMENT,
  `MaKhuVucCTV` varchar(20) NOT NULL,
  `congTacVienId` int NOT NULL,
  `khuVucId` int NOT NULL,
  `TrangThai` varchar(20) NOT NULL DEFAULT 'HoatDong',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_KVCV_ma` (`MaKhuVucCTV`),
  UNIQUE KEY `uq_KVCV_pair` (`congTacVienId`,`khuVucId`),
  KEY `idx_KhuVucCTV_CTV` (`congTacVienId`),
  KEY `idx_KhuVucCTV_KV` (`khuVucId`),
  CONSTRAINT `fk_KVCV_CTV` FOREIGN KEY (`congTacVienId`) REFERENCES `CongTacVien` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_KVCV_KV` FOREIGN KEY (`khuVucId`) REFERENCES `KhuVuc` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_KVCV_trangThai` CHECK ((`TrangThai` in (_utf8mb4'HoatDong',_utf8mb4'TamNgung')))
) ENGINE=InnoDB AUTO_INCREMENT=154 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `KhuVucCTV`
--

LOCK TABLES `KhuVucCTV` WRITE;
/*!40000 ALTER TABLE `KhuVucCTV` DISABLE KEYS */;
INSERT INTO `KhuVucCTV` VALUES (1,'KVCTV-C429E87274',52,412,'HoatDong'),(2,'KVCTV-D8CFACE442',53,409,'HoatDong'),(52,'KVCTV-A854784105',105,103,'TamNgung'),(103,'KVCTV-1D341BDA4A',154,425,'HoatDong');
/*!40000 ALTER TABLE `KhuVucCTV` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-11  7:54:33
