# Common Package Overview

Path:

`backend/src/main/java/edu/xjtlu/cpt202/backend/common`


## Package Structure

### `annotation`
Custom annotations used across the project.

- `Idempotent`: Marks an API or method that should prevent duplicate submissions.

---

### `aspect`
AOP (Aspect-Oriented Programming) logic for cross-cutting concerns.

- `IdempotentAspect`: Intercepts methods marked by `@Idempotent` and enforces idempotency rules.

---

### `config`
Global Spring Boot configuration classes.

- `CorsConfig`: Configures cross-origin requests.
- `JacksonConfig`: Configures JSON serialization/deserialization behavior.
- `MybatisPlusConfig`: Configures MyBatis-Plus features (such as pagination/interceptors).
- `SwaggerConfig`: Configures API documentation related behavior.
- `SecurityConfig`: Configures Spring Security's authentication, authorization, and access control policies.

---

### `constant`
Shared constant values used in different places.

- `CommonConstant`: Stores reusable constants (for example: fixed strings, flags, or global keys).

---

### `context`
Holds request-level context data.

- `UserContextHolder`: Stores and fetches current user info (typically via `ThreadLocal`) during one request lifecycle.

---

### `enums`
Common enum contracts and enum-related definitions.

- `BaseEnum<T>`: Defines standard enum methods:
    - `getCode()`
    - `getDesc()`

This helps unify enum behavior and can be integrated with ORM/database mapping and API docs.

---

### `exception`
Centralized business and global exception handling.

- `BusinessException`: Custom runtime exception for business errors.
- `GlobalExceptionHandler`: Handles exceptions globally and returns unified response bodies.

---

### `handler`
Framework handlers for automatic behavior.

- `MyMetaObjectHandler`: MyBatis-Plus auto-fill handler.
    - On insert: fills `createdAt` and `updatedAt`
    - On update: fills `updatedAt`

---

### `properties`
Configuration binding classes for custom `application.yml` properties.

- `CommonProperties`: Binds custom prefix config (for example JWT and OSS settings), so values can be injected safely in code.

---

### `result`
Unified API response model.

- `Result<T>`: Standard response wrapper (code/message/data style).
- `ResultCodeEnum`: Centralized response status codes and messages.
- `PageResult`: Pagination result model, contains total count and list of records for current page, used when returning paginated data.

---

### `utils`
Reusable utility classes.

- `BeanCopyUtils`: Simplifies object copy and list copy (DTO/Entity/VO transformations).
- `DateTimeUtils`: Time-related helpers, such as checking whether two time ranges overlap.

---

### `validation`
Validation grouping definitions.

- `ValidationGroups`:
    - `Create`
    - `Update`

Used with `@Validated(...)` to apply different validation rules in create vs update scenarios.

