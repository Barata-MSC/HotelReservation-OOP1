# hotel-desktop

`hotel-desktop` is the staff and receptionist application for the Hotel
Reservation Project. It is a Java Swing module that depends on `hotel-core` for
shared database access, validation, authentication, and hotel workflows.

The desktop application is part of the root Maven reactor and currently targets
Java 21.

## Purpose and architecture

```text
HotelDesktop (single JFrame)
├── Welcome and Login cards
└── WorkspaceShell
    ├── Shared SidebarPanel
    └── CardLayout workspace screens
         └── hotel-core services ──> MariaDB / MySQL
```

`HotelDesktop` owns the only top-level `JFrame`. Welcome and login are cards in
that window. After staff authentication, the shared `WorkspaceShell` presents
the workspace sidebar and registered content panels. Navigation switches
between mounted panels, preserving their screen instances and UI state.

Swing panels are responsible for displaying information and collecting user
input. Shared business rules and persistence belong in `hotel-core`; screens
receive its services rather than defining their own domain logic.

## Current screens and features

| Area | Purpose and current scope |
|---|---|
| Welcome and Login | Opens the staff sign-in flow and authenticates against the shared users table through `AuthenticationService`. |
| Dashboard | Provides staff navigation and room counts from the core room-monitoring snapshot. Reservation summary rows are static illustrative preview data, not live reservations. |
| Room Management | Creates, edits, searches, and displays rooms through `RoomService`; occupancy is controlled by reservation check-in and check-out. |
| Room Monitoring | Displays a read-only room view based on shared monitoring data. Status precedence is Out of Service, Under Maintenance, Dirty, Occupied, then Available. |
| Housekeeping | Updates room housekeeping status through `RoomService` and warns about affected upcoming reservations where applicable. |
| Walk-in Reservation | Creates walk-in reservations through `ReservationService`; walk-in bookings start Confirmed and must start today. |
| Check-in | Looks up and processes reservations through the shared reservation service, including room readiness and eligible replacement-room handling. |
| Check-out | Processes checkout through the core service, including the bill, cash payment, change, receipt, and room status update. |
| Online Reservations | **Placeholder only.** The panel says it is reserved for final implementation; it does not provide online reservation review. |

Reservation creation checks date-range availability and overlapping active
reservations. Check-in and checkout update reservation and room state through
core workflows. Checkout requires a checked-in reservation, validates the cash
payment, and leaves the room Dirty for housekeeping. A room must be marked
Clean before it can be booked again.

## Source layout

```text
src/main/java/com/hotel/desktop/
├── HotelDesktop.java              Main window and service/screen wiring
├── auth/                          Welcome and login screens
├── checkIn/                       Check-in screen
├── checkOut/                      Checkout screen
├── dashboard/                     Dashboard and dashboard UI components
├── houseKeepingStatus/            Housekeeping screen
├── nav/                           Navigation enum, sidebar, workspace shell
├── reservation/                   Walk-in screen and online placeholder
├── roomManagement/                Room management screen
├── roomMonitoring/                Monitoring screen, components, shared data source
└── ui/                            Shared staff UI helpers

src/main/resources/
└── com/hotel/desktop/auth/        Welcome-screen image resource
```

The `.form` files are maintained by the NetBeans GUI builder. Keep generated
regions in those files intact when changing the UI.

## Run locally

The desktop app needs the core database configuration and schema described in
[`hotel-core`](../hotel-core/README.md). From the repository root, build and
test this module together with its core dependency:

```powershell
mvn -pl hotel-desktop -am test
```

To launch the UI, open the root Maven project in NetBeans or another Java IDE
and run `com.hotel.desktop.HotelDesktop` from the `hotel-desktop` module.
Database-backed workflows require a reachable configured database. Set up an
enabled receptionist account with the `hotel-core` staff account setup utility
before attempting database-backed sign-in.

## Important scope notes

- The desktop provides staff-facing workflows only. It is not the customer
  website.
- The Online Reservations screen is a placeholder, and the website submission
  flow is not available.
- Dashboard room counts use database-backed monitoring data, but its
  reservation summary table remains sample preview content.
- Sign-in uses the core's hashed-password authentication. The welcome screen
  is an entry point, not proof of authentication; the dashboard is shown after
  valid credentials are accepted.
- Database operations and business rules should remain in `hotel-core`, not be
  duplicated in Swing screens.
