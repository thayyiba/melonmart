# Requirements compliance audit — MelonMart

**Purpose:** Compare the YourNameMart course specification against the existing MelonMart repository without renaming the project or replacing its UI.
**Audit basis:** Source and configuration files on `main`, inspected 2026-10-13.
**Important:** This is a source-level baseline, not proof that every requirement passed a live end-to-end test. Re-run the build and deployed smoke tests before marking items complete.

## Repository identity and stack

- [x] Preserve project identity: MelonMart, `com.melon.melonmart`, and `target/melonmart.war`.
- [x] Java 17, Maven WAR packaging, Servlet 4 / `javax.servlet`, H2, HikariCP, jBCrypt, Gson, SLF4J/Logback are configured.
- [x] GitHub Actions runs `mvn -B clean verify` and builds the Tomcat container.
- [x] Root README, deployment guide, `.env.example`, CONTRIBUTING, CHANGELOG, and three diagram documents exist.

## Feature baseline

| Requirement | Source evidence | Status |
|---|---|---|
| F1 Authentication and roles | Auth endpoints, user DAO, bcrypt checks, seeded admin | Implemented; retest deployed registration/login/session handling |
| F2 Seller listing management | Product endpoints and seller dashboard code | Implemented; retest create/edit/delete and ownership boundaries |
| F3 Browse/search/filter | Product endpoints and catalog UI | Implemented; retest keyword/category combinations |
| F4 Cart | Cart DAO and cart endpoint | Implemented; retest quantity validation and totals |
| F5 Checkout/mock payment | Order endpoint/DAO and delivery/payment fields | Implemented; retest invalid details, stock, duplicate submission, persistence |
| F6 Buyer/seller order history | Order DAO/endpoints and seller dashboard | Implemented; retest authorization and status visibility |
| F7 Admin management | Admin endpoints and admin dashboard | Implemented; retest forbidden access for non-admin roles |
| F8 Reviews/ratings | Review endpoint and delivered-order eligibility checks | Implemented; retest duplicate/ineligible reviews |

Optional wishlist, order status workflow, seller sales dashboard, and Welp chatbot code are present. Verify each against the deployed app before claiming final-review readiness.

## Engineering and security checks

- [x] Password hashing uses bcrypt; user response construction omits password hashes.
- [x] SQL migrations use numbered filenames and startup records migration versions.
- [x] Currency columns use `DECIMAL(10,2)` in the checked-in schema.
- [x] Login invalidates the prior session and sets a 30-minute timeout.
- [x] Request-ID filter and chatbot input/rate limits exist.
- [ ] **Layering gap:** at least `AuthServlet` instantiates `UserDAO` directly. The specification requires controllers to delegate business rules through service classes and services to DAO interfaces. Review and refactor direct DAO calls safely before claiming strict layered MVC/SOLID compliance.
- [ ] **Testing gap:** existing JUnit tests cover selected DAO, migration, checkout, stock, status, and password-hash behaviors. Add automated servlet/auth/filter tests and document a complete register → browse → order → review run.
- [ ] **Database audit:** verify every foreign key has an index, including on any upgraded/legacy database.
- [ ] **Security regression:** manually test SQL injection, stored/reflected XSS, direct API authorization bypass, session handling, and generic error responses against a disposable/test database.
- [ ] **Load test:** record the required 10 concurrent users for 60 seconds using JMeter or `ab`.
- [ ] **Final evidence:** ensure final report, demo recording, and live URL smoke-test evidence are available; repository documentation alone does not prove those deliverables were completed.

## Recommended execution order

1. Run `mvn -B clean verify` and inspect GitHub Actions.
2. Add missing automated auth/servlet tests and run the manual security checklist.
3. Verify foreign-key indexes and run the required load test against a non-production dataset.
4. Smoke-test all F1–F8 flows against the deployed URL.
5. Update unchecked items only when supporting test evidence exists, then prepare the final report/demo materials.
