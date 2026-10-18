-- CampusLoop schema (optional: the app also creates these tables automatically)
CREATE DATABASE IF NOT EXISTS campusloop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campusloop;

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  created_at    DATETIME(6)  NOT NULL,
  CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS items (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_id      BIGINT        NOT NULL,
  title         VARCHAR(150)  NOT NULL,
  description   VARCHAR(2000),
  category      VARCHAR(50)   NOT NULL,
  price_per_day DECIMAL(10,2) NOT NULL,
  location      VARCHAR(150),
  available     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at    DATETIME(6)   NOT NULL,
  CONSTRAINT fk_items_owner FOREIGN KEY (owner_id) REFERENCES users(id),
  CONSTRAINT chk_items_price CHECK (price_per_day > 0),
  INDEX idx_items_owner (owner_id),
  INDEX idx_items_category (category),
  INDEX idx_items_title (title)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bookings (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  item_id     BIGINT        NOT NULL,
  renter_id   BIGINT        NOT NULL,
  start_date  DATE          NOT NULL,
  end_date    DATE          NOT NULL,
  total_price DECIMAL(12,2) NOT NULL,
  status      VARCHAR(20)   NOT NULL,
  message     VARCHAR(500),
  created_at  DATETIME(6)   NOT NULL,
  CONSTRAINT fk_bookings_item   FOREIGN KEY (item_id)   REFERENCES items(id),
  CONSTRAINT fk_bookings_renter FOREIGN KEY (renter_id) REFERENCES users(id),
  CONSTRAINT chk_bookings_dates CHECK (end_date >= start_date),
  INDEX idx_bookings_item_status (item_id, status),
  INDEX idx_bookings_renter (renter_id)
) ENGINE=InnoDB;
