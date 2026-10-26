# Project context

This note records the project scope used when designing the desktop dashboard. It is a reference for future UI work, not a claim that the planned system features are already implemented.

## Source documents reviewed

- `README.md` — architecture, planned desktop screens, room states, reservation states, and module responsibilities.
- [`CS002_Object-Oriented Hotel Reservation System.pdf`](./source-materials/CS002_Object-Oriented%20Hotel%20Reservation%20System.pdf) — course requirements, room monitoring, room and reservation fields, and implementation scope.
- [`Hotel_Reservation_Flowcharts.pdf`](./source-materials/Hotel_Reservation_Flowcharts.pdf) — whole-system flow (page 4), room monitoring (page 8), reservation review (page 16), and check-in/check-out flows (pages 18-19).
- [`03-dashboard 1.png`](./source-materials/03-dashboard%201.png) — visual reference for the desktop dashboard.

Copies of the supplied PDFs and dashboard mockup are kept in `docs/reference/source-materials/` so future work can consult the original materials.

## Application boundaries

- `hotel-desktop` is the Java Swing application for administrator and customer workspaces.
- `hotel-core` is the intended home for shared models, repositories, services, validation, and business rules.
- Customer-facing screens are Java Swing only; there is no HTML/CSS/JavaScript or `hotel-web` module in this project scope.
- The administrator dashboard is staff-only navigation and an overview, not a second implementation of room or reservation rules.

## Desktop window and navigation

- `HotelDesktop.java` is the only `JFrame`; it owns the application window.
- All other desktop screens are `JPanel` instances hosted inside that frame.
- Login routes `Admin` users to the administrator shell and `Customer` users to a separate customer-only panel. Customer navigation must not expose administrator screens.
- The staff workspace is a shared shell in `com.hotel.desktop.nav`: `WorkspaceShell` holds `SidebarPanel` in `BorderLayout.WEST` and a `CardLayout` content area in `BorderLayout.CENTER`. The shared sidebar lives in `nav/SidebarPanel`, not `DashboardScreen.java`.
- Dashboard, room management, room monitoring, housekeeping, reservations, check-in, and check-out are content-only `JPanel` instances registered as administrator workspace cards. The Reservations card contains Walk-in, Online review, and Calendar tabs. `HotelDesktop.java` remains the only `JFrame`; welcome, login, sign-up, the administrator shell, and customer navigation/workspace are hosted as panels in that frame.
- Staff navigation switches the visible right-side card through `WorkspaceShell.show(NavItem)`. The shell and screen instances remain mounted, so their UI state is retained when navigating.
- New staff screens must be content-only panels registered in the shell. Do not build a screen-specific sidebar, create another `JFrame`, or rebuild the shared shell for navigation.
- Preserve the single-window navigation pattern: staff navigation buttons should switch only the visible right-side content `JPanel` through the content area's `CardLayout`, not create another `JFrame` or reconstruct the shared shell.

## Authentication and screen flow

Admin flow: Welcome -> username/password -> authenticate against the Users table and compare the password hash -> load the Admin profile -> administrator dashboard. From there, admins can open room management, room monitoring, housekeeping, the Reservations tabs (walk-in, online review/payment verification, and calendar), check-in, and check-out. Customer flow: Login -> customer email/password or sign up -> load the Customer profile -> customer-only workspace. Customer signup assigns `Customer` in the core; customers cannot enter the administrator shell or invoke staff reservation operations. Logout returns to Welcome.

Authentication and registration are database-backed in `hotel-core`. Passwords use salted PBKDF2-HMAC-SHA256 hashes; customer email is unique; existing `Receptionist` roles migrate to `Admin`. Invalid credentials produce a generic message and database errors are reported separately. The authenticated profile is held by the desktop window for the session and cleared on logout. The administrator setup utility creates an `Admin` account. The login form's customer sign-up switches to a Swing panel and performs database work in a background worker. Customer login opens a separate dashboard, room-booking, payment, booking-history, notification, and receipt workspace. The booking UI uses date/time spinners, and database-backed operations run off the Swing EDT.

## Dashboard data and terminology

The dashboard is read-only. Room counts come from the shared database-backed room monitoring snapshot. Pending online reservations, today's arrivals/departures, and recent reservation rows are loaded from the database; no sample reservation rows are shown. The dashboard refreshes while visible.

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

Walk-in reservations start Confirmed and must check in today; online reservations start Pending and require staff review. Availability covers the complete requested date range and excludes overlapping Pending, Confirmed, or Checked-In reservations. Reservation creation serializes competing bookings by locking the room row. Check-in requires a confirmed reservation whose arrival date has arrived, a clean unoccupied room, and available capacity; it updates reservation and room occupancy in one transaction. Walk-in checkout is available on or after the reservation's checkout date, calculates a stored-rate bill, validates cash, stores payment and receipt details, marks the reservation Checked-Out, and marks the room Dirty in one transaction. Online checkout requires all online payments to be verified as Fully Paid; it does not charge the customer again. Staff can force-check out a checked-in guest with a recorded reason; the room becomes Dirty and the customer is notified. Housekeeping must mark the room Clean before it can be booked again.

Regular guests receive no discount and pay 12% tax. Discounted guests must have a verified discount ID; they receive a 20% discount and are tax-exempt. Cash payments below the amount due are rejected and change is calculated.

The shared `schema.sql` includes the reservation rate/guest-count snapshots, transaction receipt reference, customer account/booking schema, payment records, notifications, discount-ID proof path, room dirty timestamp, and booking-history ledger. Customer online reservations are linked to their customer account and saved as Pending with a customer-entered payment amount; the UI suggests 50% of the total for the initial payment. Payments remain Partially Paid until staff verifies the full amount. Availability is checked over the date range and rechecked inside a transaction while the room row is locked. Payment and discount-ID proofs accept any file type up to 5 MB and are stored under generated names in a configurable directory outside the source tree. Supported payment-proof image types can be previewed in the administrator screen. Discounted guests must submit an ID proof, which remains unverified until staff review. Customer changes and cancellations are limited to Pending reservations. Existing databases apply migrations 001 through 005 once in order; migration 005 creates the booking-history ledger and backfills existing Checked-Out and Cancelled reservations. Legacy records use payment time or creation time as the best available event timestamp because older schemas did not track status-change times. New databases use the current schema directly. A live database connection is still required to exercise persistence at runtime.

The customer dashboard uses the supplied hotel and amenity images. Customer receipts and administrator walk-in reservation receipts support PDF download. The unified administrator Reservations workspace includes online review, payment/discount-ID proof verification, and a calendar. Notifications are generated for booking decisions, payment verification, and check-out. A background task checks at startup and every minute for rooms that have been Dirty for at least one hour, sets them Clean, and refreshes dashboard and room-monitoring data.

Dashboard counts and Room Monitoring use the immutable database snapshot. Room monitoring remains read-only and derives room status from `rooms.is_occupied` and `rooms.housekeeping_status`; the precedence is Out of Service, Under Maintenance, Dirty, Occupied, then Available. The same monitoring page includes rooms regardless of whether their reservations are walk-in or online.
