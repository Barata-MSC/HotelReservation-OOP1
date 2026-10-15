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

## Desktop window and navigation

- `HotelDesktop.java` is the only `JFrame`; it owns the application window.
- All other desktop screens are `JPanel` instances hosted inside that frame.
- After staff enters the dashboard workspace, keep the left-side navigation from `DashboardScreen.java` mounted as a constant shared sidebar. Place the dashboard, room management, room monitoring, housekeeping, and other staff UIs in the right-side content area; navigation switches only that content area.
- Treat staff screens as right-side content panels, not complete dashboard replacements. Do not duplicate or recreate the sidebar when adding or modifying a screen. Keep navigation, profile, and logout controls in the shared shell; change them only when an explicit navigation requirement calls for it.
- Use a `CardLayout` for the right-side content panels and switch cards when navigation buttons are clicked. Do not create a new frame or rebuild/reload the shared shell for navigation; retain the shell and each screen's state.
- This is the target workspace behavior. The current prototype still swaps whole screens in `HotelDesktop.java`, and some screens create their own sidebar; those screens must be brought into the shared-shell pattern as they are updated.
- Keep the NetBeans `.form` files alongside their Java screens. Do not delete or remove them as part of UI changes; they are part of the desktop project's GUI Builder setup.
- Preserve the single-window navigation pattern: staff navigation buttons should switch only the visible right-side content `JPanel` through the content area's `CardLayout`, not create another `JFrame` or reconstruct the shared shell.
- When changing a screen managed by the GUI Builder, keep its `.form` and Java source synchronized so the form can continue to be opened and edited safely.

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
