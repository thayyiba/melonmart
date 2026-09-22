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
1. **Keep your existing `data/melonmartdb.mv.db`**. The startup migration upgrades compatible legacy columns in place; do not replace it with another project database.
2. Set `GEMINI_API_KEY` only if you want live AI. Without it, Welp runs in safe demo mode.
3. Build: `mvn clean verify`.
4. Deploy `target/melonmart.war` to Tomcat 9.
5. Open `/melonmart/`.
6. Health: `/melonmart/api/v1/health`.

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
- `X-Request-ID` added to API responses.
