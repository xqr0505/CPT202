# Docker Setup for ExpertLink Consult System

This guide provides instructions for setting up and running the ExpertLink Consult System using Docker.

## Prerequisites
- Docker installed on your system
- Docker Compose installed (`docker compose`)

## Services Overview
This repo provides two compose files:

- `docker-compose.yml`: production-like build (builds JAR + Nginx static)
- `docker-compose.dev.yml`: development setup (source-mounted, hot reload)

### Production-like (`docker-compose.yml`)
Defines the following services:

1. **Database (MySQL)**
   - Image: `mysql:8.0`
   - Port: `9001` (host) -> `3306` (container)
   - Environment Variables:
     - `MYSQL_ROOT_PASSWORD`: Root password for MySQL
     - `MYSQL_DATABASE`: Name of the database
   - Volume: `db_data` for persistent storage

2. **Redis**
   - Image: `redis:7-alpine`
   - Port: `9002` (host) -> `6379` (container)
   - Purpose: booking cache + idempotency lock

2. **Backend**
   - Build Context: `./backend`
   - Dockerfile: `Dockerfile`
   - Port: `8080`
   - Environment Variables:
     - `SPRING_DATASOURCE_URL`: Database connection URL
     - `SPRING_DATASOURCE_USERNAME`: Database username
     - `SPRING_DATASOURCE_PASSWORD`: Database password
     - `REDIS_HOST` / `REDIS_PORT`: Redis connection
   - Depends on: `db`

4. **Frontend**
   - Build Context: `./frontend`
   - Dockerfile: `Dockerfile`
   - Port: `80`
   - Depends on: `backend`

## How to Run
1. Clone the repository:
   ```bash
   git clone https://github.com/xqr0505/CPT202.git
   cd expertlink-consult-system
   ```

2. Create the environment file:
   ```bash
   cp .env.example .env
   ```
   Then fill in the real values for `DB_PASSWORD`, `MAIL_PASSWORD`, and `JWT_SECRET`.
   If you already have Redis running locally, set `REDIS_HOST` to your host address.

3. Start the services:
   ```bash
   docker compose up -d
   ```

4. Access the services:
   - **Frontend**: `http://localhost`
   - **Backend**: `http://localhost:8080`
   - **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

5. Stop the services:
   ```bash
   docker compose down
   ```

## Development (`docker-compose.dev.yml`)
This mode mounts source code into containers for faster iteration.

1. Create `.env`:
   ```bash
   cp .env.example .env
   ```
2. Start dev services:
   ```bash
   docker compose -f docker-compose.dev.yml up -d
   ```
3. Access:
   - **Frontend (Vite)**: `http://localhost:5331`
   - **Backend (dev profile)**: `http://localhost:8081`
   - **Swagger UI**: `http://localhost:8081/swagger-ui/index.html`
   - **phpMyAdmin**: `http://localhost:9003`
   - **RedisInsight**: `http://localhost:5540`

   If RedisInsight shows a "protected mode" error, recreate the Redis container to apply the dev command flags:
   ```bash
   docker compose -f docker-compose.dev.yml up -d --force-recreate redis
   ```

## Notes
- Ensure that the ports `80`, `5331`, `5540`, `8080`, `8081`, `9001`, `9002`, `9003` are not in use by other applications.
- Modify the compose files if you need to change the default configurations.

## Troubleshooting
- If you encounter issues, check the logs for each service:
  ```bash
  docker compose logs <service-name>
  ```
  Replace `<service-name>` with `db`, `redis`, `redisinsight`, `backend`, or `frontend`.

- Ensure Docker and Docker Compose are up-to-date.

## Volumes
- `db_data`: Stores MySQL data persistently.

## License
This project is licensed under the MIT License.
