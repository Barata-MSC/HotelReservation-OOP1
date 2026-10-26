-- Apply once after migrations 001 and 002 to an existing hotel_reservation database.
USE hotel_reservation;

ALTER TABLE users
  MODIFY COLUMN username VARCHAR(254) NOT NULL,
  ADD COLUMN email VARCHAR(255) NULL AFTER full_name,
  ADD COLUMN contact_number VARCHAR(30) NULL AFTER email;

UPDATE users
SET role = 'Admin'
WHERE role = 'Receptionist';

ALTER TABLE users
  ADD UNIQUE KEY uq_users_email (email);

ALTER TABLE rooms
  ADD COLUMN dirty_since DATETIME NULL AFTER is_occupied;

UPDATE rooms
SET dirty_since = CURRENT_TIMESTAMP
WHERE housekeeping_status = 'Dirty';

ALTER TABLE reservations
  ADD COLUMN customer_user_id INT UNSIGNED NULL AFTER guest_id,
  ADD COLUMN check_in_time TIME NOT NULL DEFAULT '15:00:00' AFTER check_in,
  ADD COLUMN check_out_time TIME NOT NULL DEFAULT '11:00:00' AFTER check_out,
  ADD COLUMN payment_status ENUM('Unpaid', 'Partially Paid', 'Fully Paid')
    NOT NULL DEFAULT 'Unpaid' AFTER status,
  ADD COLUMN force_checkout_reason VARCHAR(500) NULL AFTER payment_status,
  ADD KEY ix_reservations_customer (customer_user_id),
  ADD CONSTRAINT fk_reservations_customer_user
    FOREIGN KEY (customer_user_id) REFERENCES users (id) ON DELETE SET NULL;

UPDATE reservations r
JOIN transactions t ON t.reservation_id = r.id
SET r.payment_status = 'Fully Paid'
WHERE t.payment_status = 'Paid';

CREATE TABLE payments (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  reservation_id INT UNSIGNED NOT NULL,
  payment_type ENUM('Downpayment', 'Balance', 'Cash') NOT NULL,
  payment_method ENUM('GCash', 'Bank Transfer', 'Other', 'Cash') NOT NULL,
  amount DECIMAL(10, 2) NOT NULL,
  reference_details VARCHAR(255) NULL,
  proof_file_path VARCHAR(1000) NULL,
  is_verified BOOLEAN NOT NULL DEFAULT FALSE,
  verified_by INT UNSIGNED NULL,
  submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verified_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  KEY ix_payments_reservation (reservation_id),
  CONSTRAINT fk_payments_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE,
  CONSTRAINT fk_payments_verified_by
    FOREIGN KEY (verified_by) REFERENCES users (id) ON DELETE SET NULL,
  CONSTRAINT chk_payments_positive_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
  id INT UNSIGNED NOT NULL AUTO_INCREMENT,
  customer_user_id INT UNSIGNED NOT NULL,
  reservation_id INT UNSIGNED NULL,
  notification_type VARCHAR(50) NOT NULL,
  message VARCHAR(500) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  KEY ix_notifications_customer_unread (customer_user_id, read_at, created_at),
  CONSTRAINT fk_notifications_customer_user
    FOREIGN KEY (customer_user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_notifications_reservation
    FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
