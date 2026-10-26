-- Apply after migrations 001 through 004 to an existing hotel_reservation database.
USE hotel_reservation;

CREATE TABLE IF NOT EXISTS booking_history_ledger (
  ledger_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reservation_id INT UNSIGNED NOT NULL,
  reservation_reference VARCHAR(30) NOT NULL,
  guest_name VARCHAR(150) NOT NULL,
  room_number INT NOT NULL,
  check_in DATE NOT NULL,
  check_in_time TIME NOT NULL,
  check_out DATE NOT NULL,
  check_out_time TIME NOT NULL,
  guest_count INT NOT NULL,
  reservation_type VARCHAR(30) NOT NULL,
  reservation_status VARCHAR(30) NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  event_note VARCHAR(500) NULL,
  event_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  recorded_by INT UNSIGNED NULL,
  PRIMARY KEY (ledger_id),
  KEY ix_booking_history_event_at (event_at),
  KEY ix_booking_history_status_time (reservation_status, event_at),
  KEY ix_booking_history_reference (reservation_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Existing reservations do not store status-change timestamps. Use the payment timestamp
-- for checked-out stays when available, otherwise created_at as the best available date.
INSERT IGNORE INTO booking_history_ledger (
  reservation_id, reservation_reference, guest_name, room_number,
  check_in, check_in_time, check_out, check_out_time, guest_count,
  reservation_type, reservation_status, event_type, event_note, event_at
)
SELECT r.id, r.reference, g.full_name, r.room_number,
       r.check_in, r.check_in_time, r.check_out, r.check_out_time, r.guest_count,
       r.reservation_type, r.status, r.status,
       'Imported during booking history setup; original status-change time was unavailable.',
       CASE WHEN r.status = 'Checked-Out'
            THEN COALESCE(t.paid_at, r.created_at)
            ELSE r.created_at END
FROM reservations r
JOIN guests g ON g.id = r.guest_id
LEFT JOIN transactions t ON t.reservation_id = r.id
WHERE r.status IN ('Checked-Out', 'Cancelled')
AND NOT EXISTS (
  SELECT 1 FROM booking_history_ledger h
  WHERE h.reservation_id = r.id AND h.event_type = r.status
);
