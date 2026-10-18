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
- Dashboard, room management, room monitoring, housekeeping, walk-in reservation, online reservation review, check-in, and check-out are content-only `JPanel` instances registered as workspace cards. `HotelDesktop.java` remains the only `JFrame`; welcome and login remain top-level cards.
- Staff navigation switches the visible right-side card through `WorkspaceShell.show(NavItem)`. The shell and screen instances remain mounted, so their UI state is retained when navigating.
- New staff screens must be content-only panels registered in the shell. Do not build a screen-specific sidebar, create another `JFrame`, or rebuild the shared shell for navigation.
- Preserve the single-window navigation pattern: staff navigation buttons should switch only the visible right-side content `JPanel` through the content area's `CardLayout`, not create another `JFrame` or reconstruct the shared shell.

## Staff screen flow

The whole-system flowchart is: Welcome -> staff username/password -> authenticate against the Users table and compare the password hash -> load the staff profile and role -> Home Dashboard. From the dashboard, staff can open room management, room monitoring, housekeeping, walk-in reservations, online reservation review, check-in, and check-out. Logout returns to the start of the staff flow.

Staff sign-in is database-backed in `hotel-core`: the Login screen authenticates the receptionist against the `users` table using a salted PBKDF2-HMAC-SHA256 password hash, then loads the staff profile and role before opening the existing Dashboard. Invalid credentials produce a generic message; database errors are reported separately. The authenticated profile is held by the desktop window for the session and cleared on logout. The staff setup utility creates a receptionist account; no administrator role or permission system is defined. Sidebar and dashboard quick actions navigate to the mounted staff screens. Walk-in booking, check-in, and check-out are implemented through the shared reservation service and repository. Online reservations is a placeholder for final implementation; the customer-facing website is not yet available.

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

## Implemented desktop workflow scope

Dashboard counts and Room Monitoring use the immutable snapshot produced by `hotel-core` from room rows in the shared database. Room Management and Housekeeping use `RoomService` for persisted room changes; room occupancy is changed through reservation check-in/check-out, not by room-management edits.

Walk-in reservations start Confirmed and must check in today; online reservations start Pending and require staff review. Availability covers the complete requested date range and excludes overlapping Pending, Confirmed, or Checked-In reservations. Reservation creation serializes competing bookings by locking the room row. Check-in requires a confirmed reservation whose arrival date has arrived, a clean unoccupied room, and available capacity; it updates reservation and room occupancy in one transaction. Checkout is available on or after the reservation's checkout date, calculates a stored-rate bill, validates cash, stores payment and receipt details, marks the reservation Checked-Out, and marks the room Dirty in one transaction. Housekeeping must mark the room Clean before it can be booked again.

Regular guests receive no discount and pay 12% tax. Discounted guests must have a verified discount ID; they receive a 20% discount and are tax-exempt. Cash payments below the amount due are rejected and change is calculated.

The shared `schema.sql` includes the reservation rate/guest-count snapshots and transaction receipt reference. Existing databases with the previous reservation tables must apply `hotel-core/src/main/resources/db/migrations/002_reservation_workflows.sql` once; new databases use the current schema directly. A live database connection is still required to exercise persistence at runtime.

Dashboard counts and Room Monitoring use the immutable database snapshot. Room monitoring remains read-only and derives room status from `rooms.is_occupied` and `rooms.housekeeping_status`; the precedence is Out of Service, Under Maintenance, Dirty, Occupied, then Available. Dashboard reservation summary rows remain illustrative preview data and are not presented as database results.
