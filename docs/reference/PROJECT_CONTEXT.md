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
- The staff workspace is a shared shell in `com.hotel.desktop.nav`: `WorkspaceShell` holds `SidebarPanel` in `BorderLayout.WEST` and a `CardLayout` content area in `BorderLayout.CENTER`. The shared sidebar lives in `nav/SidebarPanel`, not `DashboardScreen.java`.
- Dashboard, room management, room monitoring, housekeeping, the Check-in placeholder, and Check-out preview are content-only `JPanel` instances registered as workspace cards. `HotelDesktop.java` remains the only `JFrame`; welcome and login remain top-level cards.
- Staff navigation switches the visible right-side card through `WorkspaceShell.show(NavItem)`. The shell and screen instances remain mounted, so their UI state is retained when navigating.
- New staff screens must be content-only panels registered in the shell. Do not build a screen-specific sidebar, create another `JFrame`, or rebuild the shared shell for navigation.
- Preserve the single-window navigation pattern: staff navigation buttons should switch only the visible right-side content `JPanel` through the content area's `CardLayout`, not create another `JFrame` or reconstruct the shared shell.

## Staff screen flow

The whole-system flowchart is: Welcome -> staff username/password -> authenticate against the Users table and compare the password hash -> load the staff profile and role -> Home Dashboard. From the dashboard, staff can open room management, room monitoring, housekeeping, walk-in reservations, online reservation review, check-in, and check-out. Logout returns to the start of the staff flow.

Staff sign-in is database-backed in `hotel-core`: the Login screen authenticates the receptionist against the `users` table using a salted PBKDF2-HMAC-SHA256 password hash, then loads the staff profile and role before opening the existing Dashboard. Invalid credentials produce a generic message; database errors are reported separately. The authenticated profile is held by the desktop window for the session and cleared on logout. The staff setup utility creates a receptionist account; no administrator role or permission system is defined. The “Manage rooms” dashboard quick action opens Room Management; other dashboard quick actions navigate to their registered preview/placeholder cards. Check in remains a plain placeholder; Check out is a multi-step UI preview with sample reservations and no persistence. Neither reservation-backed workflow is implemented.

## Dashboard data and terminology

The dashboard is read-only. Its room counts come from the shared database-backed room monitoring snapshot; reservation references, guest names, and reservation statuses remain static preview data.

Use the documented domain vocabulary when wiring real data:

- Room monitoring: Available, Occupied, Dirty (needs cleaning), Under Maintenance, and Out of Service.
- Reservation status: Pending, Confirmed, Checked-In, Checked-Out, and Cancelled.
- Reservation type: Online or Walk-in.
- Online bookings begin as Pending and are reviewed by staff; a pending count in the sidebar refers to that review queue.
- Room monitoring includes counts by room type and rooms needing cleaning, maintenance, or removal from service.
- The mockup's “Cleaning” label is shown as **Dirty** in the prototype because Dirty is the specified housekeeping status.

The original mockup's illustrative room counts are internally consistent: 27 total rooms (8 Single, 12 Double, 7 Deluxe); 12 available rooms (5 Single, 4 Double, 3 Deluxe); 8 occupied, 4 Dirty, 2 under maintenance, and 1 out of service. They are design reference values, not the current database counts.

## UI-only implementation scope

Dashboard counts and Room Monitoring use the immutable snapshot produced by `hotel-core` from room rows in the shared database. Room Management and Housekeeping use `RoomService` for persisted room changes. Authentication uses staff accounts from the shared database; reservation data and reservation-backed check-in/check-out workflows remain unimplemented. The Check-out preview uses illustrative in-memory reservations and does not persist checkout or payment changes.

Room monitoring is read-only and derives status from `rooms.is_occupied` and `rooms.housekeeping_status`; the precedence is Out of Service, Under Maintenance, Dirty, Occupied, then Available. Reservations are not yet used to calculate occupancy. When reservation workflows are implemented, monitoring availability must incorporate reservation dates and the applicable reservation statuses as required by P4.
