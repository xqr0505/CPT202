# Docker Setup for ExpertLink Consult System

This guide provides instructions for setting up and running the ExpertLink Consult System using Docker.

## Prerequisites
- Docker installed on your system
- Docker Compose installed

## Services Overview
The `docker-compose.yml` file defines the following services:

1. **Database (MySQL)**
   - Image: `mysql:8.0`
   - Port: `3306`
   - Environment Variables:
     - `MYSQL_ROOT_PASSWORD`: Root password for MySQL
     - `MYSQL_DATABASE`: Name of the database
   - Volume: `db_data` for persistent storage

2. **Backend**
   - Build Context: `./backend`
   - Dockerfile: `Dockerfile`
   - Port: `8081`
   - Environment Variables:
     - `SPRING_DATASOURCE_URL`: Database connection URL
     - `SPRING_DATASOURCE_USERNAME`: Database username
     - `SPRING_DATASOURCE_PASSWORD`: Database password
   - Depends on: `db`

3. **phpMyAdmin**
   - Image: `phpmyadmin/phpmyadmin:latest`
   - Port: `8080`
   - Environment Variables:
     - `PMA_HOST`: Hostname of the database
     - `PMA_USER`: Database username
     - `PMA_PASSWORD`: Database password
   - Depends on: `db`

4. **Frontend**
   - Build Context: `./frontend`
   - Dockerfile: `Dockerfile`
   - Port: `8082`
   - Depends on: `backend`

## How to Run
1. Clone the repository:
   ```bash
   git clone https://github.com/xqr0505/CPT202.git
   cd expertlink-consult-system
   ```

2. Start the services:
   ```bash
   docker-compose up -d
   ```

3. Access the services:
   - **phpMyAdmin**: [http://localhost:8080](http://localhost:8080)
   - **Frontend**: [http://localhost:8082](http://localhost:8082)
   - **Backend**: [http://localhost:8081](http://localhost:8081)

4. Stop the services:
   ```bash
   docker-compose down
   ```

## Notes
- Ensure that the ports `3306`, `8080`, `8081`, and `8082` are not in use by other applications.
- Modify the `docker-compose.yml` file if you need to change the default configurations.

## Troubleshooting
- If you encounter issues, check the logs for each service:
  ```bash
  docker-compose logs <service-name>
  ```
  Replace `<service-name>` with `db`, `backend`, `phpmyadmin`, or `frontend`.

- Ensure Docker and Docker Compose are up-to-date.

## Volumes
- `db_data`: Stores MySQL data persistently.

## License
This project is licensed under the MIT License.
