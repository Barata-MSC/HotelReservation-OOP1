# Claude Code Project Guidelines

## Project context

This is a Java Object-Oriented Hotel Reservation System built as a Maven multi-module project. Before changing project code, read [`docs/reference/PROJECT_CONTEXT.md`](docs/reference/PROJECT_CONTEXT.md). Consult the original course guide, flowcharts, and dashboard mockup in [`docs/reference/source-materials/`](docs/reference/source-materials/) when a task depends on requirements or screen flow.

Treat the README's feature lists and architecture trees as plans unless the feature is present in the code and verified. The `hotel-core` and `hotel-desktop` modules exist; `hotel-web` is planned for the final phase and has not been created. The desktop login-to-dashboard transition is a UI preview, not authentication. Dashboard and other listed desktop previews are not production workflows or persisted data.

## Architecture boundaries

- `hotel-core` owns shared domain models, repositories, services, validation, exceptions, database configuration/schema, and all business rules.
- `hotel-desktop` is the staff/receptionist Java Swing application. Keep business rules out of UI classes; call shared core services instead.
- The planned `hotel-web` is customer-facing only and must use `hotel-core`. Do not expose staff workflows or administrative functions through it.
- Both applications are intended to share one MariaDB/MySQL database.
- Keep changes within the relevant module and follow the existing package structure and conventions.

## Desktop UI conventions

- `HotelDesktop.java` is the only `JFrame`. Other desktop screens are `JPanel` instances hosted by it.
- Staff screens belong in the shared workspace shell in `com.hotel.desktop.nav`. Register new content-only panels as workspace cards.
- Use the shell's navigation to switch the visible right-side card. Do not add another `JFrame`, a screen-specific sidebar, or rebuild the shared shell to navigate.
- The shared sidebar belongs to `nav/SidebarPanel`, not `DashboardScreen.java`.
- Preserve mounted screen instances and their UI state when navigating.
- Do not present preview navigation or static sample data as successful authentication, database-backed results, or completed business workflows.

## Domain terminology and rules

Use the project vocabulary and fixed values consistently:

- Reservation statuses: Pending, Confirmed, Checked-In, Checked-Out, Cancelled.
- Reservation types: Online and Walk-in. Online reservations start Pending and are reviewed by staff.
- Room monitoring statuses: Available, Occupied, Dirty, Under Maintenance, Out of Service. Use **Dirty** for the housekeeping state called “Cleaning” in the mockup.

Implement shared business rules in `hotel-core`, and validate inputs there rather than trusting desktop forms or browsers. In particular:

- Calculate reservation nights from dates and require check-out to be after check-in.
- Check room availability for the entire requested date range. Account for overlapping reservations and do not book rooms under maintenance or out of service.
- Protect reservation creation against concurrent conflicting bookings using appropriate database transactions, locking, or constraints.
- Use `BigDecimal` (or the established appropriate decimal type) for money, not `double`.
- Billing requirements: regular guests receive no discount and pay 12% tax; eligible discounted guests receive a 20% discount and are tax-exempt. Reject cash payments below the amount due and calculate change.
- After checkout, a room may need to become Dirty and require housekeeping before becoming Available; follow the established operational rule in the implementation.

Check the current implementation and project source documents before changing behavior. Do not infer that every README rule or planned workflow is already implemented.

## Security and configuration

- Never store plaintext passwords. Staff authentication must use hashed passwords.
- Use prepared statements or parameterized database operations.
- Keep credentials and private configuration out of version control. Use local configuration and the checked-in example properties file where available.
- Protect guest and reservation data; customer-facing flows must not reveal another guest's information.
- Do not store payment-card details; the current payment scope is cash at checkout.

## Build and validation

Run Maven commands from the repository root, which contains the parent `pom.xml`. Use the smallest relevant check for the change, then run the parent build when needed to verify module integration. README-supported commands include:

```bash
mvn -pl hotel-core test
mvn -pl hotel-desktop -am test
mvn -pl hotel-core -am package
mvn clean install
```

Add or update tests in the module being changed. For reservation and billing work, cover invalid dates, date-range overlaps, ineligible room states, concurrent booking protection, guest classifications, insufficient payment, and change calculation as applicable. For UI changes, verify the single-window navigation behavior and clearly distinguish preview behavior from implemented workflows.

## Repository workflow

- Follow [`GIT_WORKFLOW.md`](GIT_WORKFLOW.md); use a task branch and submit changes through review rather than working directly on `main`.
- Keep changes focused. Do not commit generated Maven `target/` output, IDE-private files, credentials, or unrelated user changes.
- Update directly related documentation when behavior or architecture changes. Keep README feature checkboxes unchecked until the feature is implemented and tested.
- Report the exact validation commands run and their results. If a check could not be run, state why; do not imply unrun checks passed.
