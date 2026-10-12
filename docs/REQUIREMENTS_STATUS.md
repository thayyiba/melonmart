# Requirements status and evidence

Last reviewed: 2026-10-12. This is a repository review, not a substitute for browser, security, load, or live-host testing. Do not mark a row complete until evidence is linked.

| Area | Evidence already present | Remaining verification / work |
|---|---|---|
| F1 Authentication | Login/register/logout servlet, bcrypt, session invalidation/recreation code | Test role escalation, expiry, cookie flags, all protected APIs and ownership checks |
| F2 Seller catalogue | Product CRUD and seller dashboard | Verify ownership enforcement, stock edit persistence and invalid input handling |
| F3 Browse/search | Product listing/category/search UI and API routes | Smoke-test filtering, empty results, malformed queries and mobile layouts |
| F4 Cart | Cart DAO and buyer cart UI | Verify account isolation, server-side totals and concurrent changes |
| F5 Checkout | OrderService transaction and checkout UI | End-to-end success/failure, duplicate submission, stock rollback and required-field errors |
| F6 Orders | Order history and status routes | Verify buyer/seller ownership and status-transition rules |
| F7 Admin | Admin page and role filter | Test every admin API as buyer/seller; verify status persistence |
| F8 Reviews | Review servlet/DAO and delivered-order qualification logic | Test ownership, rating bounds, duplicates and output escaping |
| O1 Wishlist | Wishlist endpoint and UI synchronization | Verify persistence and account separation after refresh/login/logout |
| O2 Workflow | Product/order workflow UI exists | Run end-to-end seller-to-buyer workflow and record evidence |
| O3 Sales dashboard | Sales summary UI/API exists | Reconcile figures against known test orders |
| O4 Welp chatbot | Provider interface, Gemini implementation, demo fallback, 500-char limit and session rate limit | Add selectable mock provider, provider timeout/caching tests, verify prompt scope and no stack traces |
| API contract | Versioned route aliases and health endpoint exist | Standardize response envelope without breaking clients; add DTOs and consistent status/error codes |
| Architecture | Servlet/DAO/service classes and diagrams exist | Reduce direct DAO use in controllers, document required patterns, audit SOLID and Javadocs |
| Database | H2, HikariCP listener, numbered migrations, schema and seed files | Validate all FK indexes, migration repeatability, concurrent checkout and backup/restore |
| CI/tests | GitHub Actions runs Maven verify and container build | Add broader service/servlet tests and load-test evidence; capture manual acceptance results |
| Security | bcrypt, request ID filter, role checks and chat rate limit are present | Audit every API/ownership boundary, XSS, cookie Secure/SameSite, custom errors and logs |
| Documentation | README, deployment guide, diagrams, contributing and changelog exist | Maintain this status file, fill retrospective and test records, attach report/slides/demo evidence |
| Deployment | Dockerfile, Tomcat 9 image, /data H2 URL and health endpoint configured | Verify Railway SUCCESS, public health, persistence after restart and smoke tests |

## Required final evidence
- [ ] GitHub Actions green on the exact release commit.
- [ ] Manual acceptance checklist completed with browser/device and commit SHA.
- [ ] Public deployment health check verified.
- [ ] Create/read/update/delete flows tested with separate buyer, seller, and admin accounts.
- [ ] Persistent database verified after a service restart.
- [ ] ERD, UML use-case, and checkout sequence reviewed against deployed code.
- [ ] Security checklist reviewed and failures linked to issues.
- [ ] Final report, presentation, demo script, and backup recording attached by the project team.
