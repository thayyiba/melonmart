-- Safe public catalog seed: no demo users, passwords, orders, or reviews.
-- seller_id is NULL so these sample products do not belong to a fake seller account.
INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Fresh Korean Strawberries', 'Sweet aromatic 250g pack', 450.00, 20, 'FRUITS', 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Fresh Korean Strawberries');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Alphonso Mango Box', 'Juicy seasonal mangoes, 3-piece box', 299.00, 24, 'FRUITS', 'https://images.unsplash.com/photo-1553279768-865429fa0078?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Alphonso Mango Box');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Avocado & Toast Kit', 'Fresh avocados with a breakfast-ready recipe card', 320.00, 12, 'GROCERIES', 'https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Avocado & Toast Kit');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Artisan Truffle Potato Chips', 'Hand-cooked gourmet crisps, 120g', 220.00, 25, 'SNACKS', 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Artisan Truffle Potato Chips');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Chocolate Chip Cookies', 'Soft-baked cookie bites for snack breaks', 160.00, 30, 'SNACKS', 'https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Chocolate Chip Cookies');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Cold-Pressed Dragonfruit Juice', 'Fruit-forward juice, 350ml', 180.00, 30, 'DRINKS', 'https://images.unsplash.com/photo-1525385133512-2f3bdd039054?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Cold-Pressed Dragonfruit Juice');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Sparkling Peach Water', 'Peach-flavoured sparkling water, 330ml', 110.00, 40, 'DRINKS', 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Sparkling Peach Water');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Everyday Gold-Tone Hoops', 'Lightweight hoops for everyday outfits', 249.00, 18, 'JEWELLERY', 'https://images.unsplash.com/photo-1611652022419-a9419f74343d?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Everyday Gold-Tone Hoops');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Pearl Detail Bracelet', 'A delicate little sparkle for your wrist', 299.00, 15, 'JEWELLERY', 'https://images.unsplash.com/photo-1611591437281-460bfbe1220a?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Pearl Detail Bracelet');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Pastel Sticker Set', 'Cute stickers for notebooks and laptop covers', 99.00, 50, 'CREATIVE', 'https://images.unsplash.com/photo-1513364776144-60967b0f800f?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Pastel Sticker Set');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Pocket Journal', 'A small notebook for big and tiny thoughts', 149.00, 35, 'CREATIVE', 'https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Pocket Journal');

INSERT INTO products (seller_id, name, description, price, stock_qty, category, image_url)
SELECT NULL, 'Crisp Apple Juice', 'A refreshing fruit drink, 250ml', 85.00, 28, 'DRINKS', 'https://images.unsplash.com/photo-1600271886742-f049cd451bba?w=800&q=80'
WHERE NOT EXISTS (SELECT 1 FROM products WHERE name='Crisp Apple Juice');
