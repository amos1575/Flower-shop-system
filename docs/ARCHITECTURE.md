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

## 10. Polyglot Persistence: MongoDB Order Audit Log

Alongside the PostgreSQL schema above (the system of record for orders, inventory, users), order lifecycle events are also written to a MongoDB collection, `order_audit_logs`:

```
OrderAuditLog {
  id: ObjectId
  orderId: Long          (indexed, not a foreign key — Mongo doesn't enforce referential
                           integrity against Postgres; the relationship is logical)
  previousStatus: String | null
  newStatus: String
  changedByEmail: String
  changedByRole: String
  note: String | null
  timestamp: LocalDateTime
}
```

**Why a second store instead of another Postgres table:** this data is append-only, write-heavy relative to how often it's read, has no relationships to other entities that need enforcing, and its "schema" is really just a log line shape that doesn't need migrations as it evolves. That's the textbook case for choosing a document store over adding another relational table — polyglot persistence (using the right data store per workload) rather than a single one-size-fits-all database.

**Where it's written:** `OrderService.createOrder` (on placement) and `OrderService.updateStatus` (on every status transition), via `OrderAuditLogRepository`.

**Where it's read:** `GET /api/orders/{id}/audit-log` (ADMIN only).

**Resilience:** both the write and the read path catch and log Mongo exceptions rather than propagating them — a MongoDB outage degrades to "no audit entry recorded" / "audit log temporarily unavailable," it never breaks order creation or status updates, which remain fully functional against Postgres alone. Verified live: the backend starts and serves orders normally with MongoDB stopped.

**Configuration:** `spring.data.mongodb.uri`, overridable via the `MONGO_URI` environment variable (defaults to `mongodb://localhost:27017/flowershop_audit` for local dev — no auth, matching how the rest of this project's local-dev defaults work).
