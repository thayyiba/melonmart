# Requirements compliance audit — MelonMart

**Purpose:** Compare the YourNameMart course specification against the existing MelonMart repository without renaming the project or replacing its UI.
**Audit basis:** Source and configuration files inspected on `fix/melonmart-account-checkout-inventory` on 2026-10-13.
**Important:** This is a source-level baseline, not a claim that every requirement has passed a live end-to-end test. Re-run the build and deployed smoke tests before marking items complete.

## Repository identity and stack

- [x] Preserve the project identity: MelonMart, `com.melon.melonmart`, and `target/melonmart.war`.
- [x] Java 17, Maven WAR packaging, Servlet 4 / `javax.servlet`, H2, HikariCP, jBCrypt, Gson, SLF4J/Logback are configured.
- [x] GitHub Actions runs `mvn -B clean verify` and builds the Tomcat container.
- [x] Root README, deployment guide, `.env.example`, CONTRIBUTING, CHANGELOG, and three diagram documents exist.

## Feature baseline

| Requirement | Current source evidence | Status |
|---|---|---|
| F1 Authentication and roles | `AuthServlet`, `UserDAO`, bcrypt checks, role assignment, seeded admin | Implemented; retest deployed registration/login and session handling |
| F2 Seller listing management | Product endpoints and seller dashboard code | Implemented; retest create/edit/delete and seller ownership boundaries |
| F3 Browse/search/filter | Product endpoints and catalog UI | Implemented; retest keyword/category combinations |
| F4 Cart | `CartDAO` and cart endpoint | Implemented; retest quantity validation and totals |
| F5 Checkout/mock payment | `OrderServlet`, `OrderDAO`, delivery/payment fields | Implemented; retest stock, invalid details, duplicate submission, and order persistence |
| F6 Buyer/seller order history | Order DAO/endpoints and seller dashboard | Implemented; retest authorization and status visibility |
| F7 Admin management | Admin endpoints and admin dashboard | Implemented; retest forbidden access for non-admin roles |
| F8 Reviews/ratings | Review endpoint and delivered-order eligibility checks | Implemented; retest duplicate/ineligible reviews |

Optional wishlist, order status workflow, seller sales dashboard, and Welp chatbot code are present. Verify each on the deployed app before claiming final-review readiness.

## Engineering and security checks

- [x] Password hashing uses bcrypt; user response construction omits password hashes.
- [x] SQL migrations use numbered filenames and startup records applied migration versions.
- [x] Currency columns use `DECIMAL(10,2)` in the checked-in schema.
- [x] Login invalidates the prior session and sets a 30-minute timeout.
- [x] Request-ID filter and chatbot input/rate limits exist.
- [ ] **Layering gap to address:** at least `AuthServlet` instantiates `UserDAO` directly. The specification asks controllers to delegate business rules through service classes and services to DAO interfaces. Do not mark strict layered MVC/SOLID compliance complete until the remaining direct DAO calls are reviewed and refactored safely.
- [ ] **Testing gap:** existing JUnit tests cover selected DAO, migration, checkout, stock, status, and password-hash behaviors. Add automated servlet/auth/filter tests and a documented end-to-end register → browse → order → review run.
- [ ] **Database audit:** verify that every foreign key has an index on the deployed schema, including legacy databases.
- [ ] **Security regression:** manually test SQL injection, stored/reflected XSS, direct API authorization bypass, session handling, and generic error responses against a disposable/test database.
- [ ] **Load test:** no checked-in evidence yet of the required 10 concurrent users for 60 seconds. Run and record a JMeter or `ab` test.
- [ ] **Documentation evidence:** ensure final report/demo recording and a tested live URL are available for assessment; repository documentation alone does not prove those deliverables were completed.

## Seller ownership recovery — operational note

The one-time startup repair assigns only rows with `products.seller_id IS NULL` to the existing `seller@melonmart.com` account when that account has role `SELLER`. It deliberately does not create an account or alter products owned by other sellers. The latest observed Railway startup said the account was absent, so the repair did **not** run. Create/register the seller account first, then restart/redeploy the application so the pending one-time repair can execute. Do not reset or delete the persistent database.

## Recommended execution order

1. Establish seller access using a strong unique password; confirm the account exists in the production database.
2. Restart the app and verify the startup log confirms the ownership repair; check seller inventory and public catalog.
3. Run `mvn -B clean verify` and inspect GitHub Actions results.
4. Add missing automated auth/servlet tests and run the manual security checklist.
5. Confirm FK indexes and run the required load test against a non-production dataset.
6. Only then update this audit's unchecked items with the evidence, and prepare the final report/demo materials.
