# Security review checklist

This is a review record, not a claim that every item has passed. Check items only after verifying implementation and recording evidence.

## Authentication and sessions
- [ ] Passwords use bcrypt; plaintext passwords are never logged or returned.
- [ ] Session ID changes after authentication; logout invalidates the session.
- [ ] Session cookies use HttpOnly, Secure in HTTPS production, and an appropriate SameSite policy.
- [ ] Authorization is enforced on every protected API/resource, not only HTML pages.
- [ ] Ownership is checked server-side for carts, addresses, wishlists, products, orders, and reviews.

## Input, output, and database
- [ ] SQL values use PreparedStatement parameters.
- [ ] User-controlled output is contextually escaped; unsafe HTML is not inserted into the DOM.
- [ ] Request bodies, string lengths, numeric ranges, enum values, and IDs are validated server-side.
- [ ] Errors return safe messages; stack traces and internal SQL are not sent to clients.
- [ ] Transactions roll back on checkout failure and stock cannot be oversold concurrently.
- [ ] Foreign-key columns have supporting indexes; currency uses DECIMAL.

## Configuration and deployment
- [ ] No credentials or real user data are committed to Git.
- [ ] Public deployment disables demo account seeding.
- [ ] Production admin is provisioned using a strong environment-only password.
- [ ] Database files live on persistent storage and are excluded from Git.
- [ ] HTTPS cookie settings and proxy behavior are verified in the deployed environment.
- [ ] Health check verifies database connectivity.
- [ ] Backup and restore procedure is documented and tested.

## Observability and AI
- [ ] Request ID is included in response headers and logging context.
- [ ] Logs exclude secrets and personal delivery data.
- [ ] Chat length and per-session rate limits are enforced.
- [ ] Provider timeout, error, and fallback behavior are tested.
- [ ] Chatbot scope is constrained to marketplace help and grounded product information.
