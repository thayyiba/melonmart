CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'BUYER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    stock_qty INT DEFAULT 10,
    category VARCHAR(50) NOT NULL,
    image_url VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS cart_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_product_unique UNIQUE (user_id, product_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

INSERT INTO users (name, email, password, role)
SELECT 'Melon Admin', 'admin@melonmart.com', 'admin123', 'SELLER'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@melonmart.com'
);

INSERT INTO users (name, email, password, role)
SELECT 'Demo Buyer', 'buyer@melonmart.com', 'buyer123', 'BUYER'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'buyer@melonmart.com'
);

INSERT INTO products
    (seller_id, name, description, price, stock_qty, category, image_url)
SELECT
    (SELECT id FROM users WHERE email = 'admin@melonmart.com'),
    'Japanese Organic Matcha Powder',
    'Ceremonial grade 30g tin',
    899.00,
    15,
    'CREATIVE',
    'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=600&q=80'
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE name = 'Japanese Organic Matcha Powder'
);

INSERT INTO products
    (seller_id, name, description, price, stock_qty, category, image_url)
SELECT
    (SELECT id FROM users WHERE email = 'admin@melonmart.com'),
    'Fresh Korean Strawberries',
    'Sweet, aromatic 250g pack',
    450.00,
    20,
    'FRUITS',
    'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=600&q=80'
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE name = 'Fresh Korean Strawberries'
);

INSERT INTO products
    (seller_id, name, description, price, stock_qty, category, image_url)
SELECT
    (SELECT id FROM users WHERE email = 'admin@melonmart.com'),
    'Cold-Pressed Dragonfruit Juice',
    '100% natural, no added sugar 350ml',
    180.00,
    30,
    'JUICES',
    'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=600&q=80'
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE name = 'Cold-Pressed Dragonfruit Juice'
);

INSERT INTO products
    (seller_id, name, description, price, stock_qty, category, image_url)
SELECT
    (SELECT id FROM users WHERE email = 'admin@melonmart.com'),
    'Artisan Truffle Potato Chips',
    'Hand-cooked gourmet crisps 120g',
    220.00,
    25,
    'SNACKS',
    'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=600&q=80'
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE name = 'Artisan Truffle Potato Chips'
);

INSERT INTO products
    (seller_id, name, description, price, stock_qty, category, image_url)
SELECT
    (SELECT id FROM users WHERE email = 'admin@melonmart.com'),
    'Aesthetic Avocado & Toast Kit',
    'Fresh hass avocados & sourdough slice',
    320.00,
    12,
    'RECIPES',
    'https://images.unsplash.com/photo-1525351484163-7529414344d8?w=600&q=80'
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE name = 'Aesthetic Avocado & Toast Kit'
);

INSERT INTO products
    (seller_id, name, description, price, stock_qty, category, image_url)
SELECT
    (SELECT id FROM users WHERE email = 'admin@melonmart.com'),
    'Sparkling Peach Water',
    'Zero calorie infused soda 330ml',
    110.00,
    40,
    'JUICES',
    'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=600&q=80'
WHERE NOT EXISTS (
    SELECT 1 FROM products WHERE name = 'Sparkling Peach Water'
);
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PLACED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);