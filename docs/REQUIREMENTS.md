# Online Flower Shop — Requirements Specification

This document defines the problem, users, scope, functional and non-functional
requirements, and representative user stories for the Online Flower Shop
system. It complements `docs/ARCHITECTURE.md`, which describes how these
requirements are implemented (layered Spring Boot backend, PostgreSQL schema,
REST API). Nothing here introduces functionality beyond what is described in
the architecture document and implemented under `backend/src/main/java/com/flowershop/`.

## 1. Problem Statement

A small-to-medium flower shop currently takes orders in person, by phone, or
informally over messaging apps. This approach does not scale: the shop owner
has no central record of inventory levels, orders are tracked on paper or in
spreadsheets, there is no way for a customer to see whether a given flower is
in stock before travelling to the shop, and delivery is coordinated by word
of mouth with no record of who is delivering what, or whether a delivery
actually happened. Payment collection is manual (cash or informal mobile
transfer) with no reconciliation against orders. As the shop's customer base
grows, this process causes lost orders, overselling of out-of-stock items,
delivery mix-ups, and no reliable way to measure sales performance.

The Online Flower Shop system solves this by providing a web-based platform
where customers can browse and search a live catalogue, place and pay for
orders online, and track delivery status, while shop staff manage inventory
and orders from a dedicated admin portal and delivery personnel manage their
assigned deliveries from their own portal — all backed by a single, consistent
database of record.

## 2. Target Users

The system serves three personas, matching the three roles enforced by the
application (`CUSTOMER`, `ADMIN`, `DELIVERY` in `users.role`):

### 2.1 Customer
A member of the public who wants to buy flowers for themselves or to be
delivered to someone else (e.g. for a birthday, anniversary, or funeral).
They need to:
- Browse and search the catalogue by name and filter by category without
  creating an account.
- Register and log in securely to place orders.
- Add items to a cart, review the total, and check out with an address and
  an online payment.
- See the status of their past and current orders, and know when a delivery
  is out for delivery or has arrived.
- Leave a rating and review for a flower they have actually received, to
  help other customers decide.

### 2.2 Admin / Shop Staff
The shop owner or staff member responsible for running the business day to
day. They need to:
- Maintain the catalogue: create, edit, deactivate categories and flowers,
  and keep stock quantities accurate.
- See and process incoming orders, moving them through their lifecycle
  (confirm, start processing, mark shipped).
- Assign confirmed orders to a delivery person and monitor delivery
  progress across all orders.
- Manage staff accounts (create delivery-person and admin logins, deactivate
  accounts that should no longer be able to sign in).
- View sales and inventory reports to understand revenue over a date range
  and which items are low on stock.

### 2.3 Delivery Person
Shop staff (or a contracted courier) responsible for physically delivering
orders. They need to:
- See only the deliveries assigned to them, not the whole shop's order
  book.
- Update a delivery's status as it progresses (assigned → out for delivery →
  delivered, or failed) and attach notes (e.g. "left with neighbour",
  "recipient unavailable").
- Have confidence that marking an order delivered is what unlocks the
  customer's ability to leave a review, so status accuracy matters.

## 3. Project Objectives and Scope

### 3.1 Objectives
- Replace manual, in-person/phone order-taking with a self-service online
  storefront.
- Give the shop a single source of truth for inventory, orders, payments,
  and deliveries.
- Provide real-time (or near-real-time) order and delivery status visibility
  to customers, reducing "where is my order" inquiries.
- Let the shop collect payment online, with a record tied to each order.
- Give the shop owner reporting to understand sales and stock without manual
  bookkeeping.
- Build customer trust through a review system restricted to customers who
  actually received the product (verified-purchase gating).

### 3.2 In Scope
- Customer-facing storefront: catalogue browsing, search, category filter,
  cart, checkout, order history, order tracking, reviews.
- Admin portal: category/flower CRUD, stock management, order processing,
  delivery assignment, staff account management, sales and inventory
  reports.
- Delivery portal: assigned-deliveries list, delivery status updates.
- JWT-based authentication and role-based authorization for three roles.
- Online payment via Stripe (PaymentIntent creation and webhook-driven
  status update).
- A single-vendor, single-currency, single-shop deployment running against
  one PostgreSQL database.

### 3.3 Out of Scope
The following are explicitly **not** part of this system, to keep scope
consistent with what is actually built:
- Multi-vendor marketplace functionality (multiple independent shops
  selling through one platform).
- In-app chat or real-time messaging between customers, staff, and delivery
  personnel (status updates are polled/refreshed, not pushed over a socket).
- Subscription or recurring-order ("flower of the month") billing.
- Native mobile applications (the frontend is a responsive web client only).
- Third-party courier/logistics API integration (route optimization, live
  GPS tracking of the delivery person) — delivery status is manually updated
  by the delivery person, not auto-derived from a tracking device.
- Multi-currency or multi-language support.
- Loyalty points, promotional discount codes, or gift cards.
- Automated email/SMS notifications (not implemented in the current backend;
  status changes are visible when the user views the relevant screen, not
  pushed to them).

## 4. Principal Functional / Business Requirements

### Customer
- FR-1: A visitor can browse the flower catalogue and view flower details
  (name, description, price, image, average rating) without logging in.
- FR-2: A visitor can search the catalogue by keyword and filter by category
  (`GET /api/flowers?search=&category=`).
- FR-3: A visitor can register for an account and log in to receive a JWT
  (`POST /api/auth/register`, `POST /api/auth/login`).
- FR-4: A logged-in customer can build a cart client-side and submit it as
  an order with a delivery address (`POST /api/orders`).
- FR-5: A customer can pay for an order online via Stripe
  (`POST /api/payments`) and the order/payment status reflects the outcome
  once Stripe confirms it via webhook.
- FR-6: A customer can view their own order history and the status of each
  order (`GET /api/orders/my`, `GET /api/orders/{id}`).
- FR-7: A customer can view the delivery status associated with one of
  their orders (`GET /api/deliveries/order/{orderId}`).
- FR-8: A customer who has an order containing a given flower with status
  `DELIVERED` can leave one rating (1–5) and comment per flower
  (`POST /api/flowers/{flowerId}/reviews`), and can edit or delete only
  their own review afterward.

### Admin
- FR-9: An admin can create, update, deactivate, and reactivate flowers and
  categories (`POST`/`PUT`/`DELETE`/`PATCH` on `/api/flowers`,
  `/api/categories`).
- FR-10: An admin can adjust stock quantity for a flower directly
  (`PATCH /api/flowers/{id}/stock`).
- FR-11: An admin can view and filter all customer orders
  (`GET /api/orders`) and advance an order's status
  (`PATCH /api/orders/{id}/status`), following the sequence
  `PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED`, with
  `CANCELLED` reachable as an exception path.
- FR-12: An admin can assign a confirmed order to a delivery person
  (`POST /api/deliveries/{id}/assign`) and view all deliveries in progress
  (`GET /api/deliveries`).
- FR-13: An admin can create delivery-person and admin staff accounts
  (`POST /api/users/staff`) and activate/deactivate any user account
  (`PATCH /api/users/{id}/status`).
- FR-14: An admin can generate a sales report for a date range and an
  inventory report of current stock levels
  (`GET /api/reports/sales?from=&to=`, `GET /api/reports/inventory`).

### Delivery
- FR-15: A delivery person can view only the deliveries assigned to them
  (`GET /api/deliveries/my`).
- FR-16: A delivery person can update the status of their assigned delivery
  (`ASSIGNED → OUT_FOR_DELIVERY → DELIVERED`, or `FAILED`) and attach notes
  (`PATCH /api/deliveries/{id}/status`); this is the action that ultimately
  marks the parent order `DELIVERED` and unlocks reviews for the customer.

### Cross-cutting
- FR-17: Every endpoint other than catalogue browsing, registration, and
  login requires a valid JWT; role-specific endpoints additionally require
  the matching role, enforced with `@PreAuthorize`.
- FR-18: A user who is deactivated (`is_active = false`) cannot authenticate
  successfully even with correct credentials.

## 5. Measurable Quality Attributes

These targets are consistent with, and do not contradict, the non-functional
requirements table in `docs/ARCHITECTURE.md` section 5.

| Attribute | Measurable Target |
|---|---|
| **Usability** | A first-time customer can go from landing on the catalogue page to a completed checkout in 5 or fewer page/screen transitions (browse → cart → address → pay → confirmation). Page layouts remain usable (no horizontal scrolling, all primary actions reachable) down to a 375px viewport width, per the mobile-first breakpoints (375/768/1280px) in ARCHITECTURE.md. |
| **Performance** | 95% of catalogue search/list requests (`GET /api/flowers`) return within 300ms under typical load (single-shop traffic, indexed `category_id` lookups per the schema's `idx_flowers_category` index). List endpoints are paginated (`Pageable`) so no endpoint returns an unbounded result set. |
| **Reliability / Availability** | The backend is stateless (JWT-based, no server-side session), so a restart or redeploy does not invalidate in-flight user sessions beyond the active request. Database backups are taken on a scheduled basis (`scripts/backup-db.ps1`) with a documented, tested restore path (`scripts/restore-db.ps1`), targeting no more than 24 hours of data loss in a recovery scenario. |
| **Security** | Passwords are never stored in plaintext — only BCrypt hashes (`users.password_hash`). Authentication is JWT-based with role claims checked via `@PreAuthorize` on every protected endpoint. All data access goes through Spring Data JPA/Hibernate (parameterized queries), eliminating raw-SQL injection risk. All traffic is intended to run over HTTPS in production. A deactivated account (`is_active = false`) is rejected at login 100% of the time. |
| **Data Integrity** | Database-level CHECK constraints (e.g. `price >= 0`, `stock_quantity >= 0`, `quantity > 0`, `rating BETWEEN 1 AND 5`, enumerated status columns) and foreign keys prevent invalid or orphaned rows regardless of application-layer bugs. A customer can have at most one review per flower, enforced by the `UNIQUE (customer_id, flower_id)` constraint on `reviews`. |
| **Maintainability** | The backend follows a strict layered structure (controller → service → repository → entity) with DTOs isolating the HTTP contract from persistence entities, so a schema or business-rule change is localized to one or two layers rather than scattered across the codebase. |

## 6. User Stories with Acceptance Criteria

### US-1 — Customer checkout
**As a** customer,
**I want to** add flowers to my cart and pay for them online,
**so that** I can order flowers without visiting the shop in person.

Acceptance criteria:
- Given I am logged in with a valid JWT, when I submit a cart of one or more
  in-stock flowers with quantities and a delivery address to
  `POST /api/orders`, then an order is created with status `PENDING` and a
  `total_amount` equal to the sum of each line's `quantity × unit_price`.
- Given I have a `PENDING` order, when I initiate payment via
  `POST /api/payments`, then a Stripe PaymentIntent is created and linked to
  that order's `payments` row with status `PENDING`.
- Given Stripe confirms the payment via the webhook
  (`POST /api/payments/webhook`), when the webhook is processed, then the
  payment status is updated to `SUCCESS` and `paid_at` is set.
- Given I attempt to order a flower with insufficient `stock_quantity`, when
  I submit the order, then the request is rejected and no order is created.
- Given I have placed orders, when I call `GET /api/orders/my`, then I see
  only my own orders, each with its current status, never another
  customer's orders.

### US-2 — Admin inventory management
**As an** admin,
**I want to** manage the flower catalogue and stock levels,
**so that** customers never see or order flowers that are unavailable, and
the catalogue reflects what the shop can actually fulfil.

Acceptance criteria:
- Given I am authenticated as `ADMIN`, when I call
  `POST /api/flowers` with a name, category, price, and initial stock
  quantity, then a new flower is created and immediately visible to
  customers via `GET /api/flowers` (subject to `is_active = true`).
- Given an existing flower, when I call `PATCH /api/flowers/{id}/stock`
  with a new quantity, then the flower's `stock_quantity` is updated and
  that change is reflected the next time any client fetches the flower.
- Given a flower I no longer want to sell, when I call
  `DELETE /api/flowers/{id}`, then the flower is deactivated
  (`is_active = false`) and no longer appears in default customer catalogue
  queries, but its historical `order_details` rows are preserved.
- Given I am authenticated as `CUSTOMER` or not authenticated at all, when I
  attempt any of the above admin write operations, then the request is
  rejected with an authorization failure, not performed.
- Given I call `GET /api/reports/inventory`, then I receive current stock
  levels across all active flowers, so I can identify low-stock items.

### US-3 — Delivery status update
**As a** delivery person,
**I want to** see my assigned deliveries and update their status as I
complete them,
**so that** the shop and the customer both know where their order is, and
the customer is only invited to review a flower once they've actually
received it.

Acceptance criteria:
- Given an admin has assigned an order to me via
  `POST /api/deliveries/{id}/assign`, when I call `GET /api/deliveries/my`,
  then that delivery appears in my list with status `ASSIGNED`, and
  deliveries assigned to other delivery staff do not appear.
- Given a delivery assigned to me, when I call
  `PATCH /api/deliveries/{id}/status` with `OUT_FOR_DELIVERY`, then the
  delivery's status updates accordingly and is visible to the customer via
  `GET /api/deliveries/order/{orderId}`.
- Given a delivery assigned to me, when I mark it `DELIVERED` (optionally
  with notes), then `delivered_at` is set, the parent order's status moves
  to `DELIVERED`, and the customer becomes eligible to submit a review for
  each flower in that order.
- Given a delivery is not assigned to me, when I attempt to update its
  status, then the request is rejected.
- Given a delivery cannot be completed (e.g. recipient unavailable), when I
  mark it `FAILED` with a note, then that reason is recorded and visible to
  the admin for follow-up.
