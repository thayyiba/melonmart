# MelonMart

Multi-seller grocery marketplace for Anna University R2025 Semester 3.

## Stack
JDK 17 · Tomcat 9 · Servlet 4 (`javax.servlet`) · Maven · H2 · HikariCP · Gson · jBCrypt · vanilla JS.

## Demo accounts
- Admin: `admin@melonmart.com` / `admin123`
- Buyer: `buyer@melonmart.com` / `buyer123`
- Seller: `seller@melonmart.com` / `seller123`

Passwords are stored as bcrypt hashes in the database.

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
