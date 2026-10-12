# Manual acceptance and smoke-test checklist

Run this against a fresh test database or disposable preview. Never use real customer data. Record date, commit SHA, browser/device, tester, and evidence for every run. Mark a test PASS only after observing the result.

## Setup and health
- [ ] Fresh clone builds with `mvn -B clean verify`.
- [ ] Application starts on Java 17 and Tomcat 9.
- [ ] GET `/melonmart/api/v1/health` returns HTTP 200 and JSON indicating app and DB are UP.
- [ ] Restart service and verify persisted data remains present on the configured persistent volume.

## Authentication and isolation
- [ ] Register a buyer; duplicate email is rejected.
- [ ] Valid login works; invalid credentials are rejected without disclosing whether an email exists.
- [ ] Session identifier changes after login; logout invalidates the session.
- [ ] Buyer cannot access seller/admin pages or APIs; seller cannot access admin operations.
- [ ] User A's addresses, cart, and wishlist are not visible to User B.
- [ ] Delete the final saved address, refresh, and verify it stays deleted.

## Buyer journey
- [ ] Homepage product shelves populate; search/category filters work.
- [ ] Product detail and reviews load; invalid review submissions are rejected.
- [ ] Add/remove/update cart items; totals use server-side prices.
- [ ] Checkout rejects missing/invalid contact, delivery, or payment fields with actionable messages.
- [ ] Successful checkout creates one order and order items, reserves stock, and clears only that buyer's cart.
- [ ] Repeated checkout submission does not create duplicate orders.
- [ ] Order history displays only the signed-in buyer's orders.
- [ ] Wishlist add/remove persists after refresh and login/logout.

## Seller and admin
- [ ] Seller can change only their own products.
- [ ] Stock persists after editing; low-stock count includes stock greater than zero and at most five.
- [ ] Seller sees only orders containing their products and permitted status changes work.
- [ ] Admin can manage permitted users/products/orders.
- [ ] Admin order-status dropdown persists valid changes and reports API errors.

## Welp chatbot
- [ ] Without a provider key, fallback response works.
- [ ] Product claims are grounded in available product context.
- [ ] Empty and over-500-character messages are rejected.
- [ ] The 11th request within a minute for one session is rate-limited.
- [ ] Provider failure returns a safe fallback and does not expose credentials or stack traces.

## Responsive and security
- [ ] Test buyer, seller, and admin flows at mobile, tablet, and desktop widths.
- [ ] No horizontal overflow, clipped controls, unreadable text, or unusable dropdowns.
- [ ] HTML/script-like input is rendered as text, not executed.
- [ ] Missing sessions, wrong roles, invalid IDs, and another user's resource IDs are rejected.
- [ ] Logs contain request IDs and no passwords, cookies, API keys, or delivery/contact data.

## Results record
- Date/time and timezone:
- Commit SHA:
- Environment / URL:
- Tester:
- Browser/device and viewport:
- Passed tests:
- Failed tests and issue links:
- Not run (with reason):
- Sanitized evidence links:
