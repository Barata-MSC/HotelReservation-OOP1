# Project context

This note records the project scope used when designing the desktop dashboard. It is a reference for future UI work, not a claim that the planned system features are already implemented.

## Source documents reviewed

- `README.md` — architecture, planned desktop screens, room states, reservation states, and module responsibilities.
- [`CS002_Object-Oriented Hotel Reservation System.pdf`](./source-materials/CS002_Object-Oriented%20Hotel%20Reservation%20System.pdf) — course requirements, room monitoring, room and reservation fields, and implementation scope.
- [`Hotel_Reservation_Flowcharts.pdf`](./source-materials/Hotel_Reservation_Flowcharts.pdf) — whole-system flow (page 4), room monitoring (page 8), reservation review (page 16), and check-in/check-out flows (pages 18-19).
- [`03-dashboard 1.png`](./source-materials/03-dashboard%201.png) — visual reference for the desktop dashboard.

Copies of the supplied PDFs and dashboard mockup are kept in `docs/reference/source-materials/` so future work can consult the original materials.

## Application boundaries

- `hotel-desktop` is the staff/receptionist Swing application.
- `hotel-core` is the intended home for shared models, repositories, services, validation, and business rules.
- `hotel-web` is planned for the final phase and is customer-facing only. Staff operations must not be exposed there.
- The desktop dashboard is staff-only navigation and an overview, not a second implementation of room or reservation rules.

## Staff screen flow

The whole-system flowchart is: Welcome -> staff username/password -> authenticate against the Users table and compare the password hash -> load the staff profile and role -> Home Dashboard. From the dashboard, staff can open room management, room monitoring, housekeeping, walk-in reservations, online reservation review, check-in, and check-out. Logout returns to the start of the staff flow.

In the current UI prototype, pressing **Log in** only opens the dashboard so its design can be previewed. Authentication, permissions, persistence, and all dashboard actions are intentionally not implemented. Do not treat that transition as a successful sign-in; replace it with the shared `hotel-core` authentication flow before production use.

## Dashboard data and terminology

The dashboard is a read-only UI preview. Its room counts, room-type availability, reservation references, guest names, and reservation statuses are static sample data, not database results.

Use the documented domain vocabulary when wiring real data:

- Room monitoring: Available, Occupied, Dirty (needs cleaning), Under Maintenance, and Out of Service.
- Reservation status: Pending, Confirmed, Checked-In, Checked-Out, and Cancelled.
- Reservation type: Online or Walk-in.
- Online bookings begin as Pending and are reviewed by staff; a pending count in the sidebar refers to that review queue.
- Room monitoring includes counts by room type and rooms needing cleaning, maintenance, or removal from service.
- The mockup's “Cleaning” label is shown as **Dirty** in the prototype because Dirty is the specified housekeeping status.

The illustrative room counts are internally consistent: 27 total rooms (8 Single, 12 Double, 7 Deluxe); 12 available rooms (5 Single, 4 Double, 3 Deluxe); 8 occupied, 4 Dirty, 2 under maintenance, and 1 out of service.

## UI-only implementation scope

The dashboard intentionally does not query a database, calculate availability, process reservations, validate credentials, or execute room-state transitions. Navigation items and quick-action buttons are visual placeholders until their respective documented screens and core services are implemented.

The room-monitoring screen and dashboard share the same in-memory sample room inventory, so their status counts stay consistent as rooms are cycled between statuses. Changes are not persisted and do not implement the production room-state rules.
