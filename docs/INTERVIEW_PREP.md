# Interview prep: decisions you should be able to explain

Practise saying each answer out loud in 30–60 seconds.

## "Why this architecture?"
Layered: controllers stay thin, services hold use-cases and transactions, `domain` holds pure rules (no Spring, no DB) so the important logic is trivially unit-testable. Entities are never returned from the API; DTOs are the contract with the React app.

## "How do you stop two people buying the last item?"
One transaction per checkout: lock variant rows with `SELECT … FOR UPDATE` in ascending id order (no deadlocks), re-read stock, check, reserve, save. `READ_COMMITTED` so reads after the lock see the latest committed data. Also `CHECK (reserved_qty <= stock_qty)` as a last line of defence in the DB.

## "What is reservation and why not just deduct stock at checkout?"
Unpaid orders would permanently lock stock. Reserve at checkout, commit on payment, release on cancel/expiry. A scheduled job releases abandoned reservations. `available = stock − reserved`.

## "Why batches and expiry?"
Cosmetics expire. Selling FEFO reduces waste; expired stock is never sold; each order item records which batch supplied it, so cancellations restock the same batch and a recall can find affected orders.

## "How do you make payments safe?"
The browser never decides the price. The PayHere webhook is verified: MD5 signature (constant-time compare), idempotency (UNIQUE payment id, so duplicates are ignored), and amount/currency match. Only then `PAID`. The webhook is public but cannot be forged. Wrong amounts are flagged for a human.

## "Why is MD5 used? Isn't it broken?"
PayHere's protocol requires it. It is used as a keyed message-authentication hash with a secret merchant key, not for password storage. Passwords use BCrypt.

## "Why a Strategy for checkout?"
Open/Closed: adding Cash-on-Delivery or Stripe is one new `CheckoutStrategy` class, with no edits to `OrderService`. Spring injects all strategies; the registry indexes them by method.

## "Why a state machine?"
Illegal jumps (DELIVERED → PAID) are impossible. It is one table, easy to review and unit test, and it also decides the stock effect of each transition.

## "JWT in a cookie, why not localStorage?"
An httpOnly cookie can't be read by JavaScript, so an XSS bug can't steal the token. The trade-off is CSRF, handled with SameSite + an Origin check + JSON-only bodies. The role is read from the DB on each request, so revocation is immediate.

## "Money as cents?"
Floating point can't represent 0.10 exactly; integers can't drift. Totals are `long` so a large cart can't overflow.

## "What would you do with more time?"
Testcontainers integration tests (concurrent checkout: N threads, 1 unit, exactly 1 wins); Redis-backed rate limiting; refresh tokens; email notifications; admin audit log screen; CI pipeline (build + test + lint).

## Be honest about
In-memory rate limiter (single instance), in-memory price sort, no refunds/emails.
