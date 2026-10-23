# hotel-web

> **Status: planned, not implemented.** Apart from this README, there is
> currently no `hotel-web` Maven module, Spring Boot application, controller,
> template, or customer booking flow in this repository. This document
> describes the intended role and boundaries for the final project phase; it
> is not a guide to a runnable application.

## Purpose

`hotel-web` is intended to become the customer-facing website for browsing
rooms, checking availability, and submitting online reservation requests. It
must not contain staff or administrative screens. Those workflows belong to
the `hotel-desktop` application.

The planned implementation is Spring Boot with Thymeleaf-rendered pages. It
will depend on `hotel-core` for shared validation, availability, reservation
processing, and persistence rather than copying business rules into web
controllers.

## Planned architecture

```text
Customer browser
       │
       ▼
hotel-web (planned: Spring Boot + Thymeleaf)
       │ calls shared services
       ▼
hotel-core (existing shared domain and persistence module)
       │
       ▼
MariaDB / MySQL shared with hotel-desktop
```

The root Maven build currently includes only `hotel-core` and `hotel-desktop`.
`hotel-web` has not been added to the Maven reactor and cannot currently be
built or launched.

## Intended customer-facing scope

The following are planned website capabilities, not implemented features:

- Browse room types, descriptions, capacity, and nightly rates.
- Search for rooms available for the requested date range and guest count.
- Enter guest and stay details and submit an online reservation request.
- Show a reservation reference and its current status after submission.
- Consider a protected reservation lookup or cancellation flow only when
  access checks prevent disclosure or changes to another guest's reservation.
- Provide responsive customer pages for desktop and mobile browsers.

Online reservations are intended to be saved as type `Online` with initial
status `Pending`, then reviewed by staff in the desktop application. The
desktop's current Online Reservations panel is only a placeholder, so the
end-to-end website-to-staff review flow does not exist yet.

## Proposed content organization

The following layout is a planning guide only. The application files and
folders shown below do not exist in the current repository:

```text
hotel-web/
├── pom.xml
└── src/main/
    ├── java/com/hotel/web/
    │   ├── controller/       Customer-facing request handlers
    │   └── configuration/    Web configuration and core wiring
    └── resources/
        ├── templates/
        │   ├── customer/     Home, rooms, availability, reservation pages
        │   └── fragments/    Shared page layout elements
        └── static/
            ├── css/
            ├── js/
            └── images/
```

The eventual project should keep HTTP and presentation concerns in
`hotel-web`, with business rules and shared database operations in
`hotel-core`.

## Planned technology

- Java and Maven, integrated into the root multi-module build.
- Spring Boot for the web application.
- Thymeleaf for server-rendered customer pages.
- HTML, CSS, and JavaScript for responsive presentation.
- The same MariaDB/MySQL database and `hotel-core` services as the desktop
  application.

These technologies and responsibilities are planned; they are not present as
dependencies or source code yet.

## Security and boundaries

- Do not expose staff sign-in, room administration, housekeeping, or other
  staff-only workflows to customer users.
- Validate all inputs and enforce reservation availability in `hotel-core`;
  browser-side validation alone is insufficient.
- Protect reservation lookups and cancellations so a customer cannot read or
  modify another guest's information.
- Use the shared repository and parameterized database operations; do not
  connect a web controller directly to unvalidated SQL.
- Keep credentials and private configuration out of source control.
- Do not add payment-card collection unless the project scope changes; the
  documented checkout workflow currently accepts cash in the desktop app.

## Build and implementation status

There is no `mvn -pl hotel-web ...` command that can succeed in the current
checkout because the module does not exist. Once the module is implemented and
registered in the root `pom.xml`, its tests can be run from the repository
root, for example:

```powershell
mvn -pl hotel-web -am test
```

Before presenting the website as complete, implement and test the customer
flows, shared-core integration, authorization and privacy checks, and
reservation conflict behavior. Keep customer-facing functionality separate
from the desktop staff workspace.
