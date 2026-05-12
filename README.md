# ExpertLink Consult System

ExpertLink is a full-stack doctor appointment booking system.

- Backend: Spring Boot (Java 17), MySQL, Redis Stack
- Frontend: Vue 3 + Vite + Element Plus

## Project Structure

- `backend/` Spring Boot API (MySQL, Flyway, MyBatis-Plus, Redis)
- `frontend/` Vue 3 application
- `docs/` Notes and docs
- `scripts/` Local dev helper scripts (Windows)

## Prerequisites (Local)

- Java 17
- Maven 3.9+ (or `backend/mvnw`)
- Node.js `^20.19.0 || >=22.12.0` and npm
- MySQL 8.0+
- Redis Stack (recommended) or a compatible Redis server

## Environment Variables

This repo uses a root `.env` file for local/dev and Docker configuration.

1. Create `.env` from the template:

```bash
cp .env.example .env
```

2. Fill in required values (at minimum):

- `DB_PASSWORD`
- `JWT_SECRET` (at least 32 characters)

Optional (only needed if you use these features):

- Email: `MAIL_PASSWORD`
- AI: `OPENAI_API_KEY`, `DASHSCOPE_API_KEY` (and related model/base URL settings)

If you run Redis locally (not via Docker), set:

- `REDIS_HOST=127.0.0.1`
- `REDIS_PORT=6379`

## Run Locally (Recommended Dev Flow)

On Windows, the easiest flow is: start infrastructure via Docker, and run backend/frontend on the host.

```powershell
scripts\run-local-dev.cmd
```

Default endpoints:

- Frontend (Vite): `http://localhost:5331`
- Backend (Spring Boot dev): `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`

This dev flow uses Docker services by default:

- MySQL: `127.0.0.1:9001`
- Redis Stack server: `127.0.0.1:9002`

## Run Manually (No Helper Scripts)

1. Start MySQL and Redis Stack (or Redis).

2. Create the database:

```sql
CREATE DATABASE IF NOT EXISTS cpt202_consultancy
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

3. Start backend:

```powershell
cd backend
./mvnw spring-boot:run
```

4. Start frontend:

```powershell
cd frontend
npm install
npm run dev
```

## Docker (Recommended for Sharing / Deployment)

For a production-like setup and a full Docker-based dev setup, use Docker.
See `README_DOCKER.md`.

## Troubleshooting

- Backend fails to start: check MySQL/Redis are reachable and your `.env` values are set.
- Frontend cannot reach backend: ensure backend is on `http://localhost:8081` and `FRONTEND_VITE_API_BASE_URL_DEV` ends with `/api`.

## Test Accounts
### admin
```
test.admin@expertlink.com
```

```
Admin@123456
```

### customer
```
test.customer@expertlink.com
```

```
Test12345
```

### specialist
>This account is a pre-configured test specialist user that exists only on the server environment. it is not automatically created when you run the source code locally or in your own deployment.

```
test.specialist@expertlink.com
```

```
12345Expertlink
```

