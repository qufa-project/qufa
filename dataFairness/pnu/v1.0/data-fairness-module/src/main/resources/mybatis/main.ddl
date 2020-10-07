CREATE TABLE `authorities` (
                               `username` VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci',
                               `authority` VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci'
)
    COLLATE='utf8_general_ci'
    ENGINE=InnoDB
;

CREATE TABLE `users` (
                         `username` VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci',
                         `password` VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci',
                         `enabled` TINYINT(4) NOT NULL DEFAULT '0',
                         PRIMARY KEY (`username`) USING BTREE
)
    COLLATE='utf8_general_ci'
    ENGINE=InnoDB
;

INSERT INTO `users` VALUES ('admin', '$2a$10$PPO3..2LYlQL2YYMDF6pZ.0eCxzAoe0zZCiZYxf3eOvN6ENwrqbdG', 1);
INSERT INTO `authorities` VALUES ('admin', 'ADMIN');
