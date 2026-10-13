# MelonMart 🍈

**Multi-seller grocery marketplace** built for Anna University R2025 Semester 3 using Java Servlets, JDBC, Apache Tomcat, Maven, and H2.

> **Project identity is intentionally preserved.** This repository remains **MelonMart**, with the existing `com.melon.melonmart` package, pages, and visual design. The YourNameMart specification is used as a requirements checklist only; it does not rename or replace the project.

- **Live application:** https://melonmart-app-production.up.railway.app/melonmart/
- **Health endpoint:** `/melonmart/api/v1/health` (also check `/melonmart/api/health` if using the legacy route)
- **Java:** 17
- **Servlet API:** Tomcat 9 / Servlet 4 / `javax.servlet.*`
- **Build:** Maven WAR
- **Database:** H2 file database on persistent deployment storage
- **CI:** GitHub Actions — `mvn -B clean verify`

**Status honesty:** This README records source-level findings and the remaining verification work. A feature being present in source does not, by itself, prove that every live, security, load, or end-to-end test has passed.

---

## Contents

- [Problem statement and goals](#problem-statement-and-goals)
- [Roles and features](#roles-and-features)
- [Technology stack](#technology-stack)
- [Architecture](#architecture)
- [Database design](#database-design)
- [Repository guide](#repository-guide)
- [Run locally](#run-locally)
- [Configuration and deployment](#configuration-and-deployment)
- [API conventions](#api-conventions)
- [Security](#security)
- [Testing and validation](#testing-and-validation)
- [Course requirements audit](#course-requirements-audit)
- [Diagrams and submission deliverables](#diagrams-and-submission-deliverables)
- [Development workflow](#development-workflow)
- [Known gaps and next steps](#known-gaps-and-next-steps)
- [Change history](#change-history)

## Problem statement and goals

A multi-seller marketplace needs a single place for sellers to manage listings and incoming orders, buyers to discover products and complete a mock checkout, and administrators to oversee users, orders, and listings. MelonMart provides these flows through a browser-based application backed by a Java Servlet/JDBC application and a persistent H2 database.

### Goals

1. Provide role-based Buyer, Seller, and Admin experiences.
2. Support the core journey: register/login → browse/search → cart → mock checkout → order history.
3. Keep product ownership enforced on the server.
4. Store passwords as bcrypt hashes and use parameterized SQL.
5. Maintain a deployable WAR, repeatable database migrations, automated build checks, diagrams, and setup documentation.
6. Preserve the current MelonMart UI and working functionality while fixing gaps incrementally.

## Roles and features

| ID | Requirement | Current source-level assessment | Evidence still needed |
|---|---|---|---|
| F1 | Registration/login; Buyer and Seller roles; seeded Admin | Auth endpoints, user DAO, bcrypt, and seeded-admin code exist | Test registration, login, role assignment, logout/session renewal, and protected routes on a test account |
| F2 | Seller creates, edits, and deletes products | Product endpoints and seller dashboard code exist | Verify all CRUD actions and that sellers cannot modify another seller's listings |
| F3 | Browse and search/filter by category and keyword | Catalog and product endpoints exist | Test combined, empty, and unusual keyword/category inputs |
| F4 | Add/update/remove cart items and calculate total | Cart DAO and cart endpoints exist | Test invalid quantities, unavailable stock, and server-calculated totals |
| F5 | Checkout with mock payment confirmation | Order endpoint/DAO and delivery/payment fields exist | Test missing contact/delivery/payment details, stock changes, duplicate submission, and persisted orders |
| F6 | Buyer order history and seller incoming orders | Order endpoints/DAO and seller dashboard code exist | Verify visibility is limited to the correct buyer/seller |
| F7 | Admin users/orders/listing moderation | Admin endpoints/dashboard code exist | Verify non-admin requests are rejected server-side |
| F8 | Product reviews and star ratings after completed orders | Review endpoint and delivered-order eligibility logic exist | Test unauthorized, duplicate, and ineligible reviews |

### Optional features

Source code includes a wishlist, order-status workflow, seller sales dashboard, and Welp chatbot. Each should be tested against the live deployment before it is claimed as fully verified.

### User/account policy for this deployment

- Do not create sample buyer accounts unless they are specifically needed for a test.
- The requested seller email is `seller@melonmart.com`. The production startup logs previously indicated this account did not exist; therefore the unowned-product repair could not run at that time.
- Never publish production passwords in this README, GitHub issues, commits, or source code.
- Existing admin access and existing product ownership must not be reset or overwritten as part of seller setup.

## Technology stack

| Layer | Technology |
|---|---|
| Language/runtime | Java 17 |
| Servlet container | Apache Tomcat 9; Servlet 4, `javax.servlet.*` |
| Build/package | Maven, WAR |
| Database | H2; embedded/local and persistent file database in deployment |
| Connection pool | HikariCP |
| Data access | JDBC, DAO classes, `PreparedStatement` |
| Password hashing | jBCrypt / bcrypt |
| JSON | Gson |
| Frontend | HTML, CSS, vanilla JavaScript, Fetch API |
| Logging | SLF4J and Logback |
| Testing | JUnit 5 and Mockito; H2 test database |
| CI | GitHub Actions |
| Deployment | Docker/Tomcat on Railway; deployment guide also documents other supported setups |

## Architecture

The intended architecture is layered MVC:

```text
Browser (HTML/CSS/vanilla JS, Fetch)
        |
        v
Servlet filters (encoding, request ID, authentication where configured)
        |
        v
HTTP endpoints / Servlets
        |
        v
Service layer (business rules and validation)
        |
        v
DAO layer (parameterized SQL)
        |
        v
HikariCP connection pool
        |
        v
H2 database
```

The connection pool lifecycle is initialized and closed by a servlet-context listener. SQL should remain in DAO/data-access code; controllers should orchestrate HTTP requests rather than contain business rules.

**Known architecture gap:** source review found at least `AuthServlet` constructing `UserDAO` directly. The required service/interface boundary is therefore not yet proven to be consistently enforced throughout the codebase. Refactor this incrementally with tests; do not undertake a risky wholesale rewrite of working flows.

## Database design

The course's minimum logical entities are:

| Table/entity | Purpose | Important relationships |
|---|---|---|
| `users` | Buyer, Seller, and Admin accounts | Unique email; role; bcrypt password hash |
| `products` | Product catalog and inventory | `seller_id` references `users.id` |
| `orders` | Order header, buyer, status, total | `buyer_id` references `users.id` |
| `order_items` | Product snapshot, quantity, unit price | References orders and products |
| `cart_items` | Per-user cart | References users and products |
| `reviews` | Product rating and comment | References products and users |
| `schema_migrations` | Records applied migration versions | Used by startup migration logic |

### Database rules

- Use a unique constraint for email.
- Store currency in `DECIMAL(10,2)`, never `FLOAT`.
- Use foreign keys for ownership and order relationships.
- Index foreign-key columns and frequently queried status/relationship columns.
- Store timestamps on persistent entities.
- Use `PreparedStatement` for all SQL parameters.
- Wrap `Connection`, `PreparedStatement`, and `ResultSet` in try-with-resources.
- Keep seed/demo data separate from schema migrations.
- Back up the persistent H2 database before any schema/data maintenance.

The checked-in schema uses decimal currency columns and the repository has numbered SQL migrations. **A complete foreign-key-index audit of upgraded/legacy production data remains outstanding.**

### Seller ownership repair

The one-time startup repair is intended to assign only products with `seller_id IS NULL` to an existing user whose email is `seller@melonmart.com` and role is `SELLER`. It must not modify products that already belong to another seller.

The last observed Railway startup reported that the demo seller account was absent, so the repair did not execute. Create/register the seller account securely first, then restart/redeploy the app and verify the startup log and seller inventory. Do not delete the Railway volume or reset the database to achieve this.

## Repository guide

| Path | Purpose |
|---|---|
| `src/main/java/com/melon/melonmart/controller/` | HTTP endpoints and servlet controllers |
| `src/main/java/com/melon/melonmart/dao/` | JDBC data access |
| `src/main/java/com/melon/melonmart/model/` | Domain objects/entities |
| `src/main/java/com/melon/melonmart/listener/` | Application/database startup and lifecycle |
| `src/main/java/com/melon/melonmart/filter/` | Request filters, logging/request ID, and access checks where mapped |
| `src/main/resources/schema.sql` | Base schema |
| `src/main/resources/seed.sql` | Idempotent seed/demo data |
| `src/main/resources/db/migrations/` | Numbered database migrations |
| `src/test/` | Automated tests |
| `.github/workflows/build.yml` | Build and test workflow |
| `Dockerfile` | Container build and Tomcat runtime |
| `.env.example` | Documented configuration template; contains no real secrets |
| `docs/DEPLOYMENT.md` | Deployment and database persistence notes |
| `docs/diagrams/erd.md` | D1: entity relationship diagram |
| `docs/diagrams/use-cases.md` | D2: use-case diagram/overview |
| `docs/diagrams/checkout-sequence.md` | D3: place-order sequence |
| `CONTRIBUTING.md` | Contribution and local setup workflow |
| `CHANGELOG.md` | Change history |

## Run locally

### Prerequisites

- JDK 17
- Maven
- Apache Tomcat 9
- Git

### Steps

1. Clone this repository and open its root directory.
2. Configure a **local/test database path** using the variables described below. Do not point a local experiment at the production database.
3. Build and run tests:

   ```bash
   mvn -B clean verify
   ```

4. Deploy `target/melonmart.war` to Tomcat 9.
5. Start Tomcat and open `http://localhost:8080/melonmart/` (adjust the port if your local Tomcat uses another port).
6. Check the health endpoint at `http://localhost:8080/melonmart/api/v1/health`.

The app's startup code applies compatible migrations and initializes the schema/seed data. Review the database URL and back up any existing database before upgrading it. Do not run tests or experiments against production data.

## Configuration and deployment

Configuration is environment-specific. Refer to [`.env.example`](.env.example) and [the deployment guide](docs/DEPLOYMENT.md) for current supported variables.

| Variable | Purpose |
|---|---|
| `MELONMART_DB_URL` | H2 JDBC URL / database location |
| `MELONMART_DB_USERNAME` | Database username, if configured |
| `MELONMART_DB_PASSWORD` | Database password, if configured |
| `GEMINI_API_KEY` | Optional server-side API key for live chatbot responses |
| `MELONMART_ADMIN_EMAIL` | Admin bootstrap email, when the bootstrap path is enabled |
| `MELONMART_ADMIN_PASSWORD` | Admin bootstrap password; keep it in the host's secret settings only |

- Never commit a real `.env`, database password, AI API key, or production account password.
- A public deployment must use unique, strong credentials. Well-known local/demo passwords must never be used in production.
- The Docker configuration disables demo-account seeding by default; review the deployment guide before enabling any demo behavior.
- Attach persistent storage for the H2 file database (the documented default mount is `/data`). Ephemeral filesystem storage can lose data on restart/redeploy.
- Back up the H2 database before migrations, account recovery, or ownership repairs.
- The deployed app is expected under `/melonmart/`; confirm the actual live health route after each deployment.

## API conventions

The course specification requires JSON endpoints under `/api/v1/...` with a consistent response envelope:

Success:
```json
{ "success": true, "data": {}, "error": null }
```

Error:
```json
{ "success": false, "data": null, "error": { "code": "VALIDATION_ERROR", "message": "Invalid input" } }
```

Expected status conventions: `200` success, `201` created, `400` invalid input, `401` unauthenticated, `403` forbidden, `404` not found, `409` conflict, `500` unexpected server error. Validate input before DAO calls; never return password hashes in DTOs.

The repository includes versioned API routes/aliases and a health endpoint. **Audit every endpoint's exact envelope and HTTP status before claiming complete API-contract compliance.**

## Security

Implemented/source-level safeguards include:

- bcrypt password hashing.
- Parameterized SQL in DAO paths.
- Server-side role and ownership checks in protected flows.
- Session invalidation on login and a configured 30-minute session timeout.
- User responses omit password hashes.
- Review eligibility checks require a delivered order containing the reviewed product.
- Request ID propagation into logging and API responses.
- Chatbot input length and per-session rate limits.
- Server-side storage for the chatbot provider API key.

Still required before final sign-off:

- Test SQL injection and reflected/stored XSS with safe test payloads.
- Verify every protected servlet/API rejects missing or wrong-role sessions.
- Confirm every user-supplied value is escaped in HTML output.
- Verify errors do not leak stack traces, secrets, or SQL details.
- Review cookie/session flags and logout behavior.
- Check all SQL call sites use `PreparedStatement`.
- Confirm no secrets are present in Git history or deployment logs.
- Perform tests only with test accounts and a disposable/non-production database.

## Testing and validation

### Automated checks

Run:

```bash
mvn -B clean verify
```

GitHub Actions runs the Maven verification workflow on pushes and pull requests. SpotBugs/static-analysis checks are configured in the build; inspect the actual CI result rather than assuming it passed.

Existing tests cover selected DAO, migration, checkout/stock/status, and password-hash behavior. Add and maintain tests for:

- Registration, login, role assignment, session renewal, logout, and protected routes.
- Servlet request/response status and error envelopes.
- Seller ownership boundaries and admin-only operations.
- Cart quantity/price/stock validation.
- Checkout idempotency, stock concurrency, and order persistence.
- Review eligibility and duplicate review behavior.
- Migration compatibility against a copy of a legacy database.

### Manual end-to-end checklist

- [ ] Register a test Buyer and log in.
- [ ] Register/log in a test Seller.
- [ ] Seller creates, edits, and deletes a test product.
- [ ] Buyer browses, searches, filters, and views product details.
- [ ] Buyer adds/updates/removes cart items and sees the correct server-computed total.
- [ ] Invalid checkout details are rejected with a useful validation message.
- [ ] Successful mock checkout creates an order and updates inventory correctly.
- [ ] Buyer sees only their own order history.
- [ ] Seller sees only incoming items belonging to that seller.
- [ ] Eligible buyer submits a review after delivery; ineligible review is rejected.
- [ ] Admin views users/orders and moderates listings; Buyer/Seller cannot access admin functions.
- [ ] Refresh/restart confirms database persistence and health status.
- [ ] Check all flows at mobile and tablet widths.

### Load test

The course requirement is **at least 10 concurrent users for 60 seconds** using JMeter or `ab`. This README has no verified load-test result yet. Run it against a test/staging dataset, record the command/configuration, response/error rate, and results, and never load-test production without a safe plan.

## Course requirements audit

This checklist maps the supplied YourNameMart specification to MelonMart. It does not imply that unchecked tests have passed.

| Specification area | Assessment | Remaining action |
|---|---|---|
| F1–F8 core features | Source implementations exist | Complete the deployed end-to-end checklist above |
| O1 wishlist | Source implementation exists | Test persistence and role/access rules |
| O2 order status workflow | Source implementation exists | Test valid transitions and forbidden transitions |
| O3 seller dashboard | Source implementation exists | Reconcile order totals and revenue against database data |
| O4 AI chatbot | Welp UI/backend code exists | Test mock mode, live provider mode, timeout/fallback, scope guardrails, caching, and rate limits |
| MVC / layered services | Partially compliant by source review | Remove direct servlet-to-DAO coupling and use service interfaces consistently |
| JDBC safety | DAO layer and migrations exist | Audit every SQL call site and resource cleanup |
| bcrypt/session security | Safeguards exist in source | Run auth/session/security regression tests |
| Database constraints/indexes | Schema/migrations exist; currency uses DECIMAL | Verify every FK index on current and upgraded databases |
| JUnit/Mockito/DAO tests | Some tests exist | Add servlet/auth/filter and full journey coverage |
| CI | GitHub Actions configured | Keep all workflow runs green and review them on every PR |
| D1 ER diagram | `docs/diagrams/erd.md` | Ensure diagram reflects actual schema |
| D2 use-case diagram | `docs/diagrams/use-cases.md` | Ensure Buyer, Seller, Admin and F1–F8 are represented |
| D3 checkout sequence | `docs/diagrams/checkout-sequence.md` | Ensure browser → servlet → service → DAO → DB and response path are shown |
| README/setup/deployed URL | This document and deployment guide | Keep commands and routes synchronized with the live app |
| `.env.example` / `.gitignore` | Files exist | Ensure every required setting is documented and no secret is tracked |
| `CONTRIBUTING.md` / `CHANGELOG.md` | Files exist | Keep them updated with future changes |
| Security checklist | Partially documented | Complete the manual tests above and retain evidence |
| Load test | No verified evidence recorded here | Run 10 concurrent users for 60 seconds on test data |
| Final report/slides/demo video | Not verified by repository source review | Prepare and rehearse the final assessment deliverables |

### Engineering standards to retain

- Prepared statements for all SQL.
- bcrypt, never plaintext or MD5/SHA-1-only password storage.
- explicit session timeout and session ID renewal at login.
- output escaping for user-supplied content.
- a single connection-pool lifecycle managed by the context listener.
- try-with-resources for JDBC resources.
- DTOs separate from entities; response DTOs must never expose password hashes.
- service methods validate input before DAO calls.
- numbered, checked-in migration files; never hand-edit the live database.
- Checkstyle/SpotBugs and tests in CI.
- Javadoc for public service/DAO classes and methods.
- Document pattern usage only where it is actually implemented: DAO, Front Controller, connection-pool singleton/lifecycle, Factory, Strategy for mock payment/provider, and Builder for complex DTOs. Do not claim a pattern without source evidence.

## Diagrams and submission deliverables

The three required diagrams are linked here:

1. **D1 — ER diagram:** [docs/diagrams/erd.md](docs/diagrams/erd.md) — entities, keys, and relationships.
2. **D2 — Use-case diagram:** [docs/diagrams/use-cases.md](docs/diagrams/use-cases.md) — Buyer, Seller, Admin and their use cases.
3. **D3 — Checkout sequence:** [docs/diagrams/checkout-sequence.md](docs/diagrams/checkout-sequence.md) — browser, servlet, service, DAO, database, and response path.

Final-review deliverables also include a report covering architecture and technical decisions, known limitations, a slide deck, a rehearsed demo script, and a backup screen recording. Their completion is not confirmed by this source review.

## Development workflow

1. Create or use a GitHub issue for each feature/bug with description, motivation, acceptance criteria, and affected layers.
2. Record impact: schema/migration needed? API response shape changed? shared behavior affected?
3. Work on a feature/fix/docs branch and open a self-reviewed pull request.
4. Keep `main` deployable; do not push secrets or production data.
5. Require passing CI before merging; update tests and docs in the same change.
6. Use conventional commit messages such as `feat:`, `fix:`, `test:`, and `docs:`.
7. Keep `CHANGELOG.md` updated and maintain the requested sprint retro/project-board evidence where required.
8. Never delete/reset the persistent production database to fix a feature or account issue.

## Known gaps and next steps

Recommended order:

1. Securely create/register `seller@melonmart.com` with a strong unique password; do not publish the password.
2. Restart/redeploy only after confirming the intended code and configuration. Verify the one-time migration assigns only products whose seller is null and leaves other sellers' products untouched.
3. Run `mvn -B clean verify` and confirm the GitHub Actions workflow is green.
4. Complete the F1–F8 manual journey on the deployed site with test accounts.
5. Add automated servlet/auth/filter tests and document the end-to-end run.
6. Audit all DAO calls, SQL parameterization, DTO exposure, output escaping, session checks, and API status/envelope consistency.
7. Verify foreign-key indexes on a copy of the production schema.
8. Run the 10-user/60-second load test on staging/test data and record evidence.
9. Update unchecked items only after tests or artifacts prove completion.
10. Prepare the final report, slides, demo script, and backup video.

## Change history

See [CHANGELOG.md](CHANGELOG.md) for release notes. Update it whenever application behavior, schema, deployment, or requirements documentation changes.

---

**Maintainer note:** Keep this README as the single high-level project handover: architecture, setup, roles/features, database, security, tests, course compliance, known gaps, and final-review checklist. Supporting files hold the detailed diagrams, migration scripts, and deployment-specific instructions.
