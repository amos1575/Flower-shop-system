# Flower Shop System

An online flower shop with three portals — **customer** (browse, cart, checkout, order tracking, reviews), **admin** (inventory, orders, delivery assignment, staff, sales/inventory reports), and **delivery** (assigned deliveries, status updates) — backed by a single REST API.

For the full architecture, database schema, and endpoint reference, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). This file is about getting it running.

## How it fits together

```
frontend-react/  (React + Vite)  ──HTTP/JSON──▶  backend/  (Spring Boot)  ──JDBC──▶  PostgreSQL
     :5173                                            :8080
```

- **`backend/`** — Spring Boot 3 (Java 21) REST API. Stateless JWT auth, three roles (`CUSTOMER`, `ADMIN`, `DELIVERY`) sharing one `users` table, Stripe integration for payments. Hibernate creates/updates the schema automatically on startup (`ddl-auto: update`) — no separate migration step needed for local dev.
- **`frontend-react/`** — the current frontend. One React app, routed by portal (`/customer/*`, `/admin/*`, `/delivery/*`), each gated by role via the JWT stored after login.
- **`frontend/`** — the original static HTML/JS version the React app replaced. Kept only as a reference; nothing depends on it, and it can be deleted.
- **`docs/`** — architecture doc.
- **`scripts/`** — PowerShell helpers to back up / restore the Postgres database via `pg_dump`/`pg_restore`.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java (JDK) | 21 | backend build/run |
| Maven | 3.8+ | or use an IDE's bundled Maven |
| Node.js | 18+ | frontend build/run (comes with npm) |
| PostgreSQL | 14+ | running locally, listening on 5432 |

## First-time setup on a new machine

### 1. Create the database

```bash
psql -U postgres -c "CREATE DATABASE flowershop;"
```

The backend does **not** create the database itself — only the tables inside it (via Hibernate on first boot).

### 2. Configure the backend (optional for local dev)

`backend/src/main/resources/application.yml` has working defaults for everything (DB credentials, JWT secret, seeded admin login, Stripe test placeholders), so you can run it as-is locally. For anything beyond your own machine, override via environment variables instead of editing the file:

| Variable | Purpose |
|---|---|
| `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL credentials |
| `JWT_SECRET` | Signing key for auth tokens — must change before any real deployment |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Seeded default admin account (created once on first boot if that email doesn't already exist) |
| `STRIPE_SECRET_KEY` / `STRIPE_WEBHOOK_SECRET` | Stripe API credentials — payment flows won't work until these are real keys |

### 3. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Wait for `Started FlowerShopApplication` in the log. The API is now at `http://localhost:8080/api`.

### 4. Run the frontend

In a separate terminal:

```bash
cd frontend-react
npm install
npm run dev
```

Open the URL it prints (`http://localhost:5173`).

### 5. Log in

- **Admin**: `admin@flowershop.local` / `ChangeMe123!` (the seeded default — change it or set `ADMIN_EMAIL`/`ADMIN_PASSWORD` before production).
- **Customer**: register your own account from the customer portal.
- **Delivery**: no self-registration — an admin creates delivery-staff accounts from the admin portal's Staff page.

## Everyday commands

| Task | Command |
|---|---|
| Run backend | `cd backend && mvn spring-boot:run` |
| Run backend tests | `cd backend && mvn test` (76 tests, no DB/broker needed) |
| Run frontend (dev) | `cd frontend-react && npm run dev` |
| Build backend jar | `cd backend && mvn clean package -DskipTests` |
| Run the built jar | `java -jar backend/target/flower-shop-backend-0.1.0-SNAPSHOT.jar` |
| Build frontend for deploy | `cd frontend-react && npm run build` (outputs static files to `frontend-react/dist/`) |
| Back up the database | `.\scripts\backup-db.ps1` |
| Restore the database | `.\scripts\restore-db.ps1 -BackupFile ".\backups\<file>.backup"` |

## Testing the API with Postman

[`postman/flower-shop.postman_collection.json`](postman/flower-shop.postman_collection.json) exercises full CRUD (Create/Read/Update/Delete) for three entities — **Categories**, **Flowers**, and **Reviews** — plus the business-logic validation each one enforces (duplicate names rejected, insufficient-stock and inactive-flower checks, the delivered-order gate on reviews, ownership checks, Bean Validation on bad input). Import it into Postman and run the whole collection top-to-bottom (Collection Runner), or from the command line:

```bash
npx newman run postman/flower-shop.postman_collection.json
```

Requires the backend running at `http://localhost:8080` with the default seeded admin account. All 38 requests / 58 assertions pass against a clean database.

## Troubleshooting

- **"Port 8080 was already in use"** — the backend is already running somewhere (another terminal, a leftover background process). Find and stop it, or free the port, before starting a new instance.
- **Backend fails to connect to Postgres** — confirm Postgres is running (`pg_isready -h localhost -p 5432`) and that the `flowershop` database exists (step 1 above).
- **Frontend loads but API calls fail / CORS errors** — the backend only accepts requests from `localhost`/`127.0.0.1` origins by default (see `SecurityConfig`). Running the frontend from any other host requires adding that origin there.
- **Login succeeds but redirects back to a login page** — you're logged in as the wrong role for that portal (e.g. a customer account on `/admin`); each portal's login only accepts its own role.
