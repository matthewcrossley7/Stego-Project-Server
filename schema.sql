--drop schemas
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `sentimages`;
DROP TABLE IF EXISTS `friends`;

--recreate schemas
CREATE TABLE `user` (
  `userID` int NOT NULL AUTO_INCREMENT,
  `email` varchar(45) DEFAULT NULL,
  `path` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`userID`)
)

CREATE TABLE `friends` (
  `user1ID` int NOT NULL,
  `user2ID` int NOT NULL,
  PRIMARY KEY (`user1ID`,`user2ID`)
)


CREATE TABLE `sentimages` (
  `imageID` int NOT NULL AUTO_INCREMENT,
  `fromUserID` int DEFAULT NULL,
  `toUserID` int DEFAULT NULL,
  `timeSent` datetime DEFAULT NULL,
  `imageName` varchar(45) DEFAULT NULL,
  PRIMARY KEY (`imageID`)
) 


