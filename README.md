# CampusLoop — Peer-to-Peer Rental System (Java)

A student campus rental marketplace built with **Java 17, Spring Boot 3.3, Spring Data JPA, MySQL 8** and a responsive HTML/CSS/JavaScript frontend (no build step).

## Features
- Login / register on one screen, BCrypt password hashing, session login (HTTP-only, SameSite cookie)
- Browse available items with search, category and max-price filters
- Create, edit and delete your own listings (owner-only, checked on the server)
- Send rental requests with date validation and automatic price calculation
- Owners approve / decline pending requests; renters can cancel their own requests
- **Overlap protection:** an item cannot have two approved bookings on the same dates
- Received / sent request lists, pending-request badge, profile page with stats
- Consistent JSON errors, input validation, security headers (CSP, X-Frame-Options, …)
- `GET /api/health` checks the database connection

## Intentionally not implemented
Payments, money transfer, physical handover, return verification, refunds and dispute handling. The app manages listings and booking requests only.

## Requirements
- JDK 17+ (tested design target: 17 / 21)
- Maven 3.8+
- MySQL 8.x running locally

## Run on Windows
1. Start the MySQL service.
2. (Optional) Run `database/schema.sql` in MySQL Workbench. The app also creates the database and tables by itself.
3. Double-click **`RUN_ON_WINDOWS.bat`** — it asks for your MySQL password, then starts the app.
   Or manually:
   ```bat
   set DB_USERNAME=root
   set DB_PASSWORD=your_password
   mvn spring-boot:run
   ```
4. Open http://localhost:8080
5. Check http://localhost:8080/api/health → `{"status":"UP","database":"connected"}`

IntelliJ / Eclipse: import as Maven project and run `PeerRentalApplication.java`.

### Configuration (environment variables)
| Variable | Default |
|---|---|
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | *(blank)* |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `campusloop` |
| `PORT` | `8080` |

### Troubleshooting
- **Access denied for user 'root'** → wrong `DB_PASSWORD`.
- **Communications link failure** → MySQL service is not running / wrong port.
- **Port 8080 already in use** → `set PORT=9090` and run again.
- **`mvn` not recognised** → install Maven and add it to PATH, or use IntelliJ.

## API reference
All endpoints except register / login / health require a logged-in session. Errors look like `{"error":"message"}`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | create account (+ auto login) |
| POST | `/api/auth/login` | log in |
| POST | `/api/auth/logout` | log out |
| GET | `/api/auth/me` | current user |
| GET | `/api/health` | app + DB status |
| GET | `/api/items?q=&category=&maxPrice=` | browse available items |
| GET | `/api/items/mine` | my listings |
| POST | `/api/items` | create listing |
| PUT | `/api/items/{id}` | edit my listing |
| DELETE | `/api/items/{id}` | delete my listing (blocked while open bookings exist) |
| POST | `/api/bookings` | request an item `{itemId,startDate,endDate,message}` |
| GET | `/api/bookings/mine` | requests I sent |
| GET | `/api/bookings/received` | requests for my items |
| PATCH | `/api/bookings/{id}/status` | `{"status":"APPROVED"｜"DECLINED"｜"CANCELLED"}` |

## Project structure
```
src/main/java/edu/geu/peerrental/
  config/      password encoder, security-header filter
  model/       User, Item, Booking, BookingStatus (JPA entities)
  repository/  Spring Data JPA repositories (JPQL, row locks)
  dto/         request/response records with validation
  service/     UserService, ItemService, BookingService (transactions)
  web/         REST controllers, session helper, error handler
src/main/resources/
  application.properties
  static/      index.html, style.css, app.js (frontend)
database/schema.sql
```

## OS + DBMS concepts for viva
- **Threads:** Tomcat handles each HTTP request on a worker thread from a pool → many users at once.
- **Critical section / synchronisation:** `BookingService.changeStatus` takes a **pessimistic row lock** (`SELECT … FOR UPDATE`) on the booking and then on the item. Two approvals for the same item are serialised; the second one waits, then sees the first approval in the **date-overlap check** and is rejected. Lock order (booking → item) is always the same, which avoids deadlock.
- **ACID:** every multi-step workflow is a `@Transactional` method on InnoDB (atomic commit / rollback, isolation through locks, durable storage).
- **Integrity:** foreign keys, NOT NULL, unique email, CHECK constraints (in `schema.sql`), indexes on search/lookup columns.
- **Security:** BCrypt hashing, HTTP-only + SameSite session cookie, session-ID rotation on login, server-side ownership checks, JPA parameterised queries (no SQL injection), HTML-escaped rendering + CSP (XSS), JSON-only write endpoints.

## Important
This is a local academic prototype and has not been security-audited or load-tested. Before any public deployment add HTTPS, CSRF tokens, rate limiting / login lockout, verified university e-mail, and automated tests.
