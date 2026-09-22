# Changelog

## 1.0.0-review
- Added compliant users/products/orders/order_items/cart/reviews schema.
- Added bcrypt seed accounts and admin role.
- Added legacy database migration for password/order column names.
- Added product rating/review count data.
- Added product details modal with customer reviews and delivered-order review eligibility.
- Added role-specific account menu entries.
- Added `/api/v1/*` aliases and health endpoint.
- Added request IDs and chatbot rate/input limits.
## v3.1.0 — Seller access + review flow fix

- Fixed `/api/auth/me` so role/user fields are available at both the nested and top-level response shapes used by the existing pages.
- Fixed Seller Dashboard authentication so a real `SELLER` account is accepted and can load inventory, incoming orders, and sales.
- Preserved review authorization: only a buyer who owns a delivered order containing the product can submit a review.
- Preserved public product-review loading so other buyers can read reviews for the product.
- No live database reset, seed overwrite, or password/role mutation was added.

