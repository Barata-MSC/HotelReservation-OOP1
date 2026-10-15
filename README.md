# Object-Oriented Hotel Reservation System

A Java-based Object-Oriented Hotel Reservation System built as a Maven multi-module project. The system will provide two applications that use the same shared business logic and MariaDB/MySQL database:

- **`hotel-desktop`** — a Java Swing application for receptionists and hotel staff. Development starts with walk-in reservations, room management, check-in/check-out, billing, receipts, housekeeping, and room monitoring.
- **`hotel-web`** — a customer-facing Spring Boot and Thymeleaf website for online reservations. This is the final development phase and will be added after the shared core and desktop workflows are established.
- **`hotel-core`** — shared Java code containing the models, repositories, services, configuration, exceptions, database schema, and all business rules used by both applications.

Both applications will connect to **one shared database** and call the same `hotel-core` business logic. Online reservations will be saved with reservation type `Online` and initial status `Pending`, then made visible to receptionists in the desktop application.

> **Project status:** In development. The `hotel-core` and `hotel-desktop` modules exist. The `hotel-web` module has not been created yet and is planned for the final phase. The desktop currently has Welcome, Login, and Dashboard UI prototypes. Login-to-dashboard navigation is preview-only: authentication, persistence, and dashboard actions are not connected. Dashboard counts and reservations are static sample data. The checklists below describe planned work; no feature is marked complete here.

## Contents

- [Project Goals](#project-goals)
- [Planned Features](#planned-features)
- [Website Pages](#website-pages)
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

The project applies object-oriented programming concepts to a hotel reservation system through a shared Java core and two separate user-facing applications.

1. **Build the shared core first.** Put the OOP models, repository operations, services, exceptions, database connection configuration, schema, and all business rules in `hotel-core`.
2. **Build the staff desktop application.** Use Java Swing in `hotel-desktop` for walk-in reservations and the hotel's staff workflows. This is the first application phase.
3. **Add online reservations last.** Create `hotel-web` in the final phase as a customer-facing Spring Boot and Thymeleaf website. It will depend on `hotel-core`, not duplicate its business rules.
4. **Share one database.** The desktop and web applications will use the same MariaDB/MySQL database so room information and reservation records remain consistent between them.
5. **Keep online booking separate from the walk-in workflow.** Online reservations will use `ReservationType = Online` and start with status `Pending`. Receptionists will review them in the desktop application's reservation management screen.

Online reservations mean that customers can submit a reservation through the website; they do **not** necessarily mean that the system accepts online card payments. The current project requirements specify cash payment processing at checkout.

## Planned Features

The checklists are intentionally left unchecked. Update them only as features are implemented and tested.

### Shared core (`hotel-core`)

- [ ] Define the shared OOP models, enums, and exceptions.
- [ ] Implement database connection configuration and repository operations.
- [ ] Implement room, reservation, guest, user, and transaction data access.
- [ ] Implement room management, availability, reservation, authentication, and billing services.
- [ ] Store the shared database schema in `hotel-core/src/main/resources/db/schema.sql`.
- [ ] Keep all business rules in this module so neither application implements a separate copy.

### Staff desktop application (`hotel-desktop`) — first phase

- [ ] Provide a staff login screen backed by the users table and hashed passwords.
- [ ] Add, update, search, and display hotel rooms.
- [ ] Manage room availability and housekeeping status.
- [ ] Create and manage **walk-in reservations**.
- [ ] Display and manage online reservations submitted through the future website.
- [ ] Check guests in and out.
- [ ] Calculate bills, process cash payments, calculate change, and generate receipts.
- [ ] Monitor available and occupied rooms, room types, cleaning needs, maintenance, and out-of-service rooms.
- [ ] Call the shared business logic in `hotel-core` rather than duplicating it in the desktop UI.

### Customer-facing website (`hotel-web`) — final phase

- [ ] Browse rooms and view room details, rates, and capacity.
- [ ] Search for rooms using check-in and check-out dates and guest requirements.
- [ ] Enter guest details and submit an online reservation.
- [ ] Save online reservations with reservation type `Online` and initial status `Pending`.
- [ ] Display a reservation reference and current booking status after submission.
- [ ] Allow a guest to view or cancel a reservation only when supported by the final system rules and access checks.
- [ ] Use the website on mobile and desktop browsers.
- [ ] Use `hotel-core` for availability checks and reservation business rules.

## Website Pages

The website is planned for the final phase. This section lists **customer-facing pages only**. Staff functions belong in the Java Swing desktop application and must not be exposed as customer website features.

### Customer-facing pages

| Page | Purpose |
|---|---|
| **Home** | Introduce the hotel and provide a clear entry point to room search. |
| **Rooms / Room Listings** | Browse rooms, room types, capacity, descriptions, and nightly rates. |
| **Room Details** | Show details for a selected room and let the customer continue to date selection. |
| **Search Availability** | Enter check-in/check-out dates and guest requirements; show rooms available for the full date range. |
| **Reservation Form** | Collect the guest's name, contact number, email, selected room, and stay dates. |
| **Reservation Confirmation** | Show the booking reference and current reservation status after a successful submission. |
| **Find / View Reservation** *(optional)* | Let a guest look up a booking and cancel it if the system's rules permit. Guest information must be protected. |

### Website template and static-resource layout (planned)

When `hotel-web` is created, use Thymeleaf templates for customer pages and shared fragments. Do not create a `staff/` template directory for staff workflows; those screens belong to `hotel-desktop`.

```text
hotel-web/
└── src/main/
    ├── java/com/hotel/web/
    └── resources/
        ├── templates/
        │   ├── customer/
        │   │   ├── home.html
        │   │   ├── rooms.html
        │   │   ├── room-details.html
        │   │   ├── availability.html
        │   │   ├── reservation-form.html
        │   │   ├── reservation-confirmation.html
        │   │   └── find-reservation.html
        │   └── fragments/
        │       ├── header.html
        │       ├── footer.html
        │       └── navigation.html
        └── static/
            ├── css/
            ├── js/
            └── images/
```

This is a proposed layout for the future web module, not a claim that the module or its templates already exist. Controllers, templates, and authorization rules will be added when online reservation development begins.

## Desktop Screens

The Java Swing staff application is developed in `hotel-desktop`. These are the planned desktop screens, not a claim that they are already implemented.

| Screen | Purpose |
|---|---|
| **Login** | Authenticate hotel staff using the users table and hashed passwords. |
| **Dashboard** | Provide a staff overview and navigation to operational modules. |
| **Room Management** | Add, edit, search, and view room records. |
| **Reservation Management** | Create walk-in reservations and review/manage online reservations, including their permitted status changes. |
| **Check-in / Check-out** | Process arrivals and departures and update reservation and room status. |
| **Billing and Receipt** | Calculate the bill using the specified guest-classification rules, accept cash payment, calculate change, and generate a receipt. |
| **Room Monitoring** | Display available and occupied rooms, room counts by type, cleaning needs, maintenance, and out-of-service rooms. |
| **Housekeeping** | Update room status to Clean, Dirty, Under Maintenance, or Out of Service. |

The desktop application will call `hotel-core` for reservation, availability, authentication, billing, and other business rules. UI classes should not contain a second implementation of those rules.

The current dashboard is a visual prototype, not an implemented room-monitoring or reservation workflow. Its navigation and quick-action controls are placeholders; connect them to the documented desktop screens and `hotel-core` services as those features are implemented. See [`docs/reference/PROJECT_CONTEXT.md`](docs/reference/PROJECT_CONTEXT.md) for dashboard terminology and source-document alignment notes.

## Technology Stack

| Area | Planned technology | Purpose |
|---|---|---|
| IDEs | Apache NetBeans and Visual Studio Code | Team development, editing, and debugging |
| Language | Java 17 or later; JDK 21 is installed for development | Shared application logic and OOP models |
| Build tool | Apache Maven multi-module project | Builds modules and manages dependencies |
| Parent project | Root `pom.xml`, group ID `com.hotel` | Defines the project and its modules |
| Shared module | `hotel-core` | Models, repositories, services, exceptions, configuration, schema, and business rules |
| Desktop UI | Java Swing | Staff workflows, beginning with walk-in reservations |
| Web framework *(final phase)* | Spring Boot, in `hotel-web` only | Customer-facing online reservation application |
| Web page rendering *(final phase)* | Thymeleaf, in `hotel-web` only | Render customer-facing HTML pages |
| Web frontend *(final phase)* | HTML, CSS, JavaScript, Bootstrap | Responsive customer website |
| Database access | JDBC through shared repository/configuration code | Database operations used by both applications |
| Database | MariaDB or MySQL | Shared persistence for rooms, guests, reservations, users, and transactions |
| Local database environment | XAMPP, if using its bundled MariaDB/MySQL service | Local development database |
| Version control | Git and GitHub | Branches, collaboration, reviews, and project history |

**Optional development tools:** Jeddict AI may assist with coding, but generated code must be reviewed and tested. NetBeans Database Manager can help inspect database connections. Cygwin is not normally required for the Git/Maven workflow; Git Bash, PowerShell, or a suitable IDE terminal can be used. FlatLaf is an optional look-and-feel choice for the Swing desktop UI. A Swing-compatible date-picker component is also optional; select one only if the standard date-input controls are insufficient for the desktop design. These tools are not substitutes for the shared business logic in `hotel-core`.

## Architecture

The project is one Maven multi-module build with three modules. The first two currently exist; `hotel-web` is planned for the final phase.

- **`hotel-core`** contains the shared models, enums, repositories, services, exceptions, database configuration, and SQL schema.
- **`hotel-desktop`** contains the Java Swing UI used by the receptionist and staff. It depends on `hotel-core`.
- **`hotel-web`** will contain the Spring Boot and Thymeleaf customer website. It will depend on `hotel-core` and will be added last.

**All business rules live only in `hotel-core`.** The desktop application and the future website call the shared core services rather than implementing their own versions of availability, guest classification, reservation processing, or billing rules.

Both applications use one MariaDB/MySQL database. Online reservations created by `hotel-web` will be saved with reservation type `Online` and status `Pending`, then retrieved and managed by staff in `hotel-desktop`.

### Suggested project structure

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
│       │   │   ├── model/
│       │   │   │   ├── Room, Reservation, Transaction
│       │   │   │   ├── guest/               # Guest, RegularGuest, DiscountedGuest
│       │   │   │   └── enums/               # ReservationType, ReservationStatus,
│       │   │   │                            # HousekeepingStatus, GuestClassification
│       │   │   ├── repository/              # Room, Guest, Reservation, Transaction, User
│       │   │   ├── service/                 # Room, Availability, Reservation, Billing, Auth
│       │   │   └── exception/
│       │   └── resources/db/schema.sql      # Shared database schema
│       └── test/java/com/hotel/core/        # Core tests
│
├── hotel-desktop/                           # Staff/receptionist Java Swing app
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/hotel/desktop/
│       │   ├── HotelDesktop.java            # Single JFrame hosting the desktop screens
│       │   ├── auth/                        # WelcomeScreen and LoginScreen
│       │   └── dashboard/                   # DashboardScreen
│       └── test/java/                       # Desktop tests, as added
│
└── hotel-web/                               # Future customer website; not created yet
    ├── pom.xml                              # To be added in the final phase
    └── src/                                 # Spring Boot + Thymeleaf customer website
```

The tree shows the planned module organization. The specific files and panels can be added as implementation progresses, but the module boundaries should remain: shared code and business rules in `hotel-core`, staff UI in `hotel-desktop`, and customer-facing online booking in `hotel-web`.

## Core OOP Design

The shared domain classes and business operations belong in **`hotel-core`**, under `com.hotel.core`. The desktop UI and future website should call the core services and repositories instead of duplicating these classes or rules.

- **`Room`** — room number, floor, type, capacity, description, daily rate, availability, and housekeeping status.
- **`Guest`** — an abstract class containing shared guest information and the contract for guest-specific billing rules.
- **`RegularGuest`** — no discount and 12% tax, according to the project guide.
- **`DiscountedGuest`** — 20% discount and tax exemption, according to the project guide; eligibility should be verified and recorded.
- **`Reservation`** — guest, room, check-in/check-out dates, number of nights, reservation type, and reservation status.
- **`Transaction`** — reservation reference, room charges, discount, tax, total, cash tendered, change, payment status, and receipt details.
- **Enums** — `ReservationType`, `ReservationStatus`, `HousekeepingStatus`, and `GuestClassification` represent the fixed values supplied in the project layout.
- **Repositories** — database operations for rooms, guests, reservations, transactions, and users.
- **Services** — shared business rules and multi-step workflows for room management, availability, reservations, billing, and authentication.
- **Exceptions** — shared exception types for core operations.

Suggested status values:

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

- Check availability for the **entire requested date range**, not just the room's current occupancy label.
- A reservation normally overlaps an existing reservation when `existingCheckIn < requestedCheckOut` and `existingCheckOut > requestedCheckIn`, provided the existing reservation has a status that blocks new bookings.
- A room must also be operational and suitable for the requested stay. Rooms under maintenance or out of service must not be bookable.
- Prevent conflicting bookings even when two users submit requests at nearly the same time. Availability checks should be supported by appropriate database transaction/locking or constraint strategies.
- Validate that check-out is after check-in, and calculate nights from the dates rather than trusting a value submitted by the browser.
- After checkout, a room may need to become **Dirty** rather than immediately **Available**. Housekeeping must mark it clean before it can be offered again if that is the chosen operational rule.

## Prerequisites

Install or prepare these tools before running the project:

- Apache NetBeans (for Java development and Swing UI work)
- Visual Studio Code (also used by the team)
- JDK 17 or later; JDK 21 is installed for development
- Git
- Apache Maven
- XAMPP or another local MariaDB/MySQL server
- A modern browser for the website phase

FlatLaf and a Swing-compatible date-picker component are optional desktop UI enhancements. Cygwin is not a required project dependency. The future `hotel-web` module will use Spring Boot and Thymeleaf; these web dependencies do not belong in `hotel-core` or `hotel-desktop` unless the project configuration specifically requires otherwise.

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

Apply that schema to the local database. The tables must exist before running database-backed workflows. Keep the desktop and future web application pointed at the **same database** so that they can share rooms, guests, users, reservations, and transactions.

For local development, use a dedicated database user rather than the database administrator/root account where possible.

### 3. Configure the shared database connection

Database connection configuration belongs in `hotel-core`, using the shared connection setup (including `DatabaseConnection` in `com.hotel.core.config` as shown in the project layout). Configure the connection details for each developer's local environment without committing actual passwords or private credentials to GitHub.

Both `hotel-desktop` and, later, `hotel-web` must use the same database connection settings and database schema. Do not create a separate application database for online reservations.

### 4. Build the multi-module project

Run Maven from the repository root, where the parent `pom.xml` is located:

```bash
mvn clean install
```

This builds and installs the modules in the reactor according to the parent Maven configuration. Fix any build errors before moving on.

### 5. Run the desktop application

In NetBeans, open the parent Maven project and run the `HotelDesktop.java` main class from the `hotel-desktop` module. Its entry point opens the desktop application's Welcome screen. The Login button currently opens the Dashboard as a UI preview only; it does not authenticate staff. The exact IDE run configuration may depend on the project's current Maven setup.

The `hotel-web` module has **not been created yet**. The customer website setup and web run instructions will be added when that final phase begins.

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

Tests should live with the module they test. The shared core tests are under `hotel-core/src/test/java/com/hotel/core/`. Tests for desktop and, later, web behavior can live under the respective module's `src/test/java/` tree. Run the parent build before merging changes to confirm that the modules work together.

Before marking a feature complete, test at least:

- Invalid guest and room information.
- Check-out on or before the check-in date.
- A reservation overlapping an existing booking.
- Reservations for rooms that are occupied, under maintenance, or out of service.
- Two requests attempting to reserve the same room and date range.
- Regular and discounted guest billing.
- Insufficient cash payment and change calculation.
- Room status transitions during check-in, checkout, and housekeeping.
- Access to staff-only functions by an unauthenticated or unauthorized user; staff functions belong to the desktop app and must not be exposed to customers through the website.

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

- Staff login belongs to the desktop application and must use the users table with hashed passwords; never store plaintext passwords.
- The future customer website must not expose staff functions, staff-only screens, or administrative operations.
- The website must not reveal other guests' personal information or reservations. Protect all reservation lookup and cancellation flows with appropriate checks.
- Use prepared statements or parameterized database operations to reduce SQL injection risk.
- Validate all inputs in the shared core/business layer, including dates, prices, IDs, and reservation status changes. Do not trust validation performed only by a desktop form or web browser.
- Keep credentials out of source control. If a real secret is accidentally committed, rotate it; deleting the file in a later commit does not erase it from Git history.
- Use appropriate access controls for database accounts and HTTPS for the deployed website.
- Do not store raw payment-card details. The current scope calls for cash transactions at checkout.

Because both applications can access the same database, use appropriate database transactions, locking, or constraints around reservation creation and availability updates. A check performed only in the user interface or by a non-atomic service check may still allow two requests to book the same room for overlapping dates.

## Project Documentation

The implementation should stay consistent with the team's project guide and flowcharts, archived with the dashboard mockup in [`docs/reference/source-materials/`](docs/reference/source-materials/). See [`docs/reference/PROJECT_CONTEXT.md`](docs/reference/PROJECT_CONTEXT.md) for the reviewed requirements and decisions. The staff desktop application is the first implementation phase, focused on walk-in reservations and hotel operations. The customer-facing website is the final phase and will use the same `hotel-core` services and database for online reservations. Update this README as architecture decisions change, and keep feature checkboxes unchecked until each feature has been implemented and tested.
