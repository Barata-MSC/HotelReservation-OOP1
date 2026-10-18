-- Apply once to a database that already has the legacy reservation tables.
USE hotel_reservation;

ALTER TABLE reservations
  ADD COLUMN room_rate DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER room_number,
  ADD COLUMN guest_count INT NOT NULL DEFAULT 1 AFTER nights;

UPDATE reservations r
JOIN rooms rm ON rm.room_number = r.room_number
SET r.room_rate = rm.price_per_day
WHERE r.room_rate = 0.00;

ALTER TABLE transactions
  ADD COLUMN receipt_reference VARCHAR(30) NULL AFTER reservation_id;

UPDATE transactions
SET receipt_reference = CONCAT('RCT-', reservation_id)
WHERE receipt_reference IS NULL;

ALTER TABLE transactions
  MODIFY COLUMN receipt_reference VARCHAR(30) NOT NULL,
  ADD UNIQUE KEY uq_transactions_receipt_reference (receipt_reference);
