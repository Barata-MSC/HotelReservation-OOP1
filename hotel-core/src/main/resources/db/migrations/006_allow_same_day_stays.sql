-- Apply after migration 005 to an existing hotel_reservation database.
-- Allows a same-day walk-in only when its checkout time is later than check-in.
USE hotel_reservation;

ALTER TABLE reservations
  DROP CONSTRAINT chk_reservations_dates,
  ADD CONSTRAINT chk_reservations_datetimes CHECK (
    check_out > check_in
    OR (check_out = check_in AND check_out_time > check_in_time)
  );
