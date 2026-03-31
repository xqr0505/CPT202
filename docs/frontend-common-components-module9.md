# Frontend Common Components Usage Guide —— from module9

QiranXiao

This document describes several reusable Vue components located under `frontend/src/components/common/`.

## 1. BookingStatusTag.vue — Appointment Status Tag

- Purpose
  - Render an appointment status as a colored tag and human-readable text.
  - Common status values: `Pending`, `Confirmed`, `Cancelled`, `Completed`.

- Visual mapping (default)
  - Pending  → Yellow
  - Confirmed → Green
  - Cancelled → Red
  - Completed → Gray

- Props (typical)
  - `status: string` — status code or label to display.
  - `size?: string` — optional size controls (if implemented).

- Notes
  - The component maps known status values to colors and text automatically. For unknown statuses it falls back to a neutral appearance.

- Recommended modules / places to use
  - M7 (Approval list)
  - M8 (Cancellation & reschedule records)
  - M9 (User history and dashboard)

## 2. EmptyPlaceholder.vue — Empty State Placeholder

- Purpose
  - Provide a consistent empty-state UI when lists or searches return no data.
  - Built on top of Element Plus `<el-empty>` and accepts customization options.

- Features
  - Default illustration and message (e.g. “No bookings yet”, “No specialists found”).
  - Optional props for custom illustration, custom message, and an optional action button (e.g. “Create booking”, “Retry search”).

- Props (typical)
  - `message?: string` — override the default text.
  - `image?: string` — override the illustration.
  - `actionLabel?: string` — optional action button label.
  - `onAction?: () => void` — optional callback when action button is clicked.

- Recommended modules / places to use
  - M5 (Search results — when no results found)
  - M7 (Approval list — when there are no pending requests)
  - M9 (Dashboard / history — when no historical records)

- Notes
  - Use this component to keep empty-state UX consistent across the app.

## 3. PaginationTable.vue — Table with Built-in Pagination

- Purpose
  - A generic, paginated table that wraps Element Plus `<el-table>` and `<el-pagination>`.
  - Handles fetching data, loading state and page changes internally when provided an API endpoint or a data fetch function.

- Props / API (typical)
  - `columns: ColumnDefinition[]` — column configuration for `<el-table>`.
  - `fetcher: (params) => Promise<{ list: any[]; total: number }>` **or** `apiUrl: string` — either a data fetch function or an endpoint URL.
  - `pageSize?: number` — default items per page.
  - `initialParams?: Record<string, any>` — any extra query params passed to the fetcher.

- Features
  - Internal management of current page, page size, loading indicator, and total count.
  - Emits events for row selection, sorting, and custom actions if needed.

- Recommended modules / places to use
  - M3 (Specialist account list)
  - M7 (Approval history)
  - M9 (Customer booking history list)

- Notes
  - If your use case needs complex filters or server-side sorting, prefer passing a custom `fetcher` function so the component remains flexible.

## 4. Common Button Component (e.g. CustomButton.vue)

- Purpose
  - A standardized button wrapper (over Element Plus `<el-button>`) with project default styles, optional loading/throttling behavior, and consistent props.

- Recommended modules / places to use
  - All modules — this is the preferred button for any action (submit, confirm, navigate) across the entire frontend codebase.

- Notes
  - Use this button to keep behavior consistent (disabled states, loading spinners, size and color patterns).

---

## Maintenance and Contact

These common components are maintained by QiranXiao.

If you need changes, new props, or notice a bug in any of the components above, please contact me so the change can be made in the shared component instead of copying it into your module. This helps avoid fragmentation and duplicated work.


