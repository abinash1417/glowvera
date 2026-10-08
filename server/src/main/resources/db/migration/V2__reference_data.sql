-- Reference data every environment needs (not demo data).
INSERT INTO skin_types (name) VALUES ('Oily'), ('Dry'), ('Combination'), ('Sensitive'), ('Normal');

INSERT INTO concerns (name) VALUES
  ('Acne'), ('Anti-aging'), ('Hydration'), ('Brightening'),
  ('Dark Spots'), ('Sun Protection'), ('Hair Fall'), ('Dandruff');

INSERT INTO categories (name, slug) VALUES
  ('Skincare', 'skincare'), ('Hair Care', 'hair-care'), ('Body Care', 'body-care'), ('Makeup', 'makeup');

-- Delivery fees per district, in cents (Rs. 350 = 35000). Adjust to your own assumptions.
INSERT INTO shipping_rates (district, fee_cents) VALUES
  ('Colombo', 35000),
  ('Gampaha', 35000),
  ('Kalutara', 35000),
  ('Kandy', 45000),
  ('Matale', 45000),
  ('Nuwara Eliya', 45000),
  ('Galle', 45000),
  ('Matara', 45000),
  ('Hambantota', 45000),
  ('Kurunegala', 45000),
  ('Puttalam', 45000),
  ('Anuradhapura', 45000),
  ('Polonnaruwa', 45000),
  ('Badulla', 45000),
  ('Monaragala', 45000),
  ('Ratnapura', 45000),
  ('Kegalle', 45000),
  ('Jaffna', 60000),
  ('Kilinochchi', 60000),
  ('Mannar', 60000),
  ('Mullaitivu', 60000),
  ('Vavuniya', 60000),
  ('Trincomalee', 60000),
  ('Batticaloa', 60000),
  ('Ampara', 60000);
