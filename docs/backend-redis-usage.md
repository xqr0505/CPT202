# Redis Usage in the Backend (common package)

This document provides a clear, logical overview of how Redis is configured and used in the backend, focusing on configuration, key conventions, and usage patterns.

---

## 1. Redis Configuration & Templates

**Location:** `common/config/RedisConfig.java`

- Provides two main `RedisTemplate` beans:
  - `redisTemplate` — for simple String key-value pairs
  - `jsonRedisTemplate` — for storing/retrieving complex objects as JSON
- Uses a custom `ObjectMapper` for JSON serialization (supports Java 8 date/time, disables timestamps, enables polymorphic types)

### 1.1. String RedisTemplate
```java
@Autowired
private RedisTemplate<String, String> redisTemplate;
redisTemplate.opsForValue().set("key", "value");
String value = redisTemplate.opsForValue().get("key");
```

### 1.2. JSON RedisTemplate
```java
@Autowired
@Qualifier("jsonRedisTemplate")
private RedisTemplate<String, Object> jsonRedisTemplate;
User user = new User("alice", 20);
jsonRedisTemplate.opsForValue().set("user:alice", user);
User cached = (User) jsonRedisTemplate.opsForValue().get("user:alice");
```

---

## 2. Redis Key Naming & Utilities

### 2.1. Key Constants
**Location:** `common/constant/RedisCacheConstant.java`

- Defines all key namespace prefixes and TTLs
- Examples:
  - `KEY_SEPARATOR` (usually `:`)
  - `BOOKING_CACHE_PREFIX`, `CUSTOMER_CACHE_PREFIX`, etc.
  - `BOOKING_LIST_CACHE_TTL_SECONDS`, `BOOKING_DETAIL_CACHE_TTL_SECONDS`

```java
String key = RedisCacheConstant.BOOKING_CACHE_PREFIX + RedisCacheConstant.KEY_SEPARATOR + "123";
long ttl = RedisCacheConstant.BOOKING_DETAIL_CACHE_TTL_SECONDS;
```

### 2.2. Key Builder Utilities
**Location:** `common/utils/RedisKeyUtils.java`

- Provides static methods to build standardized keys:
  - `buildCustomerBookingListKey(Long customerId, Integer pageNo, Integer pageSize, String tab, String status)`
  - `buildCustomerBookingDetailKey(Long customerId, Long bookingId)`
  - `buildCustomerBookingKeyPattern(Long customerId)`

```java
String listKey = RedisKeyUtils.buildCustomerBookingListKey(1L, 1, 10, "active", "confirmed");
// "booking:customer:1:list:1:10:active:confirmed"
String detailKey = RedisKeyUtils.buildCustomerBookingDetailKey(1L, 123L);
// "booking:customer:1:detail:123"
String pattern = RedisKeyUtils.buildCustomerBookingKeyPattern(1L);
// "booking:customer:1:*"
```

---

## 3. Usage Examples

### 3.1. Caching a DTO
```java
BookingDTO booking = ...;
jsonRedisTemplate.opsForValue().set("booking:123", booking);
BookingDTO cached = (BookingDTO) jsonRedisTemplate.opsForValue().get("booking:123");
```

### 3.2. Setting Expiry
```java
redisTemplate.expire("some:key", RedisCacheConstant.BOOKING_LIST_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
```

---

## 4. Best Practices & Notes
- Always use the provided key constants and utility methods for key construction.
- Use `redisTemplate` for simple string values, `jsonRedisTemplate` for objects.
- Both templates are auto-configured and can be injected anywhere in the backend.
- For advanced Redis features (pub/sub, transactions, etc.), refer to Spring Data Redis documentation.

---

For more details, see the source code in the `common` package.
