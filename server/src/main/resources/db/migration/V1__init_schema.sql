-- Glowvera schema. Money is stored as integer "cents" (BIGINT) to avoid floating point errors.
-- Timestamps are UTC DATETIME(3). Status/role columns are VARCHAR + CHECK (easier to evolve than ENUM).

CREATE TABLE users (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  name          VARCHAR(100) NOT NULL,
  email         VARCHAR(150) NOT NULL,
  phone         VARCHAR(20)  NULL,
  password_hash VARCHAR(100) NOT NULL,
  role          VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  CONSTRAINT uq_users_email UNIQUE (email),
  CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categories (
  id   BIGINT       NOT NULL AUTO_INCREMENT,
  name VARCHAR(80)  NOT NULL,
  slug VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_categories_name UNIQUE (name),
  CONSTRAINT uq_categories_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE skin_types (
  id   BIGINT      NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_skin_types_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE concerns (
  id   BIGINT      NOT NULL AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_concerns_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE products (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  name        VARCHAR(150) NOT NULL,
  slug        VARCHAR(180) NOT NULL,
  brand       VARCHAR(80)  NULL,
  description TEXT         NOT NULL,
  ingredients TEXT         NULL,
  how_to_use  TEXT         NULL,
  image_url   VARCHAR(500) NULL,
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  category_id BIGINT       NOT NULL,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  CONSTRAINT uq_products_slug UNIQUE (slug),
  CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
  INDEX idx_products_category (category_id),
  INDEX idx_products_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_skin_types (
  product_id   BIGINT NOT NULL,
  skin_type_id BIGINT NOT NULL,
  PRIMARY KEY (product_id, skin_type_id),
  CONSTRAINT fk_pst_product   FOREIGN KEY (product_id)   REFERENCES products (id)   ON DELETE CASCADE,
  CONSTRAINT fk_pst_skin_type FOREIGN KEY (skin_type_id) REFERENCES skin_types (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE product_concerns (
  product_id BIGINT NOT NULL,
  concern_id BIGINT NOT NULL,
  PRIMARY KEY (product_id, concern_id),
  CONSTRAINT fk_pc_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
  CONSTRAINT fk_pc_concern FOREIGN KEY (concern_id) REFERENCES concerns (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- stock_qty    = units physically on hand (sum of its batches)
-- reserved_qty = units held by unpaid orders
-- available    = stock_qty - reserved_qty
CREATE TABLE product_variants (
  id                  BIGINT      NOT NULL AUTO_INCREMENT,
  product_id          BIGINT      NOT NULL,
  sku                 VARCHAR(50) NOT NULL,
  name                VARCHAR(80) NOT NULL,
  price_cents         BIGINT      NOT NULL,
  stock_qty           INT         NOT NULL DEFAULT 0,
  reserved_qty        INT         NOT NULL DEFAULT 0,
  low_stock_threshold INT         NOT NULL DEFAULT 5,
  is_active           BOOLEAN     NOT NULL DEFAULT TRUE,
  PRIMARY KEY (id),
  CONSTRAINT uq_variants_sku UNIQUE (sku),
  CONSTRAINT fk_variants_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT ck_variants_price    CHECK (price_cents > 0),
  CONSTRAINT ck_variants_stock    CHECK (stock_qty >= 0),
  CONSTRAINT ck_variants_reserved CHECK (reserved_qty >= 0 AND reserved_qty <= stock_qty),
  INDEX idx_variants_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- A batch of stock with its own expiry date (cosmetics expire). Sold first-expiring-first-out (FEFO).
CREATE TABLE stock_batches (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  variant_id  BIGINT      NOT NULL,
  batch_code  VARCHAR(50) NOT NULL,
  quantity    INT         NOT NULL,
  expiry_date DATE        NOT NULL,
  received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  CONSTRAINT uq_batches_variant_code UNIQUE (variant_id, batch_code),
  CONSTRAINT fk_batches_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
  CONSTRAINT ck_batches_quantity CHECK (quantity >= 0),
  INDEX idx_batches_variant_expiry (variant_id, expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE shipping_rates (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  district  VARCHAR(40) NOT NULL,
  fee_cents BIGINT      NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uq_shipping_district UNIQUE (district),
  CONSTRAINT ck_shipping_fee CHECK (fee_cents >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE orders (
  id                     BIGINT       NOT NULL AUTO_INCREMENT,
  order_code             VARCHAR(30)  NOT NULL,
  user_id                BIGINT       NULL,
  customer_name          VARCHAR(100) NOT NULL,
  customer_email         VARCHAR(150) NOT NULL,
  customer_phone         VARCHAR(20)  NOT NULL,
  address_line           VARCHAR(255) NOT NULL,
  city                   VARCHAR(80)  NOT NULL,
  district               VARCHAR(40)  NOT NULL,
  subtotal_cents         BIGINT       NOT NULL,
  shipping_cents         BIGINT       NOT NULL,
  total_cents            BIGINT       NOT NULL,
  currency               VARCHAR(3)   NOT NULL DEFAULT 'LKR',
  payment_method         VARCHAR(20)  NOT NULL,
  status                 VARCHAR(30)  NOT NULL,
  reservation_expires_at DATETIME(3)  NULL,
  notes                  VARCHAR(500) NULL,
  created_at             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at             DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  CONSTRAINT uq_orders_code UNIQUE (order_code),
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT ck_orders_method CHECK (payment_method IN ('PAYHERE', 'WHATSAPP')),
  CONSTRAINT ck_orders_status CHECK (status IN
    ('PENDING_PAYMENT','PENDING_WHATSAPP','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','FAILED')),
  CONSTRAINT ck_orders_total CHECK (total_cents = subtotal_cents + shipping_cents),
  INDEX idx_orders_status (status),
  INDEX idx_orders_status_expiry (status, reservation_expires_at),
  INDEX idx_orders_user (user_id),
  INDEX idx_orders_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Price and names are SNAPSHOTS: later price changes never rewrite history.
CREATE TABLE order_items (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  order_id         BIGINT       NOT NULL,
  variant_id       BIGINT       NOT NULL,
  product_name     VARCHAR(150) NOT NULL,
  variant_name     VARCHAR(80)  NOT NULL,
  unit_price_cents BIGINT       NOT NULL,
  quantity         INT          NOT NULL,
  line_total_cents BIGINT       NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_items_order   FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
  CONSTRAINT fk_items_variant FOREIGN KEY (variant_id) REFERENCES product_variants (id),
  CONSTRAINT ck_items_quantity CHECK (quantity > 0),
  INDEX idx_items_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Which batch(es) supplied each order item: enables exact restocking and product recalls.
CREATE TABLE order_item_batches (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  order_item_id BIGINT NOT NULL,
  batch_id      BIGINT NOT NULL,
  quantity      INT    NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_oib_item  FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
  CONSTRAINT fk_oib_batch FOREIGN KEY (batch_id)      REFERENCES stock_batches (id),
  CONSTRAINT ck_oib_quantity CHECK (quantity > 0),
  INDEX idx_oib_item (order_item_id),
  INDEX idx_oib_batch (batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
  id                 BIGINT      NOT NULL AUTO_INCREMENT,
  order_id           BIGINT      NOT NULL,
  provider           VARCHAR(20) NOT NULL DEFAULT 'PAYHERE',
  payhere_payment_id VARCHAR(50) NULL,
  status_code        INT         NULL,
  status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  amount_cents       BIGINT      NOT NULL,
  currency           VARCHAR(3)  NOT NULL,
  signature_valid    BOOLEAN     NOT NULL DEFAULT FALSE,
  raw_payload        JSON        NULL,
  created_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  -- the database itself blocks duplicate webhooks (idempotency)
  CONSTRAINT uq_payments_payhere_id UNIQUE (payhere_payment_id),
  CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
  CONSTRAINT ck_payments_status CHECK (status IN ('PENDING','SUCCESS','FAILED','CANCELED','CHARGEDBACK')),
  INDEX idx_payments_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit trail: every status change, who made it and why.
CREATE TABLE order_status_history (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  order_id    BIGINT       NOT NULL,
  from_status VARCHAR(30)  NULL,
  to_status   VARCHAR(30)  NOT NULL,
  changed_by  VARCHAR(100) NOT NULL,
  note        VARCHAR(255) NULL,
  created_at  DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  CONSTRAINT fk_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
  INDEX idx_history_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
