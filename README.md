# MelonMart

Multi-seller grocery marketplace for Anna University R2025 Semester 3.

## Stack
JDK 17 · Tomcat 9 · Servlet 4 (`javax.servlet`) · Maven · H2 · HikariCP · Gson · jBCrypt · vanilla JS.

## Demo account safety

Demo credentials are intentionally not listed in this public README. They are well-known local/demo-only credentials and must never be used on a public deployment. The Docker image disables demo seeding by default; see [deployment security notes](docs/DEPLOYMENT.md#demo-credentials-and-first-administrator). Public deployments should use a unique administrator password configured through the hosting environment. Passwords are stored as bcrypt hashes in the database.

## Run
1. **Protect your existing database first.** Back up `melonmartdb.mv.db` before changing database settings. On Windows, the historical Tomcat database path is preserved. On non-Windows hosts, configure `MELONMART_DB_URL` to a writable path; use persistent storage in deployments so data survives restarts.
2. Set `GEMINI_API_KEY` only if you want live AI. Without it, Welp runs in safe demo mode. Database settings can be overridden with `MELONMART_DB_URL`, `MELONMART_DB_USERNAME`, and `MELONMART_DB_PASSWORD`.
3. Startup applies the numbered scripts in `src/main/resources/db/migrations`, records applied versions in `schema_migrations`, then ensures `schema.sql` and idempotent demo data in `seed.sql` are applied. The SQL scripts are designed for compatible H2 databases; back up real data before upgrades.
4. Build and test: `mvn -B clean verify`.
5. Deploy `target/melonmart.war` to Tomcat 9.
6. Open `/melonmart/`.
7. Health: `/melonmart/api/v1/health`.

## Architecture diagrams
- [D1 — Entity relationship diagram](docs/diagrams/erd.md)
- [D2 — Use-case overview](docs/diagrams/use-cases.md)
- [D3 — Checkout sequence](docs/diagrams/checkout-sequence.md)

## Main flows
Buyer: register/login → browse/search/category → product details → reviews → cart → mock checkout → order history → delivered review.
Seller: login → dashboard → add/edit/delete products → incoming orders → status updates → sales.
Admin: login → users/orders/products/moderation.

## Security points for review
- Prepared statements in DAOs.
- bcrypt password hashing.
- Server-side role assignment/authorization.
- Session invalidation/recreation at login and 30-minute timeout.
- User API responses do not expose password hashes.
- Review creation requires a delivered order containing the product.
- Chat input capped at 500 characters and 10 messages/minute/session.
- `X-Request-ID` returned by API responses and included in the SLF4J MDC logging context.

## Requirements audit
See [the requirements compliance audit](docs/REQUIREMENTS_AUDIT.md) for the source-level baseline, open gaps, and safe verification order. Items marked unchecked still require implementation or test evidence.

## Deployment
- [Deployment guide](docs/DEPLOYMENT.md) — local Tomcat 9, Docker, Render, Railway, database configuration, and persistence notes.
- A multi-stage `Dockerfile` builds the WAR with Java 17 and runs it on Tomcat 9. Attach persistent storage at `/data` when deploying the default H2 file database.
- For Render/Railway, the app is served at `/melonmart/`; the health endpoint is `/melonmart/api/health`.
