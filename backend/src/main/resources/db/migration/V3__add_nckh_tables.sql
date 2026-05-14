-- Cấu trúc bảng NCKH cho SupplyForge AI (Tối ưu snake_case cho Postgres)

CREATE TABLE IF NOT EXISTS orders (
  _id char(24) PRIMARY KEY,
  user_id char(24) NOT NULL,
  order_id varchar(255) NOT NULL,
  platform varchar(50),
  amount decimal(18,2),
  buyer_user_name varchar(255),
  cancelled_after_packaged boolean DEFAULT false,
  data jsonb,
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE IF NOT EXISTS order_items (
  id SERIAL PRIMARY KEY,
  order_id char(24) NOT NULL REFERENCES orders(_id),
  item_id varchar(255),
  model_id varchar(255),
  sku varchar(255),
  product_name text,
  quantity int,
  price decimal(18,2),
  cost decimal(10,2) DEFAULT 0.00
);

CREATE TABLE IF NOT EXISTS products (
  _id char(24) PRIMARY KEY,
  user_id char(24) NOT NULL,
  third_party_id varchar(255) NOT NULL,
  item_id varchar(255),
  product_name varchar(255),
  platform varchar(50),
  item_sku varchar(255),
  price decimal(18,2),
  stock int,
  cost decimal(18,2),
  total_revenue decimal(18,2),
  total_profit decimal(18,2),
  total_orders int,
  data jsonb,
  images jsonb,
  active_on_web boolean DEFAULT true,
  created_at timestamp,
  updated_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_products_user_id ON products(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
