# Bank4Z 

A Gen Z-first digital banking app for South Africa, built as an elective project combining **Cyber Security** and **Systems Integration**.

Bank4Z isn't just "a banking app with slang slapped on it", the goal is a proper, secure, well-integrated banking backend that happens to talk to its users the way Gen Z actually talks to each other. Security and system design are the substance. The tone is the delivery.

---

## Table of contents

- [Why Bank4Z](#why-bank4z)
- [Tech stack](#tech-stack)
- [Architecture overview](#architecture-overview)
- [Features by elective](#features-by-elective)
- [The Gen Z voice — copywriting guide](#the-gen-z-voice--copywriting-guide)
- [Getting started](#getting-started)
- [Project structure](#project-structure)
- [Iteration plan](#iteration-plan)
- [Author / academic context](#author--academic-context)

---

## Why Bank4Z

South African banks have made "youth accounts," but most of them are just stripped-down versions of the parent bank's app — same tone, same UI, same forms, just with a lower fee. Nobody's actually designed for how Gen Z reads, feels, and reacts to their own finances.

Bank4Z's angle: keep the actual banking rock-solid (real auth, real fraud detection, real transaction integrity) but make the *experience* feel like it was built by and for people who grew up texting in lowercase and screenshotting their bank balance to a group chat for validation.

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Backend | Java + Spring Boot | Mature security tooling (Spring Security) + strong integration tooling (Spring Integration/Camel) |
| Database | PostgreSQL | Reliable relational storage for financial data, strong audit/encryption support |
| Frontend/Mobile | React Native / Flutter | Single codebase, mobile-first |
| Auth | Spring Security + JWT | Demonstrates Cyber Security elective directly |
| Messaging | Apache Kafka / RabbitMQ | Async transaction events — demonstrates Systems Integration elective |
| Testing/scanning | Postman + OWASP ZAP | Actual security testing, not just claims |
| Containerization | Docker | Clean service boundaries, easy to demo |

## Architecture overview

Full diagrams live in [`Bank4Z_UML_Diagrams.md`](./Bank4Z_UML_Diagrams.md). Summary of the flow:

```
Client App → API Gateway (Security) → Application Services → Message Broker/Integration → Database
```

Four core services sit behind the gateway: **Auth**, **Account & Transactions**, **Fraud Detection**, and **Notification**. A cross-cutting security layer (Spring Security, audit logging, session management) wraps all of them.

## Features by elective

### Cyber Security
- JWT-based authentication with refresh tokens
- Role-based access control (user/admin)
- Rule-based fraud/anomaly detection (large transfers, rapid repeat transfers, new device logins)
- Session and device management (view + revoke active sessions)
- Immutable audit logging on every sensitive action
- OWASP ZAP scan pass with documented findings and fixes

### Systems Integration
- Async event-driven architecture via Kafka/RabbitMQ
- Simulated integration with an external payment rail (mock PayShap)
- Notification microservice consuming transaction events independently
- Documented service boundaries showing how this would extend to real external systems in production

## The Gen Z voice — copywriting guide

This is the part that makes Bank4Z feel like it was actually made *for* the userbase instead of just *marketed* at them. Every system message, error, and notification gets rewritten in-voice. Rule of thumb: **the underlying logic and security never change — only the copy does.**

| Situation | Standard banking copy | Bank4Z voice |
|---|---|---|
| Insufficient funds | "Insufficient funds for this transaction" | "Jaden broke and so are you..." |
| Insufficient funds (alt) | "Your balance is too low" | "Young money? No money" |
| Transfer successful | "Transaction completed successfully" | "Sent! Money moved, a few Rands broker" |
| Suspicious activity flagged | "This transaction has been flagged for review" | "It's not that I don't trust you, buut dis you? Prove it" |
| Login from new device | "New device login detected" | "New phone who dis 📱 confirm it's really you" |
| Account created | "Your account has been created" | "Welcome to the plug, you're in" |
| Session expired | "Your session has expired, please log in again" | "Bestie you got timed out, log back in" |
| Password changed | "Your password was successfully updated" | "New password, who dis" |
| Weak password | "Password does not meet security requirements" | "That password's mid, try harder" |
| Transfer limit exceeded | "You have exceeded your daily transfer limit" | "Uhmmm, Slow down, what's the rush" |

**Guardrails for this system (important for your writeup):**
- The **fun copy is presentation-layer only** — it never replaces proper error codes, logs, or backend validation messages. Internally, everything still logs as `INSUFFICIENT_FUNDS`, `FRAUD_FLAG_RAISED`, etc. The slang is what the *user* sees; the audit trail stays formal and professional.
- Security-critical messages (fraud flags, suspicious login) keep the playful tone but never sacrifice clarity — the user still needs to immediately understand something serious is happening.
- Keep a single `messages.properties`-style file (or equivalent JSON) so all copy lives in one place and can be toggled to a "formal mode" if needed for demo/marking purposes.

## Getting started

```bash
# clone the repo
git clone <your-repo-url>
cd bank4z

# start dependencies (Postgres, Kafka) via Docker
docker-compose up -d

# run the backend
./mvnw spring-boot:run

# run the frontend
cd mobile-app
npm install
npm start
```

Update `application.yml` / `.env` with your local DB and broker credentials before first run.

## Project structure

```
bank4z/
├── backend/
│   ├── auth-service/
│   ├── account-service/
│   ├── fraud-service/
│   ├── notification-service/
│   └── common/            # shared DTOs, security config, messages
├── mobile-app/             # React Native / Flutter frontend
├── docs/
│   ├── Bank4Z_UML_Diagrams.md
│   └── README.md
├── docker-compose.yml
└── pom.xml
```

## Iteration plan

Built across 5 iterations (15 working days) + buffer:

1. Foundation & Auth
2. Core Banking Features
3. Cyber Security Layer
4. Systems Integration Layer
5. Polish, Testing, Demo Prep

Full ticket breakdown lives in the project wiki / board.

## Author Aphiwe Ashly Vuba

Built as an elective project combining **Cyber Security** and **Systems Integration**. Third-party integrations (e.g. PayShap) are simulated/mocked for academic purposes.
