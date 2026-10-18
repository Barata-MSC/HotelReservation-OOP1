CREATE DATABASE IF NOT EXISTS hotel_reservation
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE hotel_reservation;

CREATE TABLE IF NOT EXISTS users (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(100) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(150) NOT NULL,
  role VARCHAR(50) NOT NULL,
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rooms (
  room_number INT NOT NULL,
  floor INT NOT NULL,
  room_type ENUM('Single', 'Double', 'Deluxe') NOT NULL,
  capacity INT NOT NULL,
  description VARCHAR(500) NOT NULL,
  price_per_day DECIMAL(10, 2) NOT NULL,
  is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
  housekeeping_status ENUM(
    'Clean',
    'Dirty',
    'Under Maintenance',
    'Out of Service'
  ) NOT NULL DEFAULT 'Clean',
  PRIMARY KEY (room_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS guests (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  full_name VARCHAR(150) NOT NULL,
  contact_number VARCHAR(30) NOT NULL,
  email VARCHAR(255) NOT NULL,
  classification ENUM('Regular', 'Discounted') NOT NULL,
  discount_id_type VARCHAR(50),
  discount_id_number VARCHAR(100),
  discount_verified BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reservations (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  reference VARCHAR(30) NOT NULL,
  guest_id INT UNSIGNED NOT NULL,
  room_number INT NOT NULL,
  room_rate DECIMAL(10, 2) NOT NULL,
  check_in DATE NOT NULL,
  check_out DATE NOT NULL,
  nights INT NOT NULL,
  guest_count INT NOT NULL DEFAULT 1,
  reservation_type ENUM('Online', 'Walk-in') NOT NULL,
  status ENUM(
    'Pending',
    'Confirmed',
    'Checked-In',
    'Checked-Out',
    'Cancelled'
  ) NOT NULL DEFAULT 'Pending',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_reservations_reference (reference),
  KEY ix_reservations_room_dates (room_number, check_in, check_out),
  CONSTRAINT fk_reservations_guest
    FOREIGN KEY (guest_id) REFERENCES guests (id),
  CONSTRAINT fk_reservations_room
    FOREIGN KEY (room_number) REFERENCES rooms (room_number),
  CONSTRAINT chk_reservations_dates CHECK (check_out > check_in)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  reservation_id INT UNSIGNED NOT NULL,
  receipt_reference VARCHAR(30) NOT NULL,
  room_rate DECIMAL(10, 2) NOT NULL,
  nights INT NOT NULL,
  room_cost DECIMAL(10, 2) NOT NULL,
  discount_amount DECIMAL(10, 2) NOT NULL,
  tax_amount DECIMAL(10, 2) NOT NULL,
  total_amount DECIMAL(10, 2) NOT NULL,
  cash_tendered DECIMAL(10, 2) NOT NULL,
  change_amount DECIMAL(10, 2) NOT NULL,
  payment_status ENUM('Paid', 'Unpaid') NOT NULL DEFAULT 'Unpaid',
  paid_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uq_transactions_receipt_reference (receipt_reference),
  UNIQUE KEY uq_transactions_reservation (reservation_id),
  CONSTRAINT fk_transactions_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample rooms: 8 Single, 12 Double, and 7 Deluxe rooms on floors 1-3.
INSERT INTO rooms (
  room_number, floor, room_type, capacity, description, price_per_day,
  is_occupied, housekeeping_status
) VALUES
  (101, 1, 'Single', 1, 'Single room on floor 1', 100.00, FALSE, 'Clean'),
  (102, 1, 'Single', 1, 'Single room on floor 1', 100.00, FALSE, 'Clean'),
  (103, 1, 'Single', 1, 'Single room on floor 1', 100.00, FALSE, 'Clean'),
  (104, 1, 'Double', 2, 'Double room on floor 1', 150.00, FALSE, 'Clean'),
  (105, 1, 'Double', 2, 'Double room on floor 1', 150.00, FALSE, 'Clean'),
  (106, 1, 'Double', 2, 'Double room on floor 1', 150.00, FALSE, 'Clean'),
  (107, 1, 'Double', 2, 'Double room on floor 1', 150.00, FALSE, 'Clean'),
  (108, 1, 'Deluxe', 2, 'Deluxe room on floor 1', 250.00, FALSE, 'Clean'),
  (109, 1, 'Deluxe', 2, 'Deluxe room on floor 1', 250.00, FALSE, 'Clean'),
  (201, 2, 'Single', 1, 'Single room on floor 2', 100.00, FALSE, 'Clean'),
  (202, 2, 'Single', 1, 'Single room on floor 2', 100.00, FALSE, 'Clean'),
  (203, 2, 'Single', 1, 'Single room on floor 2', 100.00, FALSE, 'Clean'),
  (204, 2, 'Double', 2, 'Double room on floor 2', 150.00, FALSE, 'Clean'),
  (205, 2, 'Double', 2, 'Double room on floor 2', 150.00, FALSE, 'Clean'),
  (206, 2, 'Double', 2, 'Double room on floor 2', 150.00, FALSE, 'Clean'),
  (207, 2, 'Double', 2, 'Double room on floor 2', 150.00, FALSE, 'Clean'),
  (208, 2, 'Double', 2, 'Double room on floor 2', 150.00, FALSE, 'Clean'),
  (209, 2, 'Deluxe', 2, 'Deluxe room on floor 2', 250.00, FALSE, 'Clean'),
  (210, 2, 'Deluxe', 2, 'Deluxe room on floor 2', 250.00, FALSE, 'Clean'),
  (211, 2, 'Deluxe', 2, 'Deluxe room on floor 2', 250.00, FALSE, 'Clean'),
  (212, 2, 'Deluxe', 2, 'Deluxe room on floor 2', 250.00, FALSE, 'Clean'),
  (301, 3, 'Single', 1, 'Single room on floor 3', 100.00, FALSE, 'Clean'),
  (302, 3, 'Single', 1, 'Single room on floor 3', 100.00, FALSE, 'Clean'),
  (303, 3, 'Double', 2, 'Double room on floor 3', 150.00, FALSE, 'Clean'),
  (304, 3, 'Double', 2, 'Double room on floor 3', 150.00, FALSE, 'Clean'),
  (305, 3, 'Double', 2, 'Double room on floor 3', 150.00, FALSE, 'Clean'),
  (306, 3, 'Deluxe', 2, 'Deluxe room on floor 3', 250.00, FALSE, 'Clean');
