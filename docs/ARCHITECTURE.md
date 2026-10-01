# Online Flower Shop — Architecture & Plan

Stack: **Java (Spring Boot)** backend, **PostgreSQL** database, **HTML/CSS/JS** frontend (server-rendered via Thymeleaf or a separate static/SPA client — decision point below).

## 1. High-Level Architecture

```
┌─────────────────────┐        HTTPS/REST (JSON)        ┌──────────────────────────┐
│  Frontend            │  <────────────────────────────>  │  Spring Boot Backend      │
│  HTML/CSS/JS          │                                  │  (Controller → Service    │
│  (or Thymeleaf views) │                                  │   → Repository → Entity)  │
└─────────────────────┘                                  └───────────┬──────────────┘
                                                                      │ JPA/Hibernate
                                                                      ▼
                                                            ┌──────────────────┐
                                                            │   PostgreSQL      │
                                                            └──────────────────┘
```

Layered backend (standard Spring Boot conventions):
- `controller` — REST endpoints, request/response DTOs
- `service` — business logic, transactions
- `repository` — Spring Data JPA interfaces
- `entity` — JPA entities mapping to DB tables
- `dto` — request/response payloads (never expose entities directly)
- `security` — Spring Security config, JWT filter, role-based access
- `exception` — global exception handler (`@ControllerAdvice`)

## 2. Roles & Access Control

Three roles via a single `users.role` enum column (`CUSTOMER`, `ADMIN`, `DELIVERY`), enforced with Spring Security `@PreAuthorize`:

| Role | Access |
|---|---|
| Customer | browse/search flowers, cart, checkout, pay, track own orders, leave reviews |
| Admin | CRUD categories/flowers, manage customer accounts, process orders, update inventory, sales reports |
| Delivery | view assigned deliveries, update delivery status, confirm delivery |

## 3. Database Schema (PostgreSQL)

```sql
-- Users (customers, admins, delivery personnel share one table + role)
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    phone           VARCHAR(30),
    address         TEXT,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER','ADMIN','DELIVERY')),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) UNIQUE NOT NULL,
    description     TEXT
);

CREATE TABLE flowers (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT NOT NULL REFERENCES categories(id),
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    stock_quantity  INT NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    image_url       VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED')),
    total_amount    NUMERIC(10,2) NOT NULL,
    delivery_address TEXT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_details (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    flower_id       BIGINT NOT NULL REFERENCES flowers(id),
    quantity        INT NOT NULL CHECK (quantity > 0),
    unit_price      NUMERIC(10,2) NOT NULL
);

CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id),
    amount          NUMERIC(10,2) NOT NULL,
    method          VARCHAR(30) NOT NULL,           -- CARD, MOBILE_MONEY, PAYPAL, ...
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    transaction_ref VARCHAR(255) UNIQUE,
    paid_at         TIMESTAMP
);

CREATE TABLE deliveries (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    delivery_person_id  BIGINT REFERENCES users(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED'
                        CHECK (status IN ('UNASSIGNED','ASSIGNED','OUT_FOR_DELIVERY','DELIVERED','FAILED')),
    assigned_at         TIMESTAMP,
    delivered_at        TIMESTAMP,
    notes               TEXT
);

CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    customer_id     BIGINT NOT NULL REFERENCES users(id),
    flower_id       BIGINT NOT NULL REFERENCES flowers(id),
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (customer_id, flower_id)
);

-- indexes for common lookups
CREATE INDEX idx_flowers_category ON flowers(category_id);
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_order_details_order ON order_details(order_id);
CREATE INDEX idx_reviews_flower ON reviews(flower_id);
```

Payments is kept separate from orders (1-to-many, though typically 1-to-1) so retries/refunds don't mutate order history.

## 4. Backend Modules & Key REST Endpoints

**Auth** (`/api/auth`)
- `POST /register`, `POST /login` (returns JWT), `POST /refresh`

**Flowers / Catalog** (`/api/flowers`) — public read, admin write
- `GET /flowers?search=&category=&page=`
- `GET /flowers/{id}`
- `POST /flowers`, `PUT /flowers/{id}`, `DELETE /flowers/{id}` (ADMIN)

**Categories** (`/api/categories`) — ADMIN write, public read

**Cart** — can be client-side (localStorage) until checkout, or `POST /api/cart` if persistence across devices is wanted (decision point below)

**Orders** (`/api/orders`)
- `POST /orders` (CUSTOMER — checkout from cart)
- `GET /orders/my` (CUSTOMER)
- `GET /orders/{id}/status` (CUSTOMER, DELIVERY, ADMIN)
- `GET /orders` (ADMIN — all orders, filterable)
- `PATCH /orders/{id}/status` (ADMIN)

**Payments** (`/api/payments`)
- `POST /payments` (initiate payment for an order)
- Webhook endpoint for payment gateway callback: `POST /payments/webhook`

**Deliveries** (`/api/deliveries`)
- `GET /deliveries/my` (DELIVERY — assigned deliveries)
- `PATCH /deliveries/{id}/status` (DELIVERY)
- `POST /deliveries/{id}/assign` (ADMIN)

**Reviews** (`/api/flowers/{flowerId}/reviews`)
- `POST` (CUSTOMER — requires a `DELIVERED` order containing the flower; one review per customer per flower)
- `GET` (public — returns average rating, review count, and paginated reviews)
- `PUT /{reviewId}`, `DELETE /{reviewId}` (CUSTOMER — only the review's own author; enforced in `ReviewService`)

**Reports** (`/api/reports`, ADMIN only)
- `GET /reports/sales?from=&to=`
- `GET /reports/inventory`

## 5. Non-Functional Requirements — how they're addressed

| Requirement | Approach |
|---|---|
| Security | Spring Security + JWT, BCrypt password hashing, HTTPS only, input validation via `@Valid`/Bean Validation, parameterized queries via JPA (no raw SQL injection risk) |
| Performance | Pagination on list endpoints (`Pageable`), DB indexes above, connection pooling (HikariCP default) |
| Availability | Stateless backend (JWT, no server sessions) → horizontally scalable; DB backups (see below) |
| Backup/recovery | Scheduled `pg_dump` backups; document restore procedure once hosting is chosen |
| Responsive UI | Mobile-first CSS (flexbox/grid), test breakpoints at 375/768/1280px |

## 6. Suggested Project Structure

```
flower-shop-system/
├── backend/                     # Spring Boot project (Maven)
│   └── src/main/java/com/flowershop/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       ├── security/
│       └── exception/
├── frontend/                    # HTML/CSS/JS client
│   ├── customer/                # browse, cart, checkout, orders, reviews
│   ├── admin/                   # dashboard, inventory, orders, reports
│   └── delivery/                # assigned deliveries view
└── docs/
    └── ARCHITECTURE.md          # this file
```

## 7. Decisions (locked in)

1. **Frontend delivery mechanism**: static HTML/CSS/JS calling the REST API.
2. **Payment gateway**: Stripe (well-documented sandbox/test mode, simple webhook model) — integrated in Phase 4.
3. **Cart persistence**: client-side (localStorage) only for now; revisit if cross-device persistence is needed later.
4. **Hosting target**: natively-installed PostgreSQL (Windows service) for dev, no Docker; production hosting decided later.

## 8. Phased Roadmap

1. ✅ **Phase 0 — Scaffold**: Spring Boot project (Maven, dependencies: Web, Data JPA, Security, Validation, PostgreSQL driver), native local Postgres, base package structure.
2. ✅ **Phase 1 — Auth & Users**: registration/login, JWT, role-based guards.
3. ✅ **Phase 2 — Catalog**: categories + flowers CRUD, search/filter, admin inventory screens.
4. ✅ **Phase 3 — Cart & Checkout**: cart flow, order creation, order_details.
5. ✅ **Phase 4 — Payments**: Stripe integration (PaymentIntent creation + webhook handling).
6. ✅ **Phase 5 — Delivery & Tracking**: delivery assignment, status updates, customer-facing order tracking.
7. ✅ **Phase 6 — Reviews & Reports**: reviews CRUD (with verified-purchase gate), admin sales/inventory reports.
8. ✅ **Phase 7 — Polish**: responsive styling, backups, standalone deployment packaging — see section 9.

All 8 phases are complete. Backend, database, and all three frontend UIs (customer/admin/delivery) are built and verified against a real running instance.

## 9. Deployment & Operations

### Running as a standalone JAR

```
cd backend
mvn clean package -DskipTests
java -jar target/flower-shop-backend-0.1.0-SNAPSHOT.jar
```

This is the same artifact you'd deploy to a server — verified to boot and serve traffic without Maven or an IDE involved. Configure via environment variables (all have safe local-dev defaults baked in, but production should override at least the secrets):

| Variable | Purpose |
|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL credentials |
| `JWT_SECRET` | HMAC signing key for JWTs — **must** be changed from the dev default in production |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Seeded default admin account (only created if it doesn't already exist) |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` / `STRIPE_CURRENCY` | Stripe API credentials |

The frontend is static files — serve `frontend/` from any static host or web server (nginx, S3+CloudFront, etc.); update `API_BASE_URL` in `frontend/shared/js/api-client.js` to point at the deployed backend, and add that origin to `SecurityConfig`'s CORS allowed-origins (currently permits any `localhost`/`127.0.0.1` port for local dev only).

### Database backups

`scripts/backup-db.ps1` and `scripts/restore-db.ps1` wrap `pg_dump`/`pg_restore` (custom format, so restores can run against a differently-named/empty database). Usage:

```powershell
# Back up (writes a timestamped file to backups/)
.\scripts\backup-db.ps1

# Restore (drops and recreates existing objects)
.\scripts\restore-db.ps1 -BackupFile ".\backups\flowershop_20260708_202605.backup"
```

Both scripts prompt for the postgres password unless `$env:PGPASSWORD` is already set. `backups/` is gitignored since dumps contain real customer data. For production, schedule `backup-db.ps1` via Windows Task Scheduler (or the equivalent cron job if hosted on Linux) and retain a rolling window of dumps off-box.

### Production readiness notes (not yet done — flagged for awareness)

- `spring.jpa.hibernate.ddl-auto` is currently `update`, which is convenient for iterative development but risky for production schema changes. Before a real production deploy, switch to a migration tool (Flyway or Liquibase) with `ddl-auto: validate`.
- CORS is wide open to any localhost port for local development convenience; lock this down to the actual deployed frontend origin(s) before going live.

## 10. Asynchronous Notifications: RabbitMQ

Order lifecycle events are published to RabbitMQ rather than sent synchronously from the request thread that handles the order:

```
OrderService --publish--> flowershop.notifications (topic exchange)
                               |
                               | routing key: notification.order.placed
                               | routing key: notification.order.status-changed
                               v
                   flowershop.notifications.email (queue)
                               |
                               v
                   NotificationListener --> NotificationSender
```

- **Producer**: `OrderService` calls `NotificationPublisher` on order placement and on every status change (same two hook points as the MongoDB audit log in section 9 above — these are two independent consumers of the same two events, which is exactly the kind of fan-out a message broker is for).
- **Consumer**: `NotificationListener` (`@RabbitListener`) picks events off the queue and hands them to `NotificationSender`.
- **Delivery channel**: `NotificationSender` currently *simulates* delivery — it logs what would be sent rather than calling a real email/SMS provider. This mirrors how this project already ships Stripe with placeholder test keys (`STRIPE_SECRET_KEY` etc.): the integration is real and wired end-to-end, only the final "call a paid third-party API" step is stubbed. Swapping in a real `JavaMailSender` (SMTP) or an SMS gateway (e.g. Twilio) is a drop-in change inside `NotificationSender` alone — nothing else in the flow needs to change.
- **Resilience**: publishing is wrapped in a try/catch and only logged on failure. A RabbitMQ outage degrades to "no notification sent," never to a failed order. Verified live: order creation returns `201` and the order is fully persisted with RabbitMQ stopped; the failed publish attempt is logged as a warning.

**Configuration:** `spring.rabbitmq.{host,port,username,password}`, overridable via `RABBITMQ_HOST` / `RABBITMQ_PORT` / `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` (defaults: `localhost:5672`, guest/guest — RabbitMQ's own out-of-the-box defaults). Run a local broker for development with, e.g., `docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management` (the management UI at `:15672` is useful for watching the exchange/queue while testing).
