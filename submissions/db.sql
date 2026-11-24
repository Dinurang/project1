-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS suntravels;
USE suntravels;

-- 1. Create 'user' table (Master authentication & role table)
CREATE TABLE user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- 2. Create 'contract_administrator' table (1:1 with user)
CREATE TABLE contract_administrator (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    employee_code VARCHAR(50) NOT NULL UNIQUE,
    contact_no VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_admin_user 
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Create 'call_center_agent' table (1:1 with user)
CREATE TABLE call_center_agent (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    agent_code VARCHAR(50) NOT NULL UNIQUE,
    desk_extension VARCHAR(20),
    PRIMARY KEY (id),
    CONSTRAINT fk_agent_user 
        FOREIGN KEY (user_id) REFERENCES user(id)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Create 'contract' table (1:N with contract_administrator)
CREATE TABLE contract (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    hotel_name VARCHAR(255) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    markup DECIMAL(6, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contract_admin 
        FOREIGN KEY (admin_id) REFERENCES contract_administrator(id)
) ENGINE=InnoDB;

-- 5. Create 'room_type' table (1:N with contract)
CREATE TABLE room_type (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contract_id BIGINT NOT NULL,
    type_name VARCHAR(255) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    no_of_rooms INT NOT NULL,
    max_adults INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_room_type_contract 
        FOREIGN KEY (contract_id) REFERENCES contract(id) 
        ON DELETE CASCADE
) ENGINE=InnoDB;