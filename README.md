# Object-Oriented Hotel Reservation System

A Java-based hotel reservation web application for customers and hotel staff. Customers will be able to search for rooms and submit reservations through a browser, while authorized staff will manage room inventory, reservations, check-in/check-out, billing, and housekeeping.

> **Project status:** In development. This README describes the planned architecture and requirements. Update the feature checklist as each module is implemented and tested.

## Contents

- [Project Goals](#project-goals)
- [Planned Features](#planned-features)
- [Website Pages](#website-pages)
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

The project applies object-oriented programming concepts to a hotel reservation system. It aims to provide a responsive interface for online room reservations and a staff dashboard for hotel operations.

Online reservations mean that customers can book through the website; they do **not** necessarily mean that the system accepts online card payments. The current project requirements specify cash payment processing at checkout.

## Planned Features

### Customer-facing website

- [ ] Browse rooms and view room details, rates, and capacity.
- [ ] Search for rooms using check-in and check-out dates and guest requirements.
- [ ] Enter guest details and submit a reservation.
- [ ] Receive a reservation reference and booking status.
- [ ] View or cancel a reservation when permitted by the system's rules.
- [ ] Use the website on both mobile and desktop browsers.

### Staff and administrator dashboard

- [ ] Secure staff login and role-based access.
- [ ] Add, update, search, and display hotel rooms.
- [ ] Manage room availability and housekeeping status.
- [ ] Review and manage online and walk-in reservations.
- [ ] Check guests in and out.
- [ ] Calculate bills, process cash payments, calculate change, and generate receipts.
- [ ] Monitor available and occupied rooms, room types, cleaning needs, maintenance, and out-of-service rooms.

## Website Pages

The following is the **proposed page plan** for the browser-based system. These are planned pages, not a claim that they have already been implemented. Combine pages or rename them as the design develops.

### Customer-facing pages

| Page | Purpose |
|---|---|
| **Home** | Introduce the hotel and provide a clear entry point to room search. |
| **Rooms / Room Listings** | Browse rooms, room types, capacity, descriptions, and nightly rates. |
| **Room Details** | Show details for a selected room and let the customer continue to date selection. |
| **Search Availability** | Enter check-in/check-out dates and guest requirements; show rooms available for the full date range. |
| **Reservation Form** | Collect the guest's name, contact number, email, selected room, and stay dates. |
| **Reservation Confirmation** | Show the booking reference and current reservation status after a successful submission. |
| **Find / View Reservation** *(optional)* | Let a guest look up a booking and cancel it if the system's rules permit. Protect guest information when looking up bookings. |

### Staff and administrator pages

| Page | Purpose |
|---|---|
| **Staff Login** | Authenticate authorized hotel staff. |
| **Staff Dashboard** | Provide a summary and navigation to room, reservation, and monitoring functions. |
| **Room Management** | Add, edit, search, and view room records. |
| **Reservation Management** | Review online and walk-in reservations, and manage their permitted status changes. |
| **Check-in / Check-out** | Process arrivals and departures and update reservation and room status. |
| **Billing and Receipt** | Calculate the bill using the specified guest classification rules, accept cash payment, calculate change, and show a receipt. |
| **Room Monitoring** | Display available and occupied rooms, room counts by type, cleaning needs, maintenance, and out-of-service rooms. |
| **Housekeeping** | Update room status to Clean, Dirty, Under Maintenance, or Out of Service. |

### Page and route naming suggestion

If using Spring Boot with Thymeleaf, keep page templates in `src/main/resources/templates/`. A possible organization is:

```text
templates/
├── customer/
│   ├── home.html
│   ├── rooms.html
│   ├── room-details.html
│   ├── availability.html
│   ├── reservation-form.html
│   ├── reservation-confirmation.html
│   └── find-reservation.html
├── staff/
│   ├── login.html
│   ├── dashboard.html
│   ├── rooms.html
│   ├── reservations.html
│   ├── check-in-check-out.html
│   ├── billing.html
│   ├── monitoring.html
│   └── housekeeping.html
└── fragments/
    ├── header.html
    ├── footer.html
    └── navigation.html
```

This template layout is a starting suggestion. Controllers, templates, and authorization rules should be added as each feature is implemented. A page being present in this plan does not mean its functionality is complete.

## Technology Stack

| Area | Planned technology | Purpose |
|---|---|---|
| IDE | Apache NetBeans | Java development and debugging |
| Language | Java (JDK 17 or later, as supported by the selected dependencies) | Application logic and OOP model |
| Build tool | Apache Maven | Dependencies, builds, and tests |
| Web framework | Spring Boot | Web application and backend |
| Page rendering | Thymeleaf | Render server-side HTML pages |
| Frontend | HTML, CSS, JavaScript, Bootstrap | Responsive customer and staff interfaces |
| Database access | JDBC / Spring JDBC | Execute database operations from Java |
| Database | MariaDB or MySQL | Persist rooms, guests, reservations, users, and transactions |
| Local database environment | XAMPP, if using its bundled database server | Local development database |
| Version control | Git and GitHub | Branches, collaboration, reviews, and project history |

**Optional tools:** Jeddict AI can assist with development, but generated code must be reviewed and tested. NetBeans Database Manager can help inspect database connections. Cygwin is not normally required for this Java/Maven workflow. FlatLaf is for Swing desktop interfaces and is not needed for the planned browser-based UI.

## Architecture

The application is organized into layers so that web pages, business rules, and SQL operations remain separate.

1. **Presentation layer:** Thymeleaf templates, HTML, CSS, JavaScript, and Bootstrap.
2. **Controller layer:** Receives browser requests and returns pages or responses.
3. **Service layer:** Applies reservation, availability, guest-classification, and billing rules.
4. **Repository layer:** Reads and writes application data through JDBC.
5. **Database:** Stores persistent user, room, guest, reservation, and transaction records.

### Suggested project structure

```text
src/
├── main/
│   ├── java/com/hotel/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── model/
│   │   ├── repository/
│   │   └── service/
│   └── resources/
│       ├── static/
│       │   ├── css/
│       │   ├── js/
│       │   └── images/
│       ├── templates/
│       └── application.properties
└── test/
    └── java/com/hotel/
```

Maven also uses `pom.xml` at the project root to define dependencies and build settings. Keep the structure aligned with the actual generated project as it develops.

## Core OOP Design

The following classes reflect the project specifications and can be expanded as implementation requires.

- **`Room`** — room number, floor, type, capacity, description, daily rate, availability, and housekeeping status.
- **`Guest`** — an abstract class containing shared guest information and the contract for guest-specific billing rules.
- **`RegularGuest`** — no discount and 12% tax, according to the project guide.
- **`DiscountedGuest`** — 20% discount and tax exemption, according to the project guide; eligibility should be verified and recorded.
- **`Reservation`** — guest, room, check-in/check-out dates, number of nights, reservation type, and reservation status.
- **`Transaction`** — reservation reference, room charges, discount, tax, total, cash tendered, change, payment status, and receipt details.
- **Repositories** — database operations for rooms, guests, reservations, and transactions.
- **Services** — business rules and multi-step workflows, such as checking availability and completing checkout.

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

Install these tools before running the project:

- Apache NetBeans (a current version compatible with your Java setup)
- JDK 17 or later, compatible with the selected Spring Boot version and dependencies
- Git
- Maven, or the Maven Wrapper included in the repository
- XAMPP or another local MariaDB/MySQL server
- A modern browser

Cygwin, FlatLaf, and a separate Java date-picker component are not required for the planned web architecture. A native HTML date input can be used initially, with date validation enforced by the Java backend.

## Local Setup

The instructions below assume the repository uses Spring Boot, Maven, Thymeleaf, and MariaDB/MySQL. Adjust names if the actual project configuration differs.

### 1. Clone the repository

```bash
git clone <GITHUB_REPOSITORY_URL>
cd <REPOSITORY_FOLDER>
```

Open the Maven project in NetBeans. Do not run `git init` in a repository that has already been cloned.

### 2. Start and create the local database

Start the database service from XAMPP (the service may be MariaDB or MySQL depending on the XAMPP release). Create a database named `hotel_reservation`, for example through phpMyAdmin or the database client:

```sql
CREATE DATABASE hotel_reservation
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Create or apply the project's schema after the schema script or migration files have been added to the repository. Do not assume the tables exist until the schema has been created.

For local development, use a dedicated database user rather than the database administrator/root account where possible.

### 3. Configure database connection settings

Configure Spring's datasource properties with environment variables or a local configuration file that is **excluded from Git**. Example environment-variable names:

```text
DB_URL=jdbc:mariadb://localhost:3306/hotel_reservation
DB_USERNAME=your_local_database_user
DB_PASSWORD=your_local_database_password
```

Your `application.properties` can reference them, for example:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
```

Use the JDBC driver that matches the database selected in `pom.xml`. Never commit actual passwords, tokens, or production credentials.

### 4. Build and run

If the project includes the Maven Wrapper, use it from PowerShell on Windows:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Or use an installed Maven command:

```bash
mvn clean test
mvn spring-boot:run
```

When the application starts successfully, open `http://localhost:8080` in your browser. This address is for the local development computer only; online access from other devices requires deployment and suitable network configuration.

## Run and Test

Useful Maven commands:

```bash
mvn test          # Run automated tests
mvn clean package # Compile, test, and package the application
mvn spring-boot:run
```

Before marking a feature complete, test at least:

- Invalid guest and room information.
- Check-out on or before the check-in date.
- A reservation overlapping an existing booking.
- Reservations for rooms that are occupied, under maintenance, or out of service.
- Two requests attempting to reserve the same room and date range.
- Regular and discounted guest billing.
- Insufficient cash payment and change calculation.
- Room status transitions during check-in, checkout, and housekeeping.
- Access to staff-only pages by unauthenticated or unauthorized users.

## GitHub Team Workflow

Use `main` as the stable branch. Each task should be developed on its own branch and submitted through a pull request. See [`GIT_WORKFLOW.md`](GIT_WORKFLOW.md) for the team's recommended commands and safety notes.

Recommended branch names:

- `feature/room-management`
- `feature/reservation-scheduling`
- `feature/billing`
- `fix/date-overlap-validation`
- `docs/readme-setup`

Recommended commit messages are short and action-oriented, such as `Add room availability validation` or `Document local database setup`.

## Security Notes

- Hash passwords with a suitable password-hashing mechanism; never store plaintext passwords.
- Enforce staff roles and permissions on the server, not only by hiding UI elements.
- Use prepared statements or parameterized database operations to reduce SQL injection risk.
- Validate all inputs on the backend, including dates, prices, IDs, and reservation status changes.
- Use HTTPS in the deployed environment.
- Keep credentials out of source control. If a real secret is accidentally committed, rotate it; deleting the file in a later commit does not erase it from Git history.
- Do not store raw payment-card details. The current scope calls for cash transactions at checkout.

## Project Documentation

The implementation should stay consistent with the team's project guide and flowcharts. Update this README as architecture decisions change and replace the planned-feature checklist with completed, tested features as work progresses.
