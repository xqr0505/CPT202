# Redis Usage in the Backend (common package)

This document explains how Redis is configured and used in the backend, especially with the new configuration in `common/config/RedisConfig.java`.

## 1. Redis Configuration Overview

The backend uses Spring Data Redis for caching and distributed data operations. The configuration is centralized in:

- `backend/src/main/java/edu/xjtlu/cpt202/backend/common/config/RedisConfig.java`

This configuration provides two main `RedisTemplate` beans:

- `redisTemplate` (String key, String value)
- `jsonRedisTemplate` (String key, Object value, JSON serialization)

### Key Points
- **String RedisTemplate**: For simple string key-value operations.
- **JSON RedisTemplate**: For storing complex objects as JSON, with type information preserved.
- **Custom ObjectMapper**: Ensures compatibility with Java 8 date/time types and disables timestamp serialization.

## 2. Bean Details & Usage

### 2.1. String RedisTemplate

```java
@Autowired
private RedisTemplate<String, String> redisTemplate;

// Example usage
redisTemplate.opsForValue().set("key", "value");
String value = redisTemplate.opsForValue().get("key");
```

### 2.2. JSON RedisTemplate

```java
@Autowired
@Qualifier("jsonRedisTemplate")
private RedisTemplate<String, Object> jsonRedisTemplate;

// Example usage
User user = new User("alice", 20);
jsonRedisTemplate.opsForValue().set("user:alice", user);
User cached = (User) jsonRedisTemplate.opsForValue().get("user:alice");
```

- The `jsonRedisTemplate` uses Jackson for serialization, supporting polymorphic types and Java 8 date/time.
- Type information is included in the JSON, so you can cache and retrieve any object.

## 3. Customization Details

- **Jackson2JsonRedisSerializer** is configured with a custom `ObjectMapper` that:
  - Enables default typing for non-final classes (for polymorphic deserialization)
  - Registers `JavaTimeModule` for Java 8 date/time support
  - Disables writing dates as timestamps

## 4. When to Use Which Template?
- Use `redisTemplate` for simple string values (counters, tokens, etc).
- Use `jsonRedisTemplate` for caching objects, lists, or maps.

## 5. Example: Caching a DTO

```java
// Save a DTO
BookingDTO booking = ...;
jsonRedisTemplate.opsForValue().set("booking:123", booking);

// Retrieve
BookingDTO cached = (BookingDTO) jsonRedisTemplate.opsForValue().get("booking:123");
```

## 6. Notes
- Both templates are auto-configured and can be injected anywhere in the backend.
- For advanced Redis features (pub/sub, transactions, etc.), refer to Spring Data Redis documentation.

---

For more details, see the source code in `common/config/RedisConfig.java`.
