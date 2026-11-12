# Bank4Z 💸

A Gen Z-first digital banking app for South Africa, built as an elective project combining **Cyber Security** and **Systems Integration**.

Bank4Z isn't just "a banking app with slang slapped on it". The goal is a proper, secure, well-integrated banking backend that happens to talk to its users the way Gen Z actually talks to each other. Security and system design are the substance. The tone is the delivery.

---

## Table of contents

- [Why Bank4Z](#why-bank4z)
- [Tech stack](#tech-stack)
- [Architecture overview](#architecture-overview)
- [Features by elective](#features-by-elective)
- [API overview](#api-overview)
- [Fraud detection](#fraud-detection)
- [Sessions and devices](#sessions-and-devices)
- [The Gen Z voice](#the-gen-z-voice)
- [Running the app](#running-the-app)
- [Running the tests](#running-the-tests)
- [Troubleshooting](#troubleshooting)
- [Project structure](#project-structure)
- [Iteration plan and progress](#iteration-plan-and-progress)
- [Author / academic context](#author--academic-context)

---

## Why Bank4Z

South African banks have made "youth accounts", but most of them are stripped-down versions of the parent bank's app: same tone, same UI, same forms, just with a lower fee. Nobody has really designed for how Gen Z reads, feels, and reacts to their own finances.

Bank4Z's angle: keep the actual banking rock-solid (real auth, real access control, real transaction integrity) but make the *experience* feel like it was built by and for people who grew up texting in lowercase.

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.3 | Mature security tooling (Spring Security) and strong integration tooling |
| Database | PostgreSQL 16 | Reliable relational storage for financial data |
| Migrations | Flyway | Versioned, repeatable schema changes |
| Mobile app | Flutter | One codebase, animation-friendly, compiled to native code |
| Auth | Spring Security + JWT (access + refresh tokens) | Stateless, role-aware authentication |
| Token storage (app) | flutter_secure_storage | Keychain / Keystore, not plain preferences |
| Containers | Docker + Docker Compose | Reproducible local environment |
| CI | GitHub Actions | Build, test, analyse and security-scan on every push |
| Planned (Iteration 4) | Apache Kafka / RabbitMQ | Async transaction events |

## Architecture overview

Full UML diagrams live in [`docs/Bank4Z_UML_Diagrams.md`](./docs/Bank4Z_UML_Diagrams.md).

```
Flutter app -> API (Spring Security: JWT + roles) -> Services -> PostgreSQL
```

The backend is currently a modular monolith: one deployable app, split into packages that map onto the future microservices (`authservice`, `accountservice`, `notificationservice`, `fraudservice`, `adminservice`). Shared code (security config, error handling, validation) lives in `common`. Iteration 4 pulls notifications and fraud checks apart behind a message broker.

## Features by elective

### Cyber Security
- JWT authentication with separate access and refresh tokens (typed, so one can't be used as the other)
- BCrypt password hashing; vague login errors so attackers can't probe which emails exist
- Role-based access control: `/api/admin/**` is protected twice (URL rule + `@PreAuthorize`), deny-by-default everywhere else
- Clean 401 (not logged in) vs 403 (not allowed) responses
- Roles are read from the database on every request, so a demotion takes effect immediately
- No endpoint can create or promote an admin; the first admin comes from environment variables only
- SA ID validation (real date + Luhn checksum), phone and password rules
- Money movement under pessimistic row locks (no double-spend races, no deadlocks)
- Admin views expose the minimum data needed (no ID numbers or hashes)
- Server-side login sessions: list your devices, log out one device or all others, and a revoked device stops working immediately
- Refresh token rotation with reuse detection: every refresh token works once; replaying an old one ends the whole session
- "New phone who dis" alerts when someone logs in from a device you haven't used before
- Rule-based fraud detection on every transfer (rapid-fire transfers are blocked, large ones are flagged for admin review); customers never see the thresholds
- Secrets via environment variables; `.env` is git-ignored
- Automated CodeQL scan in CI

Coming in Iteration 3: an OWASP ZAP pass and audit logging.

### Systems Integration
- Versioned database migrations (Flyway), validated against a real Postgres in CI
- Modular service boundaries ready to split into separate services
- Transfers create transactions and notifications atomically
- CI pipeline: build + test, Flutter analysis, and security scanning

Coming in Iteration 4: message broker, a simulated external payment rail (mock PayShap), and a standalone notification consumer.

## API overview

| Method | Path | Access | What it does |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create an account (a bank account is created automatically) |
| POST | `/api/auth/login` | public | Get access + refresh tokens |
| POST | `/api/auth/refresh` | public | Swap a refresh token for new tokens (the old refresh token stops working) |
| GET | `/api/accounts/me` | user | Balance and account details |
| GET | `/api/accounts/me/transactions?page=&size=` | user | Paginated transaction history |
| POST | `/api/transfers` | user | Send money to another Bank4Z account |
| GET | `/api/notifications?page=&size=` | user | Paginated notifications |
| GET | `/api/notifications/unread-count` | user | Unread badge count |
| POST | `/api/notifications/{id}/read` | user | Mark one notification read |
| POST | `/api/notifications/read-all` | user | Mark all notifications read |
| GET | `/api/sessions` | user | List your active devices (`current` marks this one) |
| DELETE | `/api/sessions/current` | user | Log out this device |
| DELETE | `/api/sessions/{id}` | user | Log out one specific device |
| POST | `/api/sessions/revoke-others` | user | Log out every other device |
| GET | `/api/admin/users?page=&size=` | admin | List users |
| PUT | `/api/admin/accounts/{accountNumber}/status` | admin | Freeze / unfreeze an account |
| GET | `/api/admin/fraud-flags?resolved=&page=&size=` | admin | Review fraud flags (`resolved=false` = still open) |
| POST | `/api/admin/fraud-flags/{id}/resolve` | admin | Mark a fraud flag as reviewed |

Protected endpoints need the header `Authorization: Bearer <accessToken>`. Access tokens last 1 hour. `POST /api/auth/login` also accepts optional `deviceId` and `deviceName` fields; without them the login shows up as "Unknown device".

Errors share one shape. The `message` is the fun, user-facing copy; the `code` is the stable, formal identifier:

```json
{
  "timestamp": "2026-10-10T12:00:00Z",
  "message": "Jaden broke and so are you 😭",
  "code": "INSUFFICIENT_FUNDS"
}
```

## Fraud detection

Every transfer is checked **after** basic validation and **before** any money moves. Thresholds live in `src/main/resources/application.yml` under `bank4z.fraud`.

| Rule | Triggers when (defaults) | Risk | What happens |
|---|---|---|---|
| `RAPID_TRANSFERS` | 5 completed transfers out of the same account within 10 minutes | HIGH | Transfer is **blocked**. Nothing is debited. The attempt is recorded (status `FLAGGED`, shown as "On hold" in the app), a flag is raised, and the user is notified. The window slides, so it clears itself. |
| `LARGE_TRANSFER` | Amount of R5,000 or more | MEDIUM | Transfer **goes through**, a flag is raised for admin review, and the user is notified. |

How it fits together:
- A blocked transfer returns HTTP 403 with `code: FRAUD_BLOCKED` and a deliberately vague message. The exact limits stay admin-only (they're in the flag's `reason`), so an attacker can't learn the threshold to stay under it.
- The sender's account row is locked first, so two simultaneous transfers can't both slip under the rapid-transfer limit.
- Admins review flags with `GET /api/admin/fraud-flags?resolved=false` and close them with `POST /api/admin/fraud-flags/{id}/resolve`.
- New-device logins are handled by session management (see below); they alert the user but don't block.

To try it: send R1.00 to another account six times in a row. The sixth is blocked. To try the large-transfer rule, give the sender R6,000 in pgAdmin and send R5,000.

## Sessions and devices

Every login creates a **session** on the server (one row per login, tied to a device). Both tokens carry the session id, and every authenticated request checks that the session is still active. That is what makes "log out that device" work straight away, instead of the token staying usable for up to an hour.

- **Devices screen** (app) / `GET /api/sessions`: see where you're logged in, log out one device, or log out all the others.
- **Refresh token rotation:** each refresh hands out a new refresh token and retires the old one. If an old one is ever presented again (a bug or a stolen token), the entire session is revoked.
- **New-device alert:** the app generates a random device id once per install. A login from an id the user has never used before (after their first login) creates a "New phone who dis 📱" notification.
- **Not trusted:** device names are cleaned (control characters stripped, length capped) and the IP is the address the server actually sees, never the forgeable `X-Forwarded-For` header.

Try it in PowerShell (two "devices" for the same user):

```powershell
$body = '{"email":"alex@example.com","password":"Passw0rdTest","deviceId":"phone-1","deviceName":"Alex Pixel"}'
$phone = Invoke-RestMethod -Method Post -Uri "$api/auth/login" -ContentType "application/json" -Body $body

$body = '{"email":"alex@example.com","password":"Passw0rdTest","deviceId":"laptop-1","deviceName":"Alex Laptop"}'
$laptop = Invoke-RestMethod -Method Post -Uri "$api/auth/login" -ContentType "application/json" -Body $body
# the second login created a "New phone who dis" notification for Alex

$laptopHeaders = @{ Authorization = "Bearer $($laptop.accessToken)" }
Invoke-RestMethod -Uri "$api/sessions" -Headers $laptopHeaders                                 # both devices listed
Invoke-RestMethod -Method Post -Uri "$api/sessions/revoke-others" -Headers $laptopHeaders      # log out the phone

# the phone's token (still unexpired) is now rejected straight away:
try { Invoke-RestMethod -Uri "$api/accounts/me" -Headers @{ Authorization = "Bearer $($phone.accessToken)" } } catch { $_.ErrorDetails.Message }
```

Tokens issued before this feature existed have no session, so they stop working: log in again once after updating.

## The Gen Z voice

Every user-facing message gets rewritten in-voice. Rule of thumb: **the underlying logic and security never change, only the copy does.**

| Situation | Standard banking copy | Bank4Z voice |
|---|---|---|
| Insufficient funds | "Insufficient funds for this transaction" | "Jaden broke and so are you 😭" |
| Transfer successful | "Transaction completed successfully" | "Sent! Money moved, no cap 💸" |
| Sending to yourself | "Cannot transfer to the same account" | "You can't send money to yourself, bestie" |
| Unknown account number | "Recipient account not found" | "We couldn't find that account number 🤷" |
| Not logged in | "Authentication required" | "You need to log in first, bestie 🔐" |
| No permission | "Access denied" | "Nice try 😏 you don't have access to that" |
| Validation failed | "Request validation failed" | "Some of that doesn't check out 🧐" |
| Account created | "Your account has been created" | "Welcome to the plug 🔌 you're in" |
| Money received | "Credit received" | "Money moved! R40.50 landed from 4Z... 🤑" |
| Empty notifications | "No notifications" | "All quiet... for now 🤫" |
| Transfer blocked by fraud rule | "Transaction declined: suspected fraud" | "Hold up, this looks sus 👀 We paused that transfer to keep your money safe." |
| Large transfer flagged | "Transaction flagged for review" | "Big move 👀 ... went through, but we flagged it for a quick safety check." |
| Login from a new device | "New device login detected" | "New phone who dis 📱 Someone just logged in on ... Not you? Open Devices and log out other devices." |
| Session ended | "Session expired" | "Session ended, log back in bestie" |

Still planned:

| Situation | Bank4Z voice |
|---|---|
| Daily limit reached | "Slow down bestie, you hit your daily cap" |

**Guardrails (worth saying out loud in a demo):**
- The fun copy is presentation-layer only. Logic, logs and the `code` field stay formal (`INSUFFICIENT_FUNDS`, `FORBIDDEN`, ...).
- Security-critical messages keep the playful tone but never sacrifice clarity.
- Copy currently lives next to the code that raises it; it is slated to move into one central messages file.

## Running the app

### Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 (not newer) | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker Desktop (with Compose) | recent | `docker --version` |
| Flutter SDK | stable | `flutter doctor` |

> **Heads up:** the project targets Java 21. If your IDE or `JAVA_HOME` points at a newer JDK (e.g. 26), compilation fails with `TypeTag :: UNKNOWN`. Point both at 21.

### 1. Clone and configure

```bash
git clone <your-repo-url>
cd Bank4Z
```

Create your local environment file (it is git-ignored, never commit it):

```powershell
# Windows PowerShell
Copy-Item .env.example .env
```
```bash
# macOS / Linux
cp .env.example .env
```

Open `.env` and set a real **`JWT_SECRET`**. It must be **at least 32 characters**, otherwise the backend refuses to start.

```powershell
# Windows PowerShell: generate a secret
$bytes = New-Object byte[] 48
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```
```bash
# macOS / Linux
openssl rand -base64 48
```

Optionally set `ADMIN_EMAIL` and `ADMIN_PASSWORD` (12+ characters) to have an admin account created on startup. Leave them blank to skip.

### 2. Start the backend

**Option A: everything in Docker (simplest)**

```bash
docker-compose up -d --build
```

This starts PostgreSQL, the backend (on port 8080), and pgAdmin (on port 5050). Flyway applies the database migrations automatically on first start. (Newer Docker installs also accept `docker compose` without the hyphen.)

**Option B: backend in your IDE (faster while developing)**

```bash
docker-compose up -d postgres
```

Then run `BackendApplication` from IntelliJ. In its Run Configuration, add environment variables `JWT_SECRET` (32+ chars) and, optionally, `ADMIN_EMAIL` / `ADMIN_PASSWORD`. IntelliJ does not read `.env` by itself. The database settings default to `localhost:5432`, which matches the compose file.

### 3. Check it's actually up

```powershell
docker-compose ps
docker-compose logs backend --tail 50
```

Look for a line containing `Started BackendApplication`. Then:

```powershell
curl.exe -i http://localhost:8080/api/admin/users
```

A `401` with a JSON body ("You need to log in first, bestie 🔐") means the backend is running and protecting its endpoints. "Unable to connect" means it is not running; see [Troubleshooting](#troubleshooting).

> **Windows note:** in PowerShell, plain `curl` is an alias for `Invoke-WebRequest` with different syntax. Use `curl.exe` for real curl, or the `Invoke-RestMethod` examples below.

### 4. Try the API (PowerShell)

Two test users (the IDs below are valid SA ID numbers that pass the Luhn check):

```powershell
$api = "http://localhost:8080/api"

# Register two users
Invoke-RestMethod -Method Post -Uri "$api/auth/register" -ContentType "application/json" `
  -Body '{"fullName":"Alex Test","email":"alex@example.com","idNumber":"8001015009087","phoneNumber":"0821234567","password":"Passw0rdTest"}'

Invoke-RestMethod -Method Post -Uri "$api/auth/register" -ContentType "application/json" `
  -Body '{"fullName":"Sam Test","email":"sam@example.com","idNumber":"9202204720083","phoneNumber":"0839876543","password":"Passw0rdTest"}'

# Log in as Alex and keep the token
$login = Invoke-RestMethod -Method Post -Uri "$api/auth/login" -ContentType "application/json" `
  -Body '{"email":"alex@example.com","password":"Passw0rdTest"}'
$headers = @{ Authorization = "Bearer $($login.accessToken)" }

# Alex's account (note the account number, it starts with 4Z)
Invoke-RestMethod -Uri "$api/accounts/me" -Headers $headers
```

New accounts start with R0.00. Add test money with pgAdmin (`http://localhost:5050`, login `admin@bank4z.local` / `admin`, then connect to host `postgres`) or `psql`:

```sql
UPDATE accounts SET balance = 1000 WHERE account_number = '4Z...';  -- Alex's number
```

Then send money to Sam (use Sam's account number from logging in as Sam and calling `/accounts/me`):

```powershell
Invoke-RestMethod -Method Post -Uri "$api/transfers" -Headers $headers -ContentType "application/json" `
  -Body '{"recipientAccountNumber":"4Z...","amount":250.00,"reference":"Pizza night"}'
```

`Invoke-RestMethod` hides the body of error responses (where the Gen Z messages live). To see them:

```powershell
try { Invoke-RestMethod -Uri "$api/admin/users" } catch { $_.ErrorDetails.Message }
```

**Admin endpoints.** Set `ADMIN_EMAIL` / `ADMIN_PASSWORD` in `.env` and restart the backend, or promote an existing user in the database:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'alex@example.com';
```

Roles are checked against the database on every request, so the promotion applies immediately. Then call `GET /api/admin/users` with that user's token.

### 5. Run the mobile app

```bash
cd mobile-app
flutter pub get
flutter run
```

Start an Android emulator (or connect a device with USB debugging) first, and confirm `flutter devices` lists it.

The app's API address is set in `mobile-app/lib/config/api_config.dart`:

| Where the app runs | Base URL |
|---|---|
| Android emulator | `http://10.0.2.2:8080/api` (default) |
| iOS simulator | `http://localhost:8080/api` |
| Physical phone | `http://<your-computer's-LAN-IP>:8080/api` (same Wi-Fi) |

### 6. Stop everything

```bash
docker-compose down        # stop, keep database data
docker-compose down -v     # stop AND wipe the database (fresh start)
```

## Running the tests

```bash
mvn clean verify                 # backend: compile + unit tests
cd mobile-app && flutter analyze # app: static analysis
cd mobile-app && flutter test    # app: tests (none yet)
```

The same checks run automatically on every push and pull request via GitHub Actions (`.github/workflows/ci.yml`), plus a CodeQL security scan. The backend CI job runs against a real PostgreSQL container, so migrations are validated every time.

## Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| "Unable to connect to the remote server" / connection refused on `localhost:8080` | The backend isn't running. Run `docker-compose ps`. If `backend` is `Exited` or restarting, read `docker-compose logs backend --tail 100` and match the error below. If Docker commands themselves fail, start Docker Desktop. |
| Backend log: `WeakKeyException` / "not secure enough for any JWT HMAC-SHA algorithm" | `JWT_SECRET` is shorter than 32 characters (the placeholder in `.env.example` is too short on purpose). Generate a real one, see step 1. |
| Backend log: `password authentication failed for user` | The database volume was created with different credentials. `docker-compose down -v`, then `docker-compose up -d --build` (this wipes local data). |
| Backend log: `Flyway ... checksum mismatch` or `Validate failed` | A migration was edited after it was applied. Never edit applied migrations; add a new `V<n>__...sql`. For local dev, `docker-compose down -v` resets. |
| Backend log: `Schema-validation: missing table/column` | Migrations weren't found. They must be in `src/main/resources/db/migration/` and named `V1__description.sql` (two underscores). |
| `port is already allocated` / `address already in use` (5432, 8080, 5050) | Another program is using that port (often a locally installed PostgreSQL on 5432). Stop it or change the left side of the `ports:` mapping in `docker-compose.yml`. |
| `javac 26` / `TypeTag :: UNKNOWN` / `ExceptionInInitializerError` | IDE compiling with the wrong JDK. Set Project SDK and Maven JDK to 21. |
| `cannot find symbol` on a class that "should" exist | The file is missing or empty in the repo. Check `git status`; a new file showing 0 added lines is the giveaway. |
| PowerShell: `Cannot bind parameter 'Headers'` | `curl` is an alias for `Invoke-WebRequest`. Use `curl.exe` or `Invoke-RestMethod` with `-Headers @{ ... }`. |
| App works in PowerShell but not on the Android emulator | The emulator can't see your `localhost`. Use `http://10.0.2.2:8080/api`. |
| Everything worked, then 401s after about an hour | The access token expired. Log in again (silent token refresh in the app is not built yet). |
| Suddenly 401 on a device that was working | That device's session was ended (logged out from another device, or a reused refresh token was detected). Log in again. Also expected once after upgrading to sessions: old tokens have no session. |

## Project structure

```
Bank4Z/
├── .github/workflows/ci.yml
├── docs/
│   └── Bank4Z_UML_Diagrams.md
├── mobile-app/
│   ├── pubspec.yaml
│   └── lib/
│       ├── main.dart
│       ├── config/        (API base URL)
│       ├── models/        (account, transaction, transfer result, notification, device session)
│       ├── services/      (auth, account, notification, session API clients)
│       └── screens/       (login, register, home, transfer, notifications, devices)
├── src/
│   ├── main/
│   │   ├── java/com/bank4z/backend/
│   │   │   ├── BackendApplication.java
│   │   │   ├── authservice/          (users, registration, login, JWT principal)
│   │   │   ├── accountservice/       (accounts, transactions, transfers)
│   │   │   ├── notificationservice/  (in-app notifications)
│   │   │   ├── adminservice/         (admin endpoints, first-admin bootstrap)
│   │   │   ├── sessionservice/       (login sessions, devices, refresh rotation)
│   │   │   ├── fraudservice/         (fraud rules, flags, admin review)
│   │   │   └── common/               (config, security, exceptions, validation)
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/         (V1 schema, V2 phone number, V3 notifications, V4 fraud flags, V5 sessions)
│   └── test/java/...                 (unit and web-layer security tests)
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .gitignore
└── pom.xml
```

## Iteration plan and progress

| Iteration | Focus | Status |
|---|---|---|
| 1 | Foundation and auth (B4Z-01 to 05) | Done |
| 2 | Core banking: accounts, history, transfers, notifications (B4Z-06 to 09) | Done |
| 3 | Cyber Security layer: RBAC (B4Z-10), fraud detection (B4Z-11) and sessions/devices (B4Z-12) done; security testing and audit logging next (B4Z-13 to 14) | In progress |
| 4 | Systems Integration: message broker, mock payment rail, notification consumer (B4Z-15 to 18) | Planned |
| 5 | Polish, end-to-end testing, documentation, demo (B4Z-19 to 23) | Planned |

## Author / academic context

Built as an elective project combining **Cyber Security** and **Systems Integration**. Third-party integrations (e.g. PayShap) are simulated for academic purposes where real sandbox access isn't available to students. This is documented explicitly rather than implied as a live integration.