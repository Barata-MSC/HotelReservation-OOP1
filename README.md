# Object-Oriented Hotel Reservation System

A Java 21 Object-Oriented Hotel Reservation System delivered as a Maven multi-module desktop application backed by a shared MariaDB/MySQL database:

- **`hotel-desktop`** — the Java Swing application for both administrators and customers. Separate role-specific workspaces are hosted in the same `JFrame`.
- **`hotel-core`** — shared Java code containing the models, repositories, services, configuration, exceptions, database schema, and all business rules.

The desktop application connects to the shared database and calls `hotel-core`. The proposed `hotel-web` application was cancelled and is out of scope: customer-facing booking and payment screens are part of the Java Swing desktop application. This repository has no web frontend, web server, or `hotel-web` module.

> **Project status:** The repository implements Java Swing desktop workflows for role-aware Admin/Customer authentication, customer booking and payment-proof screens, administrator reservation management, booking history, database-backed dashboard summaries, and room dirty-time reset. The current schema is in `hotel-core/src/main/resources/db/schema.sql`; existing databases may need migrations 001–006 applied in order, depending on their starting schema. Passwords are stored as PBKDF2 hashes. Database-backed runtime behavior requires local database configuration and the appropriate schema. The feature checklist below is a completion tracker; its boxes remain unchecked until each item has been fully implemented and verified.

## Contents

- [Project Goals](#project-goals)
- [Feature Checklist](#feature-checklist)
- [Customer Workspace](#customer-workspace)
- [Desktop Screens](#desktop-screens)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Core OOP Design](#core-oop-design)
- [Business Rules](#business-rules)
- [Prerequisites](#prerequisites)
- [Local Setup](#local-setup)
- [Run and Test](#run-and-test)
- [GitHub Team Workflow](#github-team-workflow)
- [Security Notes](#security-notes)
- [Project Documentation](#project-documentation)

## Project Goals

The project applies object-oriented programming concepts to a hotel reservation system through a shared Java core and separate administrator and customer workspaces in one desktop application.

1. **Keep shared logic in the core.** OOP models, repository operations, services, exceptions, database configuration, schema, and business rules belong in `hotel-core`.
2. **Provide staff workflows in the desktop application.** Administrators manage rooms and reservations, monitor rooms, handle check-in/check-out, and maintain housekeeping through Java Swing screens in `hotel-desktop`.
3. **Provide customer workflows in the desktop application.** Customer-facing booking, payment, booking-history, notification, and receipt screens are Java Swing panels in `hotel-desktop`; a web application is not part of the project.
4. **Share one database.** Administrator and customer workspaces use the same MariaDB/MySQL database so room and reservation records remain consistent.
5. **Keep online booking separate from walk-in operations.** Online reservations use `ReservationType = Online` and start with status `Pending`; administrators review them in the desktop application's Reservations workspace.

The customer dashboard, date/time room search, pending online booking, customer-entered partial payments (with 50% suggested), proof submission, booking history, notifications, and PDF receipt screens are implemented in code. Administrators review online bookings and payments in the Reservations workspace; the dashboard's reservation summary is database-driven. Database-backed behavior requires a configured MariaDB/MySQL database with a schema version compatible with the application.

Administrators can review archived checkout, cancellation, and dispute events from the Booking history sidebar page. Its date filters cover the past seven calendar days, today, the current month, and the current year. Existing terminal reservations are backfilled by migration 005; because older records do not store status-change times, the migration uses the transaction payment time for checked-out stays when available and otherwise uses reservation creation time.

## Feature Checklist

These checklists track end-to-end completion, not whether code exists. Keep an item unchecked until its implementation and verification are complete; some listed capabilities already have code, but database-backed behavior still depends on the configured environment.

### Shared core (`hotel-core`)

- [ ] Define the shared OOP models, enums, and exceptions.
- [ ] Implement database connection configuration and repository operations.
- [ ] Implement room, reservation, guest, user, and transaction data access.
- [ ] Implement room management, availability, reservation, authentication, and billing services.
- [ ] Store the shared database schema in `hotel-core/src/main/resources/db/schema.sql`.
- [ ] Keep all business rules in this module so neither application implements a separate copy.

### Staff desktop application (`hotel-desktop`)

- [ ] Provide role-restricted Admin login backed by the users table and hashed passwords.
- [ ] Add, update, search, and display hotel rooms.
- [ ] Manage room availability and housekeeping status.
- [ ] Create and manage **walk-in reservations**.
- [ ] Display and manage online reservations submitted through the customer workspace.
- [ ] Check guests in and out.
- [ ] Calculate bills, process cash payments, calculate change, and generate receipts.
- [ ] Monitor available and occupied rooms, room types, cleaning needs, maintenance, and out-of-service rooms.
- [ ] Call the shared business logic in `hotel-core` rather than duplicating it in the desktop UI.

### Customer workspace (`hotel-desktop`, Java Swing)

- [ ] Browse rooms and view room details, rates, and capacity.
- [ ] Search for rooms using check-in and check-out dates and guest requirements.
- [ ] Enter guest details and submit an online reservation.
- [ ] Save online reservations with reservation type `Online` and initial status `Pending`.
- [ ] Display a reservation reference and current booking status after submission.
- [ ] Allow a guest to view or cancel a reservation only when supported by the final system rules and access checks.
- [ ] Provide a customer dashboard and booking navigation in the shared single-window application.
- [ ] Use `hotel-core` for availability checks and reservation business rules.

## Customer Workspace

Customer-facing screens are Java Swing `JPanel` cards hosted by `HotelDesktop`. These implemented customer pages are hosted in the Swing desktop application; staff operations remain restricted to the administrator workspace. Their database-backed behavior requires a valid local database configuration.

### Customer pages

| Page | Purpose |
|---|---|
| **Dashboard** | Show the supplied hotel and amenity images and provide an entry point to room search. |
| **Book a Room** | Collect guest details, date/time spinners, room type, and guest count; show rooms available for the full date range. |
| **Payment** | Enter a payment amount (the suggested initial payment is 50% of the booking total) or submit part/all of the remaining balance with a method, reference, and bounded proof upload. Payments remain Partially Paid until staff verifies the full amount. |
| **My Bookings** | Show the signed-in customer's reservations and allow pending-only changes/cancellation and eligible balance payments. |
| **Notifications** | Show customer-scoped booking notifications and unread counts. |
| **Receipt** | Show a reservation/payment summary and download it as a PDF. |

## Desktop Screens

The Java Swing application is developed in `hotel-desktop`. It has Welcome and Login top-level cards, a role-restricted administrator workspace, and a separate customer workspace hosted by the same `JFrame`. Admin login uses salted password hashes; customer sign-up and booking/payment screens are implemented. The administrator Reservations workspace contains Walk-in, Online review, and Calendar tabs. Dashboard room counts and recent reservation summaries are loaded from the database.

| Screen | Purpose |
|---|---|
| **Login / Sign up** | Authenticate Admin and Customer accounts and create customer accounts using the users table and hashed passwords. |
| **Customer workspace** | Give customers a separate dashboard, booking, payment, booking-history, notification, and receipt navigation that cannot open staff screens. |
| **Dashboard** | Provide an administrator overview and navigation to operational modules. |
| **Room Management** | Add, edit, search, and view room records. |
| **Reservation Management** | Create walk-in reservations and review/manage online reservations, including their permitted status changes. |
| **Check-in / Check-out** | Process arrivals and departures and update reservation and room status. |
| **Billing and Receipt** | Calculate the bill using the specified guest-classification rules, accept cash payment, calculate change, and generate a receipt. |
| **Room Monitoring** | Read-only display of available and occupied rooms, availability by room type, Dirty rooms, maintenance, and out-of-service rooms. |
| **Housekeeping** | Update room status to Clean, Dirty, Under Maintenance, or Out of Service. |

The desktop application calls `hotel-core` for reservation, availability, authentication, billing, and other business rules. UI classes should not contain a second implementation of those rules.

The shared administrator shell switches among mounted workspace cards. Customer accounts route to a separate customer-only navigation shell; administrator navigation checks the authenticated role. Welcome, login, sign-up, and both workspaces are panels in the same `JFrame`. Customer reservation persistence, room-row locking, proof-file storage, customer booking history, and customer notifications are implemented through `hotel-core`. The Reservations workspace includes walk-in, online review/payment verification, and calendar tabs. Admin dashboard reservation summaries are database-driven. Admin checkout stores walk-in payment details and marks the room Dirty; online checkout requires fully verified online payments. Customer and walk-in reservation receipts can be downloaded as PDFs. See [`docs/reference/PROJECT_CONTEXT.md`](docs/reference/PROJECT_CONTEXT.md) for workflow rules and source-document alignment notes.

## Technology Stack

| Area | Technology | Purpose |
|---|---|---|
| IDEs | Apache NetBeans and Visual Studio Code | Team development, editing, and debugging |
| Language | Java 17 or later; JDK 21 is installed for development | Shared application logic and OOP models |
| Build tool | Apache Maven multi-module project | Builds modules and manages dependencies |
| Parent project | Root `pom.xml`, group ID `com.hotel` | Defines the project and its modules |
| Shared module | `hotel-core` | Models, repositories, services, exceptions, configuration, schema, and business rules |
| Desktop UI | Java Swing | Administrator workflows and customer screens |
| Database access | JDBC through shared repository/configuration code | Database operations used by both workspaces |
| Database | MariaDB or MySQL | Shared persistence for rooms, guests, reservations, users, and transactions |
| Local database environment | XAMPP, if using its bundled MariaDB/MySQL service | Local development database |
| Version control | Git and GitHub | Branches, collaboration, reviews, and project history |

**Optional development tools:** Jeddict AI may assist with coding, but generated code must be reviewed and tested. NetBeans Database Manager can help inspect database connections. Cygwin is not normally required for the Git/Maven workflow; Git Bash, PowerShell, or a suitable IDE terminal can be used. FlatLaf is an optional look-and-feel choice for the Swing desktop UI. A Swing-compatible date-picker component is also optional; select one only if the standard date-input controls are insufficient for the desktop design. These tools are not substitutes for the shared business logic in `hotel-core`.

## Architecture

The project is a Maven multi-module build with two modules: `hotel-core` and `hotel-desktop`.

- **`hotel-core`** contains the shared models, enums, repositories, services, exceptions, database configuration, and SQL schema.
- **`hotel-desktop`** contains the Java Swing UI for administrators and customers. It depends on `hotel-core`.

**All business rules live only in `hotel-core`.** Both workspaces call shared core services rather than implementing their own versions of availability, guest classification, reservation processing, or billing rules.

The administrator and customer workspaces use one MariaDB/MySQL database. Online reservations submitted through the customer workspace are saved with reservation type `Online` and status `Pending`, then reviewed by administrators in `hotel-desktop`.

### Current project structure

```text
hotel-reservation/
├── pom.xml                                  # Parent Maven project; group ID com.hotel
├── README.md
├── GIT_WORKFLOW.md
├── .gitignore
│
├── hotel-core/                              # Shared code and business rules
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/hotel/core/
│       │   │   ├── config/                  # DatabaseConnection
│   │   │   ├── model/                      # Rooms, reservations, users, receipts, snapshots
│   │   │   ├── model/enums/                # Reservation, room, guest, and payment values
│   │   │   ├── repository/                 # JDBC persistence and booking-history access
│   │   │   ├── service/                    # Room, reservation, customer, and auth workflows
│   │   │   ├── setup/                      # Initial administrator account utility
│   │   │   └── exception/
│   │   └── resources/db/                   # Current schema and numbered migrations
│       └── test/java/com/hotel/core/        # Core service and setup tests
│
├── hotel-desktop/                           # Admin and customer Java Swing app
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/hotel/desktop/
│       │   ├── HotelDesktop.java            # Single JFrame hosting the desktop screens
│       │   ├── auth/                        # Welcome, login, and customer sign-up panels
│       │   ├── checkIn/ and checkOut/       # Staff stay-management workflows
│       │   ├── customer/                    # Customer-only Swing workspace and screens
│       │   ├── dashboard/ and nav/          # Staff dashboard and shared navigation
│       │   ├── reservation/                 # Walk-in, online, calendar, and history screens
│       │   ├── roomManagement/              # Room administration
│       │   ├── roomMonitoring/               # Read-only room status and shared data source
│       │   └── houseKeepingStatus/            # Housekeeping screen
│
```

The tree summarizes the current source layout. Keep the module boundaries: shared code and business rules in `hotel-core`, and administrator and customer Swing UI in `hotel-desktop`. There is no separate web module.

## Core OOP Design

The shared domain classes and business operations belong in **`hotel-core`**, under `com.hotel.core`. Both desktop workspaces should call the core services and repositories instead of duplicating these classes or rules.

- **`Room`** — an immutable model with room number, floor, type, capacity, description, daily rate, occupancy, and housekeeping status.
- **`Guest`** — an immutable record of guest details, classification, discount-ID details, verification state, and optional proof path.
- **Guest billing** — `GuestClassification` determines the regular or discounted billing rules; a discounted rate applies only when the discount ID is verified.
- **`Reservation`** — guest, room, check-in/check-out dates and times, stored rate, number of nights, guest count, reservation type, and status.
- **Checkout and payment models** — checkout bills and receipts carry calculated amounts and receipt data; cash transactions and customer payment records are persisted through the repositories.
- **Enums** — reservation, room, housekeeping, guest classification, booking history, and customer payment enums represent fixed domain values.
- **Repositories** — `RoomRepository`, `ReservationRepository`, user repositories, and `BookingHistoryRepository` handle database operations.
- **Services** — `RoomService`, `ReservationService`, `CustomerReservationService`, and `AuthenticationService` own shared workflows; `PaymentProofStorage` and `RoomStatusResetScheduler` support proof storage and room status maintenance.
- **Exceptions** — shared exception types for core operations.

Current status values:

- **Reservation:** Pending, Confirmed, Checked-In, Checked-Out, Cancelled.
- **Housekeeping:** Clean, Dirty, Under Maintenance, Out of Service.
- **Reservation type:** Online, Walk-in.

Use enums for fixed status values where practical, rather than scattering free-form strings throughout the code.

## Business Rules

### Billing

Let `Nights = Check-out Date - Check-in Date`.

- Room cost = Room rate per night × Nights
- Discount amount = Room cost × Discount rate
- Tax = (Room cost − Discount amount) × Tax rate
- Final total = Room cost − Discount amount + Tax
- Change = Cash tendered − Final total

| Guest classification | Discount | Tax |
|---|---:|---:|
| Regular guest | 0% | 12% |
| Discounted guest | 20% | 0% |

Reject a cash transaction if the amount tendered is less than the amount due. Use an appropriate decimal type such as `BigDecimal` for money calculations rather than `double`.

### Reservation availability

- Check availability for the **entire requested stay**, including its date and time, not just the room's current occupancy label.
- A reservation normally overlaps an existing reservation when `existingCheckIn < requestedCheckOut` and `existingCheckOut > requestedCheckIn`, using the stay's date and time, provided the existing reservation has a status that blocks new bookings.
- A room must also be operational and suitable for the requested stay. Rooms under maintenance or out of service must not be bookable.
- Prevent conflicting bookings even when two users submit requests at nearly the same time. Availability checks should be supported by appropriate database transaction/locking or constraint strategies.
- Validate that check-out is after check-in, and calculate nights from the dates rather than trusting a value submitted by the UI.
- After checkout, a room may need to become **Dirty** rather than immediately **Available**. Housekeeping must mark it clean before it can be offered again if that is the chosen operational rule.

## Prerequisites

Install or prepare these tools before running the project:

- Apache NetBeans (for Java development and Swing UI work)
- Visual Studio Code (also used by the team)
- JDK 17 or later; JDK 21 is installed for development
- Git
- Apache Maven
- XAMPP or another local MariaDB/MySQL server

FlatLaf and a Swing-compatible date-entry component are optional desktop UI enhancements. Cygwin is not a required project dependency. Customer-facing screens are part of the Java Swing desktop module; do not add web frontend dependencies.

## Local Setup

### 1. Clone the repository

Use a fresh clone of the current GitHub repository, especially because the `main` branch history was reset and force-pushed. If you have an old clone, first back up any uncommitted or unpushed work you need to keep, then remove or rename the old clone and clone the repository again.

```bash
git clone <GITHUB_REPOSITORY_URL>
cd HotelReservation-OOP1
```

Open the **parent `pom.xml` at the repository root** in NetBeans or VS Code. Do not open only `hotel-core` or `hotel-desktop` as if it were the parent project. The root Maven project coordinates the modules.

### 2. Create and initialize the database

Start the database service from XAMPP (the service may be MariaDB or MySQL depending on the XAMPP release). Create a database for the hotel system, for example `hotel_reservation`, if the team has not already created one:

```sql
CREATE DATABASE hotel_reservation
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

The shared schema script is located at:

```text
hotel-core/src/main/resources/db/schema.sql
```

Apply that schema to the local database. The tables must exist before running database-backed workflows.

For a database initialized from an older version of the schema, apply `hotel-core/src/main/resources/db/migrations/001_add_users_is_enabled.sql` once (it safely skips the change when the column already exists). Do not drop or recreate the database to apply this migration.

For an existing database that already has the guest, reservation, and transaction tables, also apply `hotel-core/src/main/resources/db/migrations/002_reservation_workflows.sql` once after the users migration. It adds reservation rate/guest-count snapshots and transaction receipt references required by the desktop workflows. The migration is not idempotent; do not run it more than once. New databases get these fields from `schema.sql` and do not need this migration.

Apply `hotel-core/src/main/resources/db/migrations/003_customer_online_booking.sql` once after migrations 001 and 002. It adds customer account fields, online booking/payment/notification tables, and room dirty-time data. Existing `Receptionist` account roles are migrated to `Admin`. Then apply `hotel-core/src/main/resources/db/migrations/004_customer_discount_id_proof.sql` once to add the customer discount-ID proof path. Migrations 003 and 004 are not idempotent; back up the database and do not run either more than once. New databases already have these fields in `schema.sql` and do not need these migrations.

Apply `hotel-core/src/main/resources/db/migrations/005_booking_history_ledger.sql` after migration 004. It creates the archival event ledger and imports existing Checked-Out and Cancelled reservations. New databases get the table from `schema.sql` and do not need migration 005.

Apply `hotel-core/src/main/resources/db/migrations/006_allow_same_day_stays.sql` after migration 005 to an existing database. It updates the reservation date/time constraint to allow a same-day stay only when checkout is later than check-in. This migration is not idempotent; apply it once. A new database initialized from the current `schema.sql` already has the updated constraint and does not need the migrations.

For local development, use a dedicated database user rather than the database administrator/root account where possible.

### 3. Configure the shared database connection

Database connection configuration belongs in `hotel-core`, using the shared connection setup (including `DatabaseConnection` in `com.hotel.core.config` as shown in the project layout). Configure the connection details for each developer's local environment without committing actual passwords or private credentials to GitHub.

Copy `hotel-core/src/main/resources/db.properties.example` to the ignored local file `hotel-core/src/main/resources/db.properties` and replace its database username/password placeholders. Do not commit the local file or credentials.

The administrator and customer workspaces use the same database connection settings and database schema. Do not create a separate application database for online reservations.

Payment proof files are stored outside the source tree. By default the application uses `.hotel-reservation/payment-proofs` under the current user's home directory. Configure another location with the JVM property `-Dhotel.payment.proof.directory=<path>` or the `HOTEL_PAYMENT_PROOF_DIRECTORY` environment variable. Payment proofs and discount-ID proofs accept any file type up to 5 MB; they are stored under generated filenames and the database stores the saved paths. Discounted guests must still submit an ID proof, and its discount remains unverified until staff review. The administrator screen previews supported payment-proof image files in-app; other file types can be submitted but do not have an in-app image preview.

Discount-ID uploads use the same configured proof-storage directory and the same 5 MB size limit; staff must verify the ID before the discounted classification is approved.

### 4. Build the multi-module project

Run Maven from the repository root, where the parent `pom.xml` is located:

```bash
mvn clean install
```

This builds and installs the modules in the reactor according to the parent Maven configuration. Fix any build errors before moving on.

### 5. Create the initial administrator account

After applying the schema/migration and configuring `db.properties`, run the account setup utility in a terminal with password input enabled:

```bash
mvn -pl hotel-core org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.hotel.core.setup.StaffAccountSetup
```

Enter an administrator username, display name, and a password between 10 and 128 characters. Password input is hidden; the utility stores a salted PBKDF2-HMAC-SHA256 hash and creates an enabled account with the `Admin` role. Customer sign-up is available from the login screen and creates only `Customer` accounts.

If an older database already contains the legacy `staff` placeholder row, it is not a valid account and is not overwritten; choose a different unique username. The migration and setup utility leave existing user rows intact.

### 6. Run the desktop application

In NetBeans, open the parent Maven project and run the `HotelDesktop.java` main class from the `hotel-desktop` module. Its entry point opens the desktop application's Welcome screen. Login routes an Admin to the staff workspace and a Customer to a separate customer-only workspace. Walk-in bookings are created as Confirmed reservations for today and have a PDF receipt download. Pending online bookings can be approved or rejected, payment and discount-ID proofs can be reviewed, and the administrator calendar and dashboard read current reservation data. Walk-in checkout collects cash; online checkout requires fully paid and verified online payments. Normal and forced check-out mark the room Dirty, and a background task changes rooms back to Clean after one hour. A valid database configuration and the required schema/migrations are necessary for these workflows.

## Run and Test

Run Maven commands from the repository root unless stated otherwise.

Build all modules and run their configured tests:

```bash
mvn clean install
```

Run tests for the shared core only:

```bash
mvn -pl hotel-core test
```

Build the desktop module and its required modules:

```bash
mvn -pl hotel-desktop -am test
```

Build only the core module and its dependencies (where applicable):

```bash
mvn -pl hotel-core -am package
```

Tests should live with the module they test. The shared core tests are under `hotel-core/src/test/java/com/hotel/core/`. Tests for desktop behavior can live under `hotel-desktop/src/test/java/`. Run the parent build before merging changes to confirm that the modules work together.

Before marking a feature complete, test at least:

- Invalid guest and room information.
- Check-out on or before the check-in date.
- A reservation overlapping an existing booking.
- Reservations for rooms that are occupied, under maintenance, or out of service.
- Two requests attempting to reserve the same room and date range.
- Regular and discounted guest billing.
- Insufficient cash payment and change calculation.
- Room status transitions during check-in, checkout, and housekeeping.
- Access to staff-only functions by an unauthenticated or unauthorized user; customers must never be routed to administrator screens.

## GitHub Team Workflow

Use `main` as the stable branch. Develop each task on its own branch and submit it through a pull request. See [`GIT_WORKFLOW.md`](GIT_WORKFLOW.md) for the team's recommended commands and safety notes.

**Repository history note:** The history on `main` was reset and force-pushed. All collaborators must use a fresh clone of the current repository. Back up any uncommitted or unpushed work from an old clone before deleting or renaming that folder; then run `git clone` again. Do not attempt to merge the old clone's outdated history into the reset `main` without coordinating with the repository owner.

Recommended branch names:

- `feature/room-management`
- `feature/walk-in-reservations`
- `feature/reservation-scheduling`
- `feature/billing`
- `feature/online-reservations`
- `fix/date-overlap-validation`
- `docs/readme-setup`

Recommended commit messages are short and action-oriented, such as `Add room availability validation` or `Document local database setup`.

The `.gitignore` excludes generated Maven `target/` directories and NetBeans private project files such as `nbproject/private/`. Do not force-add these generated or local-only files unless the team has a specific, documented reason.

## Security Notes

- Admin login and customer sign-up belong to the desktop application and must use the users table with hashed passwords; never store plaintext passwords.
- Customer workspace navigation must not expose staff functions, staff-only screens, or administrative operations.
- Customer screens must not reveal another guest's personal information or reservations. Protect all reservation lookup and cancellation flows with appropriate checks.
- Use prepared statements or parameterized database operations to reduce SQL injection risk.
- Validate all inputs in the shared core/business layer, including dates, prices, IDs, and reservation status changes. Do not rely only on desktop-form validation.
- Keep credentials out of source control. If a real secret is accidentally committed, rotate it; deleting the file in a later commit does not erase it from Git history.
- Use appropriate access controls for database accounts.
- Do not store raw payment-card details. The current scope calls for cash transactions at checkout.

Because both workspaces use the same database, use appropriate database transactions, locking, or constraints around reservation creation and availability updates. A check performed only in the user interface or by a non-atomic service check may still allow two requests to book the same room for overlapping dates.

## Project Documentation

The implementation should stay consistent with the team's project guide and flowcharts, archived with the dashboard mockup in [`docs/reference/source-materials/`](docs/reference/source-materials/). See [`docs/reference/PROJECT_CONTEXT.md`](docs/reference/PROJECT_CONTEXT.md) for the reviewed requirements and current implementation scope. The Java Swing application contains separate administrator and customer workspaces and uses `hotel-core` for shared business rules. The `hotel-web` application was cancelled; do not describe or add it as part of the current project. Update this README as architecture decisions change, and keep feature checkboxes unchecked until each feature has been implemented and tested.
