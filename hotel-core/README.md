# hotel-core

`hotel-core` is the shared Java domain, persistence, and business-rules module for
the Hotel Reservation Project. The existing Swing application depends on it.
The planned customer website is also intended to use this module rather than
implementing a separate set of reservation rules.

This module is part of the root Maven reactor and currently targets Java 21.
It uses JDBC with the MariaDB driver and stores the shared database schema and
migrations in its resources.

## Responsibilities

- Model rooms, guests, reservations, staff accounts, monitoring snapshots, and
  checkout bills and receipts.
- Validate room and reservation inputs and centralize room and reservation
  workflows.
- Read and write shared data through JDBC repositories.
- Provide database configuration, schema, and SQL migrations.
- Authenticate enabled staff accounts using salted PBKDF2-HMAC-SHA256 password
  hashes.
- Keep persistence workflows and business rules independent of Swing screens.

## Current contents

```text
src/main/java/com/hotel/core/
├── config/       DatabaseConnection
├── exception/    Shared validation, lookup, readiness, and authentication errors
├── model/        Room, Guest, Reservation, staff, monitoring, and checkout records
├── model/enums/  Room, housekeeping, reservation, and guest classifications
├── repository/   JDBC repositories and repository contracts
├── service/      Room, reservation, authentication, and password-hashing logic
└── setup/        StaffAccountSetup utility

src/main/resources/
├── db.properties.example
└── db/
    ├── schema.sql
    └── migrations/
        ├── 001_add_users_is_enabled.sql
        ├── 002_reservation_workflows.sql
        └── 003_discount_id_verification.sql

src/test/java/com/hotel/core/
├── service/      Room, reservation, authentication, and password-hasher tests
└── setup/        Staff account setup tests
```

## Implemented capabilities

- **Rooms:** create, update, find, search, and list rooms; validate room data;
  prevent room-management edits from changing occupancy; and produce a room
  monitoring snapshot from persisted room rows.
- **Reservations:** find rooms available for a date range; create walk-in
  reservations as `Confirmed` and online reservations as `Pending`; review
  pending online reservations; look up reservations and arrivals; and list
  eligible replacement rooms.
- **Booking protection:** check availability across the requested stay,
  consider overlapping `Pending`, `Confirmed`, and `Checked-In` reservations,
  and serialize competing room bookings by locking the room row during
  reservation creation.
- **Check-in:** handle pending-reservation confirmation and availability
  re-checks, room readiness, room replacement, and discount verification as
  part of the reservation workflow.
- **Checkout and billing:** calculate nights from stay dates, use `BigDecimal`
  for monetary values, apply the regular-guest 12% tax or eligible discounted
  guest 20% discount with no tax, reject insufficient cash, calculate change,
  persist payment and receipt details, and mark the room `Dirty` after checkout.
- **Housekeeping support:** enforce room status restrictions and provide
  reservation information for warnings when rooms with upcoming bookings are
  set to `Under Maintenance` or `Out of Service`.
- **Staff authentication:** validate enabled accounts and verify salted
  PBKDF2-HMAC-SHA256 password hashes. The setup utility creates receptionist
  accounts; it does not define an administrator role.

The web application is not implemented yet. Although the core currently
contains service operations for online reservations and their review, the
desktop's online-reservations screen remains a placeholder and there is no
customer website to submit those bookings.

## Architecture and boundaries

```text
hotel-desktop ──┐
                ├──> hotel-core ───> MariaDB / MySQL
hotel-web* ─────┘

* Planned; the hotel-web module and application source have not been created.
```

`hotel-core` owns validation, persistence coordination, and shared business
rules. User interfaces should call its services instead of querying the
database directly or copying reservation and billing logic. The database
schema includes users, rooms, guests, reservations, and transactions.

Not every class uses the same object-oriented form: for example, `Guest` and
`Reservation` are Java records in the current implementation. Use the current
source as the authority when extending models or APIs.

## Local setup

Run Maven commands from the repository root.

1. Use JDK 21 and ensure Maven is available.
2. Create a MariaDB/MySQL database named `hotel_reservation`, or update the
   connection URL to match your local database.
3. Copy the example configuration to the ignored local configuration file:

   ```powershell
   Copy-Item hotel-core\src\main\resources\db.properties.example hotel-core\src\main\resources\db.properties
   ```

4. Set the local database URL, user, and password in
   `hotel-core/src/main/resources/db.properties`. Do not commit credentials.
5. For a new database, apply
   `hotel-core/src/main/resources/db/schema.sql`. For an existing database,
   apply only the migrations it still needs, in order. In particular, apply
   `002_reservation_workflows.sql` before `003_discount_id_verification.sql`
   when upgrading an older reservation schema. Migrations `002` and `003` are
   not idempotent; do not run them more than once. Migration `001` is designed
   to skip its change when the column already exists.

The staff-account setup utility can create an enabled receptionist account
after the database and configuration are ready:

```powershell
mvn -pl hotel-core org.codehaus.mojo:exec-maven-plugin:3.5.0:java "-Dexec.mainClass=com.hotel.core.setup.StaffAccountSetup"
```

It requests a username, display name, and password; passwords must be between
10 and 128 characters. Passwords are stored as hashes, not plaintext.

## Build and test

From the repository root:

```powershell
mvn -pl hotel-core test
mvn -pl hotel-core -am package
mvn clean install
```

The core unit tests cover room and reservation services, authentication,
password hashing, and staff account setup. Database-backed runtime workflows
require a reachable database with the schema and local connection settings
configured.

## Security and implementation notes

- Keep `db.properties` and real database credentials out of version control.
- Repositories use JDBC; use parameterized statements for database inputs.
- Keep staff-only operations in the desktop application. The planned website
  must not expose staff workflows.
- Protect customer reservation lookups so guests cannot access another guest's
  personal data.
- Do not store payment-card details; the implemented checkout workflow accepts
  cash.
