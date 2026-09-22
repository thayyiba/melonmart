-- MelonMart R2025 review-ready schema (H2 / MySQL compatibility)
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'BUYER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_role CHECK (role IN ('BUYER','SELLER','ADMIN'))
);

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    price DECIMAL(10,2) NOT NULL,
    stock_qty INT NOT NULL DEFAULT 0,
    category VARCHAR(80) NOT NULL,
    image_url VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_products_price CHECK (price >= 0),
    CONSTRAINT chk_products_stock CHECK (stock_qty >= 0),
    CONSTRAINT fk_products_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    buyer_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_orders_status CHECK (status IN ('PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED')),
    CONSTRAINT chk_orders_total CHECK (total_amount >= 0),
    CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS order_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price >= 0),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS cart_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_cart_quantity CHECK (quantity > 0),
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    user_id INT NOT NULL,
    order_id INT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uq_review_user_product_order UNIQUE (user_id, product_id, order_id),
    CONSTRAINT fk_reviews_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_products_seller ON products(seller_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_orders_buyer ON orders(buyer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_cart_user ON cart_items(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_product ON cart_items(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_product ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user ON reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_order ON reviews(order_id);

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


CREATE TABLE IF NOT EXISTS wishlist (
 id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, product_id INT NOT NULL,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT wishlist_user_product_unique UNIQUE (user_id, product_id),
 FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
 FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_wishlist_user ON wishlist(user_id);
