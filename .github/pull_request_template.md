## Overview
<!-- Briefly describe the purpose of this PR. Which PBI or Task does it solve? -->
- **PBI Reference:** [e.g., Module 9 - PBI-2: Booking History List]
- **Task Type:** `feat` / `fix` / `refactor` / `test`

## Changes Logic
<!-- Bullet points of what you implemented/changed -->
- Implemented `XXXService` for ...
- Created `XXXController` with endpoints: `GET /api/v1/...`
- Added Frontend component `XXX.vue` with validation logic.
- Applied `BookingStatusEnum` to replace magic numbers.

## Testing Evidence
<!-- Provide proof that your code works. NO SCREENSHOT, NO MERGE. -->

### 1. Backend: JUnit Unit Tests
- [ ] Core logic covered by Unit Tests in `src/test/java/...`
- **Screenshot of JUnit Result:** 
> [Paste screenshot here: Showing green checkmarks for all tests]

### 2. Backend: Swagger Interface Test
- [ ] API successfully tested via Swagger UI (`/doc.html` or `/swagger-ui/`)
- **Screenshot of Swagger Response:**
> [Paste screenshot here: Showing a successful 200 OK response with data]

### 3. Frontend: Component & UI Test
- [ ] Component rendered correctly without Console errors.
- [ ] Data interaction with Backend is successful.
- **Screenshot of UI/Network Tab:**
> [Paste screenshot here: Showing the page and the successful Network request]

## Definition of Done (DoD) Checklist
- [ ] **No Magic Values:** Used Constants/Enums for all status codes and roles.
- [ ] **Code Structure:** Followed `Service/Impl` and `DTO/VO` separation.
- [ ] **Atomic Commits:** This PR contains small, logical commits (not one giant "done" commit).
- [ ] **TypeScript:** No `any` types used in Frontend; interfaces are defined.
- [ ] **Documentation:** Swagger `@Operation` annotations are added to Controller methods.

## Additional Notes
<!-- Anything else the reviewer should know? -->
