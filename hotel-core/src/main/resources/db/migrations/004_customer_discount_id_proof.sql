-- Apply once after migrations 001, 002, and 003 to an existing hotel_reservation database.
USE hotel_reservation;

ALTER TABLE guests
  ADD COLUMN discount_id_proof_path VARCHAR(1000) NULL AFTER discount_id_number;
