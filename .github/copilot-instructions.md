# Project Instructions

- Before project work, read [`docs/reference/PROJECT_CONTEXT.md`](../docs/reference/PROJECT_CONTEXT.md) for verified requirements, terminology, architecture boundaries, and current UI scope.
- Consult the original course guide, flowcharts, and dashboard mockup in [`docs/reference/source-materials/`](../docs/reference/source-materials/) when a task depends on requirements or screen flow.
- Treat feature checklists as completion trackers, not as proof that code is absent. The desktop application includes database-backed authentication, reservation workflows, customer screens, and dashboard summaries; verify code and tests before describing any capability as complete.
- The proposed `hotel-web` application was cancelled. Keep customer-facing screens in the Java Swing `hotel-desktop` module; do not add a web module or describe the project as having a website.
- Whenever project work changes files, finish with a ready-to-use pull request title and detailed description based on [`.github/PULL_REQUEST_TEMPLATE.md`](PULL_REQUEST_TEMPLATE.md). Include only verified changes and tests; mark tests that could not be run and why. Do not create, push, or open a pull request unless explicitly asked.

# UI Integration Guide: Hotel Reservation System (read before building or wiring any screen)

Do not ask me to upload files. Read them from the repo. Before anything else, read CLAUDE.md, docs/reference/PROJECT_CONTEXT.md and README.md. If anything below disagrees with the code, trust the code and tell me.

## Working mode
- Ask first. Step 1: read the code and reply with findings, the exact files you will add/change/delete, risks and questions. Do NOT edit until I approve. Step 2: implement, then run and report exact results of:
  - mvn -pl hotel-core test
  - mvn -pl hotel-desktop -am test
  (and mvn clean install before a feature is called done).
- Work on a task branch, never main. Keep changes focused. Never claim a check passed if you did not run it.
- Do not mark README checkboxes. Update README/PROJECT_CONTEXT only where statements become false.

## Project map
- hotel-core (com.hotel.core): shared config, models/enums, JDBC repositories, `RoomService`, `ReservationService`, `CustomerReservationService`, `AuthenticationService`, exceptions, setup utility, schema and migrations.
- hotel-desktop (com.hotel.desktop): `HotelDesktop` (the ONLY JFrame), auth/, customer/, dashboard/, nav/, reservation/, checkIn/, checkOut/, roomManagement/, roomMonitoring/, houseKeepingStatus/.
- Customer-facing screens are Java Swing panels in hotel-desktop; do not add a web module or expose staff features in customer navigation.
- ALL business rules live in hotel-core. Swing classes only display and call services. Validate in the core, not just the form.

## Adding or wiring a desktop screen
1. A screen is a content-only JPanel. No new JFrame, no screen-specific sidebar.
2. Add a NavItem value if one is missing (current values: DASHBOARD, ROOM_MANAGEMENT, ROOM_MONITORING, HOUSEKEEPING, RESERVATIONS, CHECK_IN, CHECK_OUT). Walk-in entry, online review/payment verification, and calendar are tabs inside the Reservations panel.
3. Register it with WorkspaceShell.addScreen(NavItem, JPanel) in HotelDesktop; switch with WorkspaceShell.show(NavItem). The shell uses a CardLayout, so screens stay mounted and keep their UI state; do not recreate them to navigate.
4. Create services once in HotelDesktop and pass them to screens through constructors (no new RoomService inside each screen). Screens that must update after another screen changes data should take a Runnable/callback or listen to the shared data source.
5. Do not add screens that show preview/sample data as if it were real results.

## Current core API (verify against the code)
- Room (immutable): roomNumber, floor, roomType, capacity, description, pricePerDay (BigDecimal), occupied (boolean), housekeepingStatus.
- RoomService: addRoom, updateRoom, findByNumber, search(Integer roomNumber, RoomType), findAll, setHousekeepingStatus(roomNumber, status), setOccupied(roomNumber, boolean). Throws SQLException and ValidationException. Maintenance/Out of Service is rejected when a checked-in guest is in the room.
- Database (schema.sql): users, rooms, guests, reservations, transactions, customer payments, notifications, and booking-history data. rooms has is_occupied BOOLEAN and housekeeping_status ENUM('Clean','Dirty','Under Maintenance','Out of Service'). There is NO stored "Available" value.
- Room monitoring status is derived with this precedence: Out of Service > Under Maintenance > Dirty > Occupied > Available. (The monitoring work adds RoomMonitoringStatus, RoomMonitoringSnapshot and a shared RoomMonitoringDataSource; verify they exist before using them, and use them rather than recomputing status in a screen.)
- Reservations: reservation and customer-booking services are implemented in `hotel-core`. Staff and customer bookings check availability for the requested stay; creation rechecks availability while locking the room row. Pending, Confirmed, and Checked-In reservations block overlapping stays. Check-in/check-out update reservation and room state through core workflows. Verify the current service and repository behavior before changing these rules.

## Data and threading pattern for every DB-backed screen
- Use the existing DatabaseConnection and prepared statements only. No credentials in source.
- Never run DB calls on the Swing EDT: use SwingWorker (or equivalent), update the UI on the EDT.
- Every screen needs: loading state, error state with Retry, empty state. Never fall back to fake data.
- Reload when the screen is shown and after other screens change related data.
- Money is BigDecimal. Nights are calculated from dates, never trusted from the form.
- Use the label "Dirty", not "Cleaning". Statuses must not rely on color alone.

## Flowchart map (docs/reference/source-materials/): build screens to match
P1/P19/P20/P21 Room management | P3 Housekeeping | P4 Room monitoring (read-only) | P5 Walk-in reservation (P6 guest, P7 discount ID, P8 dates, P9 availability, P10 save) | P12 Online reservations review | P14 Check in (P22 discount eligibility, P23 room ready) | P15 Check out (P16 bill, P17 cash, P18 receipt).
Billing: Regular 0% discount + 12% tax; Discounted 20% discount + 0% tax; reject cash below total.

## Visual conventions
- Match the existing look: Georgia headings, Segoe UI body, ink (20,18,26), sun yellow (255,204,0), canvas (255,222,62), thick ink outlines, custom-painted rounded components. Fit the 1024x800 workspace to the right of the 256px sidebar without horizontal scroll.
- NetBeans owns .form files: never edit inside GEN-BEGIN/GEN-END regions. Add logic outside them and tell me if a .form change is unavoidable.

## Done checklist
- Business logic in hotel-core with tests (invalid input, ineligible states, overlaps, billing, insufficient cash as applicable).
- No logic duplicated in Swing; no sample data presented as real.
- Navigation still single-window and state-preserving.
- Build/test commands run and reported honestly.

## Housekeeping screen (flowchart P3, updateHousekeepingStatus)
- Staff pick a room (list or room number), see its current status, and set it to Clean, Dirty, Under Maintenance or Out of Service. This is the only screen that changes housekeeping status besides Room Management and check-out.
- Use RoomService.setHousekeepingStatus(roomNumber, status). Show ValidationException and RoomNotFoundException messages to the user; do not catch-and-ignore them.
- Rule already in the core: a room with a checked-in guest cannot be set to Under Maintenance or Out of Service (staff must check the guest out first). Do not re-implement it in Swing.
- P3 also says to warn staff when Pending or Confirmed reservations are ahead for a room being set to Maintenance/Out of Service. Add that as a core method (for example in a service that reads reservations), returning the affected reservations, and let the screen display the warning. Do not query reservations from Swing.
- Housekeeping reads and writes real rooms through RoomService on a background thread, with loading/error/empty states. After a change, refresh Room Monitoring and the Dashboard through the shared data source.
- After check-out a room becomes Dirty. Housekeeping marks it Clean, and only then can it be checked into again (P23).

## Reservation and check-in/check-out workflows (P5, P9, P10, P14, P15)
These workflows are implemented in `hotel-core`; screens should call the existing services rather than duplicate business rules. Verify current behavior and tests before making changes.
- `Guest` is a record containing guest identity, `GuestClassification`, discount ID details, verification state, and optional proof path. Billing uses the verified classification: Regular receives 0% discount and 12% tax; a verified Discounted guest receives 20% discount and 0% tax.
- `ReservationService` handles staff availability, walk-in reservations, reservation review, check-in, check-out, and billing. `CustomerReservationService` handles customer booking, changes, cancellations, payments, and customer booking data. Walk-in reservations start Confirmed; Online reservations start Pending. Stay duration is calculated in the core.
- Overlap rule: existingCheckIn < requestedCheckOut AND existingCheckOut > requestedCheckIn, counting Pending, Confirmed and Checked-In reservations. Rooms Under Maintenance or Out of Service are not bookable.
- Cancellation is allowed only while Pending or Confirmed.

Check-in (P14, checkInGuest):
- Find by reference or guest name. By status: Pending -> confirm first and re-check availability; Checked-In, Checked-Out or Cancelled -> block and show the current status; Confirmed -> continue.
- Block when it is before the arrival date or after the check-out date ("Too early" / "Stay already ended").
- Confirm discount eligibility (P22): if the guest is or asks to be Discounted, a verified ID must be on file or be verified now (P7: ID number and type, name matches, ID not expired). If not verified, treat the guest as Regular (0% / 12%).
- Ensure the room is ready (P23): Clean, not occupied and in service. If not, search for a replacement (P9) or tell staff the guest waits for housekeeping.
- In ONE database transaction: reservation -> Checked-In and room is_occupied = true.

Check-out (P15, checkOutGuest):
- Find the reservation; it must be Checked-In, otherwise show "Guest is not checked in".
- Calculate the bill (P16) in the core with BigDecimal, rounded to 2 decimals:
  nights = checkOut - checkIn; roomCost = rate x nights; discount = roomCost x discountRate; tax = (roomCost - discount) x taxRate; total = roomCost - discount + tax.
  Example: rate 2,500.00 x 3 nights -> Regular 8,400.00; Discounted 6,000.00.
- Cash payment (P17): cash only. Reject a non-positive, invalid or insufficient amount BEFORE anything is saved, and allow retry. change = cashTendered - total.
- On success, save the transaction (room_rate, nights, room_cost, discount_amount, tax_amount, total_amount, cash_tendered, change_amount, payment_status Paid, paid_at) and in the same database transaction set reservation -> Checked-Out, room is_occupied = false and housekeeping -> Dirty.
- If payment is not accepted, checkout is not completed and the reservation stays Checked-In.
- Receipt (P18): guest name and classification, room number and type, check-in and check-out dates, nights, rate per night, room cost, discount, tax, total, cash tendered, change, reservation reference.
- Amounts have no currency symbol (the project documents name none).

Screens:
- Reservations (walk-in, online review/payment verification, and calendar tabs), Check in (CHECK_IN), and Check out (CHECK_OUT) stay as content-only panels registered in the shell. They show validation messages from the core, use background threads for database work, and refresh Room Monitoring, Dashboard, and Housekeeping after any change that affects rooms.
- Room Monitoring derives status from the shared room data and stored occupancy/housekeeping values. Do not introduce a second status calculation in a screen.
- Dashboard room and reservation summaries are loaded from the database; do not reintroduce sample reservation rows or a hard-coded online-reservation badge.