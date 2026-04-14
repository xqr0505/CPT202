# ExpertLink Consult System

A full-stack consulting system built with Spring Boot (backend) and Vue 3 + Vite (frontend).

## Project Structure

- `backend/` — Spring Boot API, MySQL, Flyway, MyBatis-Plus
- `frontend/` — Vue 3 application
- `docs/` — project notes and structure docs

## Prerequisites

Make sure the following are installed locally:

- Java 17
- Maven 3.9+ or the included Maven Wrapper
- Node.js 20.19.0+ or 22.12.0+
- npm
- MySQL 8.0+

## Required Local Setup

### 1) Create the database

The backend uses MySQL database `cpt202_consultancy`.

Run the following SQL in your local MySQL client:

```sql
CREATE DATABASE IF NOT EXISTS cpt202_consultancy
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

### 2) Check backend database credentials

Backend dev config is in:

- `backend/src/main/resources/application-dev.yml`

Current defaults:

- host: `127.0.0.1`
- port: `3306`
- database: `cpt202_consultancy`
- username: `root`
- password: `12345`

If your local MySQL settings are different, update that file before running the backend.

## Run the Backend

Open a terminal in the repository root and run:

```powershell
cd backend
./mvnw spring-boot:run
```

If you prefer system Maven, use:

```powershell
cd backend
mvn spring-boot:run
```

Backend defaults:

- URL: `http://localhost:8081`
- API docs: `http://localhost:8081/swagger-ui/index.html`

## Run the Frontend

Open another terminal in the repository root and run:

```powershell
cd frontend
npm install
npm run dev
```

Frontend defaults:

- URL: `http://localhost:5173`

## Quick Start Checklist

1. Start MySQL.
2. Create the `cpt202_consultancy` database.
3. Confirm `backend/src/main/resources/application-dev.yml` matches your MySQL credentials.
4. Start the backend.
5. Start the frontend.
6. Open the frontend in your browser.
7. Open Swagger UI if you want to test APIs directly.

## Validation Commands

Run these if you want to verify the project after setup:

### Backend

```powershell
cd backend
./mvnw test
./mvnw -DskipTests package
```

### Frontend

```powershell
cd frontend
npm run type-check
npm run build
npm run lint
```

## Troubleshooting

- If the backend fails to start, check MySQL is running and the database credentials in `application-dev.yml` are correct.
- If the frontend cannot reach the backend, confirm the backend is running on `http://localhost:8081`.
- If dependencies are missing, run `npm install` again in `frontend/` and rerun the backend Maven command.

## Run With Docker

Docker (production-like + dev) instructions are in:

- `README_DOCKER.md`

