# Backend Project Structure

This document gives a short overview of the backend directory layout and what each package is used for.

## 1. Main Backend Entry

**Path:** `backend/src/main/java/edu/xjtlu/cpt202/backend/`

- `BackendApplication.java` — Spring Boot application entry point.
- `common/` — shared code used across all modules.
- `modules/` — business features organized by domain.

## 2. Shared Package: `common/`

**Path:** `backend/src/main/java/edu/xjtlu/cpt202/backend/common/`

This package contains reusable infrastructure and utility code:

- `annotation/` — custom annotations.
- `aspect/` — AOP logic.
- `config/` — Spring and application configuration.
- `constant/` — shared constants.
- `context/` — request/user context holder classes.
- `enums/` — common enums.
- `exception/` — custom exceptions and global exception handling.
- `handler/` — MyBatis-Plus and other framework handlers.
- `properties/` — configuration property classes.
- `result/` — standard API response wrappers.
- `utils/` — helper classes and common utilities.
- `validation/` — validation groups and related validation support.

## 3. Feature Modules: `modules/`

**Path:** `backend/src/main/java/edu/xjtlu/cpt202/backend/modules/`

The business code is split by feature:

### 3.1 `auth/` Module 1
Authentication and authorization related code, such as login, token handling, and role-based access control.

### 3.2 `user/` Module 2, 3
User-related features, including customer and specialist profile management.

### 3.3 `schedule/` Module 4, 5
Scheduling features, such as specialist availability and schedule search.

### 3.4 `booking/` Module 6, 7, 8, 9
Booking features, including appointment creation, management, and booking status handling.

Typical subpackages inside a module may include:

- `controller/` — handles HTTP requests and returns API responses.
- `service/` — business logic layer.
- `mapper/` — MyBatis-Plus data access layer.
- `entity/` — database entity classes.
- `model/` — DTO and VO classes.
- `enums/` — module-specific enums.

## 4. Resources Folder

**Path:** `backend/src/main/resources/`

- `application.yml` — main application configuration.
- `application-dev.yml` — development environment configuration.
- `application-prod.yml` — production environment configuration.
- `db/migration/` — database migration scripts.
  - `V1__init.sql` — initial database schema script.
- `static/` — static resources.
- `templates/` — server-side template files.

## 5. Current Directory Layout Summary

```text
backend/
├── src/main/java/edu/xjtlu/cpt202/backend/
│   ├── BackendApplication.java
│   ├── common/
│   └── modules/
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    └── db/migration/V1__init.sql
```



