-- Coupons + order discounts.
-- Usage counts are NOT stored: they are counted from orders (cancelled / failed orders do not count),
-- so a cancelled order automatically "gives the coupon use back" with no extra bookkeeping.

CREATE TABLE coupons (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  code               VARCHAR(40)  NOT NULL,
  description        VARCHAR(150) NULL,
  discount_type      VARCHAR(10)  NOT NULL,
  discount_value     BIGINT       NOT NULL,          -- PERCENT: 1-100 | FIXED: amount in cents
  max_discount_cents BIGINT       NULL,              -- optional cap for PERCENT coupons
  min_order_cents    BIGINT       NOT NULL DEFAULT 0,
  usage_limit        INT          NULL,              -- total uses allowed (NULL = unlimited)
  per_user_limit     INT          NOT NULL DEFAULT 1,
  starts_at          DATETIME(3)  NULL,
  expires_at         DATETIME(3)  NULL,
  is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  CONSTRAINT uq_coupons_code UNIQUE (code),
  CONSTRAINT ck_coupons_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
  CONSTRAINT ck_coupons_value CHECK (
    (discount_type = 'PERCENT' AND discount_value BETWEEN 1 AND 100)
    OR (discount_type = 'FIXED' AND discount_value > 0)),
  CONSTRAINT ck_coupons_min CHECK (min_order_cents >= 0),
  CONSTRAINT ck_coupons_limits CHECK (per_user_limit >= 1 AND (usage_limit IS NULL OR usage_limit >= 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE orders
  ADD COLUMN coupon_code    VARCHAR(40) NULL AFTER shipping_cents,
  ADD COLUMN discount_cents BIGINT      NOT NULL DEFAULT 0 AFTER subtotal_cents,
  ADD INDEX idx_orders_coupon (coupon_code);

-- total = subtotal - discount + shipping (the old rule had no discount)
ALTER TABLE orders DROP CHECK ck_orders_total;
ALTER TABLE orders
  ADD CONSTRAINT ck_orders_total CHECK (total_cents = subtotal_cents - discount_cents + shipping_cents),
  ADD CONSTRAINT ck_orders_discount CHECK (discount_cents >= 0 AND discount_cents <= subtotal_cents);
