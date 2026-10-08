# GlowVera: Cosmetics & Beauty E-Commerce Platform

A full-stack online store for a cosmetics retailer: a responsive **customer storefront**, an **Admin Panel** (orders, products, inventory, sales analytics, coupons), **PayHere Sandbox** online payment and an **Order via WhatsApp** checkout.

## Links

| | |
|---|---|
| **Live application** | https://glowvera-seven.vercel.app |
| **API (backend)** | https://glowvera-scvd.onrender.com |
| **GitHub repository** | https://github.com/abinash1417/glowvera |
| **Admin login (demo)** | Email: `ADMIN_EMAIL_HERE`  /  Password: `ADMIN_PASSWORD_HERE` |
| **Customer login** | Register any account on the site (checkout needs a customer account) |
| **PayHere sandbox test card** | Visa `4916217501611292`, any future expiry, any CVV |

> **Note:** the API runs on a free host that sleeps when idle. The first request after a quiet period can take about a minute.

## Technologies

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite 8, React Router 7, Axios, Tailwind CSS 4 |
| Backend | Java 21, Spring Boot 3.5 (Web, Data JPA / Hibernate, Security, Validation) |
| Database | MySQL 8, schema versioned with Flyway migrations |
| Authentication | JWT (HS256, 1 hour) in an httpOnly cookie, BCrypt password hashing |
| Payments | PayHere Sandbox (signed checkout form and verified server-to-server webhook) |
| API docs | SpringDoc OpenAPI / Swagger UI (`/swagger-ui.html`, disabled in production) |
| Testing | JUnit 5, Mockito (59 automated tests) |
| Hosting | Vercel (frontend), Render (backend, Docker), Aiven (managed MySQL) |

## Features

**Customer store:** product browsing with search and filters (category, skin type, concern, price, sort), product details with variants, cart, checkout, coupon codes, order tracking and order history.

**Checkout:** PayHere Sandbox online payment, or send the complete cart to the business WhatsApp number as a clear, readable message.

**Admin panel:** order management and status changes, product and variant management, inventory with expiry batches and low-stock alerts, **sales analytics** (revenue, orders, top products, payment split, coupon use) and **coupon management**.

## Setup (run locally)

**Prerequisites:** JDK 21, Maven 3.9+, Node 20+, MySQL 8.

```bash
# 1) Backend
cd server
cp .env.example .env     # then fill in the values (see the table below)
mvn spring-boot:run      # runs Flyway migrations, creates the admin, seeds demo products
                         # API: http://localhost:5000   Swagger: http://localhost:5000/swagger-ui.html

# 2) Frontend (new terminal)
cd client
npm install
npm run dev              # http://localhost:5173  (Vite proxies /api to the backend)
```

**Main environment variables (`server/.env`, never committed):**

| Variable | Purpose |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection (JDBC URL) |
| `JWT_SECRET` | Token signing key, at least 32 characters |
| `JWT_EXPIRES_MINUTES` | Login lifetime, default 60 |
| `ADMIN_NAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Admin account created at first start (password 12+ characters) |
| `PAYHERE_MERCHANT_ID`, `PAYHERE_MERCHANT_SECRET`, `PAYHERE_SANDBOX` | PayHere credentials |
| `WHATSAPP_NUMBER` | Business number, digits only (for example `94771234567`) |
| `CLIENT_URL`, `PUBLIC_API_URL` | Frontend and backend public addresses (CORS, PayHere return and notify URLs) |
| `SEED_DEMO_DATA` | `true` adds demo products on first start |

**PayHere locally:** PayHere must reach the notify address, so expose the backend (for example `ngrok http 5000`) and set `PUBLIC_API_URL` to that https address.

**Tests:** `cd server && mvn test`

## Architecture

```
React (Vite)  --/api (JSON + httpOnly cookie)-->  Spring Boot
                                                    |
   web         controllers, global error handler    |  thin: validate, delegate
   service     use cases and transactions           |  business flow
   domain      pure Java rules (no Spring)          |  order state machine, cart rules,
                                                    |  stock math, coupon math, money
   checkout    Strategy: PayHere / WhatsApp         |
   repository  Spring Data + Specifications         |  queries, row locks
   entity      JPA model                            |
                                                    v
                                                 MySQL 8
```

| Pattern | Where | Why |
|---|---|---|
| Strategy | `CheckoutStrategy` with `PayHereCheckoutStrategy`, `WhatsAppCheckoutStrategy` | A new payment method is one new class; no `if (method == ...)` in services |
| State machine | `OrderStateMachine` (pure) | The whole order lifecycle in one table; every change is validated |
| Specification | `ProductSpecifications`, `OrderSpecifications` | Composable search and filters |
| DTO + Mapper | `dto`, `mapper` | Entities never leak to the API |
| Single responsibility | `OrderPlacementService`, `OrderTransitionService`, `OrderExpiryService`, `StockService`, `CouponService`, `AnalyticsService` | One reason to change per class |

## Database design

```
users --< orders >-- order_items >-- product_variants >-- products >-- categories
              |           |                  |             +-- product_skin_types, product_concerns
              |           +-- order_item_batches >-- stock_batches
              +-- payments
              +-- order_status_history          shipping_rates (per district)    coupons
```

- **Money is stored as integer cents** (`BIGINT`), so there are no floating-point errors.
- **Variants** (size or shade) carry price and stock. **Batches** carry an expiry date because cosmetics expire. Stock is sold **FEFO** (first expiring, first out) and expired stock is never sold.
- `order_items` keep **snapshots** of name and price, so later price changes never rewrite history. Orders also snapshot the coupon code and discount used.
- `order_item_batches` records which batch supplied each unit, so a cancellation restocks the same batch and a recall can be traced.
- `payments.payhere_payment_id` is **unique**, so the database itself makes the payment webhook idempotent.
- `CHECK` constraints: `reserved_qty <= stock_qty`, no negative stock, `total = subtotal - discount + shipping`.
- `order_status_history` is a full audit trail.

**Inventory:** `available = stock - reserved`. Checkout reserves stock (30 minutes for PayHere, 24 hours for WhatsApp). Payment or confirmation commits it. Cancellation, failure or expiry releases it. A scheduled job releases abandoned reservations every minute.

## Order lifecycle

```
PENDING_PAYMENT --> PAID --> PROCESSING --> SHIPPED --> DELIVERED
     |      +--> FAILED          |
     +--> CANCELLED <-----------+   (PAID or PROCESSING can be cancelled and the stock is restocked)
PENDING_WHATSAPP --> PAID | PROCESSING | CANCELLED
```

Only `OrderTransitionService` changes a status, whether the trigger is the PayHere webhook, an admin action or the expiry job.

## Checkout flows

**PayHere:** the server creates the order and reserves stock, then returns a signed form (`hash = MD5(merchant_id + order_id + amount + currency + MD5(secret))`). The browser posts it to PayHere. PayHere then calls `POST /api/payments/payhere/notify`, and the server (1) verifies the MD5 signature with a constant-time comparison, (2) ignores duplicate notifications, (3) checks the amount and currency equal the order total, and only then (4) marks the order `PAID`. Card details are never stored.

**WhatsApp:** the order is saved first, then the customer gets a `wa.me` link with a readable message. Variants of the same product are grouped on one line, and long carts fall back to a compact message to stay within URL limits:

```
*New order GLW-2026-00007*
*Customer*  Nimal Perera, 0771234567, 12 Galle Road, Moratuwa, Colombo
*Items*
- Hydra Glow Gel Moisturizer: 50ml x 2, 100ml x 1 (Rs. 6,900.00)
- Vitamin C Brightening Serum: 15ml x 1 (Rs. 2,900.00)
Subtotal: Rs. 9,800.00   Delivery (Colombo): Rs. 350.00   *Total: Rs. 10,150.00*
```

## Coupons and analytics

- **Coupons:** percentage or fixed amount, optional cap, minimum order, total usage limit, uses per customer, start and expiry time. The discount comes off the subtotal only and never exceeds it. The same `CouponService` rules run for the cart preview and the real checkout, the browser sends only a code, and the coupon row is locked during checkout so the last use cannot be taken twice. Usage is counted from orders, so a cancelled or failed order gives the use back.
- **Analytics** (`GET /api/admin/analytics?days=7|30|90`): revenue, orders, average order value, discounts, change against the previous period, revenue per day, top products, orders by status, payment split and coupons used. Revenue counts paid, processing, shipped and delivered orders. Days follow Sri Lanka time (`Asia/Colombo`).

## Important technical decisions

| Decision | Why |
|---|---|
| **Separate React frontend and Spring Boot API** | Clear boundary between UI and business rules; the API can be tested and documented on its own (Swagger). |
| **Prices, totals and discounts are calculated on the server** | The browser is never trusted for money. A tampered cart or coupon amount has no effect. |
| **Money stored as integer cents** | Avoids floating-point rounding errors in totals, discounts and payment amounts. |
| **Stock is reserved at checkout, committed on payment, released on cancel or expiry** | Two customers cannot buy the last unit, and abandoned carts do not block stock forever. |
| **Row locks in a single transaction (`SELECT ... FOR UPDATE`, ascending id order)** | Prevents overselling and deadlocks under concurrent checkouts. |
| **Batches with expiry dates and FEFO selling** | Cosmetics expire, so the earliest-expiring stock is sold first and expired stock is never sold. |
| **PayHere webhook is the source of truth for payment** | Returning to the success page does not mark an order paid; only a verified, non-duplicate notification with the right amount does. |
| **Order is saved before the WhatsApp message is opened** | The shop keeps a record even if the customer never sends the message; the admin confirms it in the panel. |
| **Strategy pattern for checkout methods** | A new payment method is one new class, with no changes to the order service. |
| **JWT in an httpOnly cookie, not localStorage** | JavaScript (and so XSS) cannot read the token. The frontend host forwards `/api` to the backend so the cookie is first-party. |
| **Short 1 hour session, role re-checked on every request** | Limits the damage from a stolen token and applies role changes immediately. |
| **Coupon usage counted from orders, with the coupon row locked at checkout** | Cancelled orders give the use back automatically, and the last use cannot be taken twice. |
| **Flyway migrations** | The database schema is versioned and reproducible on every environment. |

## Security approach

- **Passwords:** BCrypt (cost 12). Login uses one generic error and a dummy hash comparison, so timing does not reveal which emails exist.
- **Sessions:** JWT in an **httpOnly, SameSite** cookie (`Secure` in production) with a **1 hour** lifetime. The user and role are re-read from the database on every request, so a removed or demoted user loses access at once.
- **Authorization:** one rule table in `SecurityConfig`. `/api/admin/**` requires `ROLE_ADMIN`. Customers can read only their own orders. Public registration always creates a `CUSTOMER`; the admin is created only from environment variables.
- **CSRF:** SameSite cookie, `OriginCheckFilter` for state-changing requests, and JSON-only bodies.
- **Validation:** Bean Validation on every request. **Prices and discounts are always calculated on the server**, never taken from the browser.
- **Rate limiting:** 5 failed logins per 15 minutes per IP, plus limits on registration, checkout and order tracking.
- **Secrets:** all configuration comes from environment variables bound to a validated `AppProperties` (the app refuses to start with a short `JWT_SECRET`). `.env` is git-ignored and only `.env.example` is committed. The PayHere secret never reaches the browser.
- **Payments:** signature verification, idempotent webhook, amount and currency check. No card data is stored.
- **Errors:** one global handler, so stack traces and SQL never reach the client.

## Concurrency (no overselling)

Checkout runs in one transaction that locks the variant rows (`SELECT ... FOR UPDATE`, in ascending id order so deadlocks cannot happen), re-reads fresh stock, checks every line and reserves it. A second buyer of the last unit waits, then gets a clear "no longer available" message. The transaction uses `READ_COMMITTED` so the re-read sees committed data.

## Deployment

- **Frontend:** Vercel, root directory `client`. `client/vercel.json` forwards `/api/*` to the backend, so the login cookie stays first-party.
- **Backend:** Render (Docker, `server/Dockerfile`) with environment variables and `APP_ENV=production`.
- **Database:** Aiven managed MySQL 8, connected over SSL.

## Assumptions and limitations

- Currency is LKR. Shipping is a flat fee per district.
- Checkout requires a customer account. Guest checkout is possible in the schema (`orders.user_id` is nullable) but not exposed.
- **PayHere Sandbox** does not accept a subdomain such as `glowvera-seven.vercel.app`, so the parent domain `vercel.app` was registered in the sandbox account.
- The backend and database run on free plans: the API sleeps when idle, and the free database can be switched off after a long period without use.
- Rate limiting is in memory, which is correct for one instance. Several instances would need Redis.
- The public product list sorts by price in memory, which is fine for a small catalog.
- There are no email notifications. Refunds are not automated: cancellations release stock, and refunds and chargebacks are handled manually in the PayHere portal.
- Coupons need a logged-in customer, and coupon guessing is limited only by login.
- Analytics are aggregated in the application and dated by the time the order was placed. A large shop would use SQL grouping or a summary table.
- Product images are files in `client/public/images`.