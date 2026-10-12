-- Demo accounts. Passwords are bcrypt hashes, never plaintext.
INSERT INTO users (name, email, password_hash, role)
SELECT 'Melon Admin', 'admin@melonmart.com', '$2a$12$VjfToSg23r0oelNBg5LAiO3E1LpKt.B4QpMUPKmywnCH4aicRwzvi', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@melonmart.com');

INSERT INTO users (name, email, password_hash, role)
SELECT 'Demo Buyer', 'buyer@melonmart.com', '$2a$12$69iKRAa6sGyXRjUpr.CYsOTGOiEiwNhw3yHTKgGy79NGfIV8fPVk2', 'BUYER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'buyer@melonmart.com');

INSERT INTO users (name, email, password_hash, role)
SELECT 'Demo Seller', 'seller@melonmart.com', '$2a$12$QHSGjoJCG8XAbfitQX08nOjE4E/fjvZ6/Q4ZPaacs5xpFByenKw8S', 'SELLER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'seller@melonmart.com');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email='seller@melonmart.com'), 'Fresh Korean Strawberries', 'Sweet aromatic 250g pack', 450.00, 20, 'FRUITS', 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Fresh Korean Strawberries');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email='seller@melonmart.com'), 'Cold-Pressed Dragonfruit Juice', '100% natural, no added sugar, 350ml', 180.00, 30, 'JUICES', 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Cold-Pressed Dragonfruit Juice');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email='seller@melonmart.com'), 'Artisan Truffle Potato Chips', 'Hand-cooked gourmet crisps, 120g', 220.00, 25, 'SNACKS', 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Artisan Truffle Potato Chips');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email='seller@melonmart.com'), 'Aesthetic Avocado & Toast Kit', 'Fresh hass avocados and sourdough', 320.00, 12, 'BREAKFAST', 'https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Aesthetic Avocado & Toast Kit');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT (SELECT id FROM users WHERE email='seller@melonmart.com'), 'Sparkling Peach Water', 'Zero calorie infused sparkling water, 330ml', 110.00, 40, 'DRINKS', 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Sparkling Peach Water');

-- Small demo order/review so the review UI is visible during a presentation.
INSERT INTO orders (buyer_id, status, total_amount)
SELECT (SELECT id FROM users WHERE email='buyer@melonmart.com'), 'DELIVERED', 450.00
WHERE NOT EXISTS (
    SELECT 1 FROM orders o JOIN users u ON u.id=o.buyer_id
    WHERE u.email='buyer@melonmart.com' AND o.total_amount=450.00 AND o.status='DELIVERED'
);

INSERT INTO order_items (order_id, product_id, quantity, unit_price)
SELECT
    (SELECT MAX(o.id) FROM orders o JOIN users u ON u.id=o.buyer_id WHERE u.email='buyer@melonmart.com' AND o.status='DELIVERED' AND o.total_amount=450.00),
    (SELECT id FROM products WHERE name='Fresh Korean Strawberries'), 1, 450.00
WHERE NOT EXISTS (
    SELECT 1 FROM order_items oi JOIN orders o ON o.id=oi.order_id JOIN users u ON u.id=o.buyer_id
    WHERE u.email='buyer@melonmart.com' AND oi.product_id=(SELECT id FROM products WHERE name='Fresh Korean Strawberries')
);

INSERT INTO reviews (product_id, user_id, order_id, rating, comment)
SELECT
    (SELECT id FROM products WHERE name='Fresh Korean Strawberries'),
    (SELECT id FROM users WHERE email='buyer@melonmart.com'),
    (SELECT MAX(o.id) FROM orders o JOIN users u ON u.id=o.buyer_id WHERE u.email='buyer@melonmart.com' AND o.status='DELIVERED' AND o.total_amount=450.00),
    5, 'Fresh and sweet. Arrived in great condition!'
WHERE NOT EXISTS (
    SELECT 1 FROM reviews r JOIN users u ON u.id=r.user_id WHERE u.email='buyer@melonmart.com' AND r.product_id=(SELECT id FROM products WHERE name='Fresh Korean Strawberries')
);
