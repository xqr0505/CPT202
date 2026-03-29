# How to Use the `common` Package

## 1. Standard API Responses (`Result` & `PageResult`)
**Path:** `common/result/*`

**Rule:** EVERY method in your `Controller` MUST return a `Result<T>` object. Do not return raw Entities, Strings, or Maps.

### Single Object or List
```java
// [Good Practice]
@GetMapping("/{id}")
public Result<UserVO> getUser(@PathVariable Long id) {
    UserVO user = userService.getUserById(id);
    return Result.success(user); // Returns standard JSON with code 200
}
```

### Pagination (For Search/History Modules)
When returning paginated data, wrap your data in `PageResult<T>`.
```java
// [Good Practice]
@PostMapping("/search")
public Result<PageResult<BookingVO>> searchBookings(@RequestBody SearchDTO dto) {
    PageResult<BookingVO> page = bookingService.search(dto);
    return Result.success(page);
}
```

---

## 2. Exception Handling (`BusinessException`)
**Path:** `common/exception/*`

**Rule:** NEVER use `try-catch` to return an error message manually in the Controller. NEVER return `Result.error()` manually if something goes wrong in the Service layer. Instead, **throw a `BusinessException`**. The `GlobalExceptionHandler` will automatically catch it and format it into a standard JSON response.

```java
// [Bad Practice]
public Result<String> cancelBooking(Long id) {
    if (id == null) return Result.error("ID cannot be null");
}

// [Good Practice] (In your Service layer)
public void cancelBooking(Long id) {
    if (id == null) {
        // Find the appropriate Error Code in ResultCodeEnum
        throw new BusinessException(ResultCodeEnum.PARAM_ERROR);
    }
    // Business logic...
}
```

---

## 3. Input Validation (`@Validated`)
**Path:** `common/validation/ValidationGroups`

**Rule:** Do not write `if (dto.getName() == null)` in your code. Use standard `jakarta.validation` annotations inside your DTOs (`@NotNull`, `@NotBlank`, `@Email`, etc.).

We use `ValidationGroups` to distinguish between Create and Update actions:
*   `Create.class`: Usually requires all fields (e.g., password cannot be null).
*   `Update.class`: Usually requires the ID, but other fields can be optional.

**Step 1: Define DTO**
```java
public class UserDTO {
    // ID is only required when updating
    @NotNull(groups = ValidationGroups.Update.class, message = "ID is required for update")
    private Long id;

    // Name is required when creating
    @NotBlank(groups = ValidationGroups.Create.class, message = "Name cannot be empty")
    private String name;
}
```

**Step 2: Trigger Validation in Controller**
```java
@PostMapping("/create")
public Result<Void> createUser(@Validated(ValidationGroups.Create.class) @RequestBody UserDTO dto) {
    userService.create(dto);
    return Result.success();
}
```

---

## 4. Object Mapping (Entity ↔ DTO ↔ VO)
**Path:** `common/utils/BeanCopyUtils`

**Rule:**
*   **DTO (Data Transfer Object):** Used to receive parameters from the frontend.
*   **Entity:** Used to map to the database table.
*   **VO (View Object):** Used to return data to the frontend (hide sensitive fields like passwords).

Do not manually use `entity.setName(dto.getName())`. Use `BeanCopyUtils`.

```java
// Convert single object
UserVO vo = BeanCopyUtils.copyBean(userEntity, UserVO.class);

// Convert a list of objects
List<UserVO> voList = BeanCopyUtils.copyBeanList(entityList, UserVO.class);
```

---

## 5. Security & Current User (`SecurityUtils`)
**Path:** `common/utils/SecurityUtils` (and `common/security/*`)

**Rule:** You no longer need to parse HttpServletRequest to find out who is calling your API. The system automatically verifies the JWT Token.

### How to get the logged-in user's ID
In your Service layer, simply call:
```java
// [Good Practice]
public void createBooking(BookingDTO dto) {
    Long currentUserId = SecurityUtils.getCurrentUserId();
    // Save currentUserId into the booking entity...
}
```

### How to restrict API access by Role
Use Spring Security's `@PreAuthorize` annotation directly on your Controller methods.
```java
// Only ADMIN and SPECIALIST can access this API
@PreAuthorize("hasAnyRole('ADMIN', 'SPECIALIST')")
@GetMapping("/specialist/schedule")
public Result<ScheduleVO> getSchedule() {  }
```

> **// TODO: [Module 1] Please complete the documentation below once implemented:**
>
> ### Security Infrastructure Details (For internal reference)
> *   **JWT Generation & Parsing:** Detailed logic is implemented in `common/utils/JwtUtils.java`. (Add note on expiration time and secret key location).
> *   **Authentication Filter:** `common/security/JwtAuthenticationFilter.java` is responsible for intercepting requests, extracting the token from the `Authorization` header, validating it, and injecting the user details into the `SecurityContextHolder`.
> *   **Security Configuration:** `common/config/SecurityConfig.java` defines public endpoints (e.g., Swagger, `/auth/login`) and configures the password encoder (`BCrypt`).
> *   **Exception Handlers:** `EntryPoint` and `AccessDeniedHandler` are configured to catch 401/403 errors and return our standard `Result` JSON format.

---

## 6. Database Auto-Fill (MyBatis-Plus)
**Path:** `common/handler/MyMetaObjectHandler`

**Rule:** **DO NOT** manually set `createdAt` or `updatedAt` in your code

The system is configured to automatically intercept `INSERT` and `UPDATE` SQL statements.
*   When you call `mapper.insert(entity)`, `created_at` and `updated_at` are automatically filled with the current time.
*   When you call `mapper.updateById(entity)`, `updated_at` is automatically updated.

---

## 7. Preventing Duplicate Submissions (`@Idempotent`)
**Path:** `common/annotation/Idempotent`

**Rule:** For critical actions (like booking an appointment or creating a schedule), users might double-click the submit button, causing duplicate database records.

Simply add the `@Idempotent` annotation to your Controller method. The AOP aspect (`IdempotentAspect`) will automatically block the second request if it arrives within a short timeframe.

```java
@Idempotent
@PostMapping("/book")
public Result<Void> bookAppointment(@RequestBody BookingDTO dto) {
    bookingService.book(dto);
    return Result.success();
}
```

---

## 8. Time Utilities (`DateTimeUtils`)
**Path:** `common/utils/DateTimeUtils`

For Modules 4, 5, 6, 7, 8, 9 (Scheduling & Booking), time conflict calculation is extremely common. Please use the unified utility methods to avoid time zone issues or calculation bugs.

```java
LocalDateTime start1 = LocalDateTime.of(2024, 3, 1, 10, 0);
LocalDateTime end1 = LocalDateTime.of(2024, 3, 1, 12, 0);
LocalDateTime start2 = LocalDateTime.of(2024, 3, 1, 11, 0);
LocalDateTime end2 = LocalDateTime.of(2024, 3, 1, 13, 0);

// Check if two time slots overlap (Crucial for Booking!)
boolean isOverlap = DateTimeUtils.isOverlap(start1, end1, start2, end2);

```
