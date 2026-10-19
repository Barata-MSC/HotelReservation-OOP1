-- Apply once to a database that already has the reservation workflow schema.
USE hotel_reservation;

ALTER TABLE guests
  ADD COLUMN discount_id_expiry DATE NULL AFTER discount_verified,
  ADD COLUMN discount_name_matches BOOLEAN NOT NULL DEFAULT FALSE AFTER discount_id_expiry;
