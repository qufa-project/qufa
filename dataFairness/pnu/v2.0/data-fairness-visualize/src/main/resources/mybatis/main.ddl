CREATE TABLE `authorities`
(
    `username`  VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci',
    `authority` VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci'
) COLLATE='utf8_general_ci'
    ENGINE=InnoDB
;

CREATE TABLE `users`
(
    `username` VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci',
    `password` VARCHAR(255) NOT NULL DEFAULT '' COLLATE 'utf8_general_ci',
    `enabled`  TINYINT(4) NOT NULL DEFAULT '0',
    PRIMARY KEY (`username`) USING BTREE
) COLLATE='utf8_general_ci'
    ENGINE=InnoDB
;

INSERT INTO `users`
VALUES ('admin', '$2a$10$PPO3..2LYlQL2YYMDF6pZ.0eCxzAoe0zZCiZYxf3eOvN6ENwrqbdG', 1);
INSERT INTO `authorities`
VALUES ('admin', 'ADMIN');

CREATE TABLE
    data_meta
(
    tablename VARCHAR(255) NOT NULL COMMENT '테이블이름',
    order0    VARCHAR(255) COMMENT '2진컬럼이름',
    order1    VARCHAR(255) COMMENT '표준편차1컬럼이름',
    order2    VARCHAR(255) COMMENT '표준편차2컬럼이름',
    order3    VARCHAR(255) COMMENT '표준편차3컬럼이름',
    order4    VARCHAR(255) COMMENT '표준편차4컬럼이름',
    order5    VARCHAR(255) COMMENT '표준편차5컬럼이름',
    stddev0   VARCHAR(255) COMMENT '2진컬럼표준편차',
    stddev1   VARCHAR(255) COMMENT '표준편차1값',
    stddev2   VARCHAR(255) COMMENT '표준편차2값',
    stddev3   VARCHAR(255) COMMENT '표준편차3값',
    stddev4   VARCHAR(255) COMMENT '표준편차4값',
    stddev5   VARCHAR(255) COMMENT '표준편차5값',
    stddev    VARCHAR(255) COMMENT '전체표준편차',
    PRIMARY KEY (tablename)
)
    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_general_ci;

CREATE TABLE
    data_structure
(
    id        INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
    tablename VARCHAR(255),
    dataPath  VARCHAR(10000),
    COUNT     INT(10) UNSIGNED,
    columns   VARCHAR(255),
    PRIMARY KEY (id)
)
    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 DEFAULT COLLATE=utf8mb4_general_ci;