# Frontend Project Structure

This document gives a short overview of the frontend directory layout and what each folder is used for.

## 1. Main Frontend Entry

**Path:** `frontend/src/`

- `main.ts` — application entry point.
- `App.vue` — root Vue component.
- `router/` — route definitions and navigation setup.
- `store/` and `stores/` — Pinia state management.
- `styles/` — global styles and theme variables.
- `views/` — page-level components.
- `layout/` — page layouts.
- `components/` — reusable components.
- `api/` — API request modules.
- `assets/` — static assets such as images and icons.
- `types/` — TypeScript type definitions.
- `utils/` — helper functions.

## 2. API Layer: `api/`

This folder contains request modules grouped by business domain:

- `auth.ts` — authentication APIs.
- `user.ts` — user-related APIs.
- `booking.ts` — booking-related APIs.
- `request.ts` — Axios wrapper and shared request config.

## 3. Components: `components/`

Reusable UI components are placed here:

- `common/` — general-purpose shared components.
- `business/` — feature-related reusable components.

## 4. Layouts: `layout/`

This folder stores page layouts used by different roles or flows:

- `AuthLayout.vue` — login and registration layout.
- `AdminLayout.vue` — admin dashboard layout.
- `DefaultLayout.vue` — default customer-facing layout.

## 5. Styles: `styles/`

This folder manages global styling:

- `variables.scss` — global CSS variables and theme tokens.
- `common.scss` — shared utility classes and base styles.
- `element.scss` — Element Plus style overrides.

## 6. Pages: `views/`

This folder contains page-level views, grouped by module or role:

- `auth/` — authentication pages.
- `customer/` — customer pages.
- `specialist/` — specialist pages.
- `admin/` — admin pages.

## 7. Current Directory Summary

```text
frontend/
├── src/
│   ├── api/
│   ├── assets/
│   ├── components/
│   ├── layout/
│   ├── router/
│   ├── store/
│   ├── stores/
│   ├── styles/
│   ├── types/
│   ├── utils/
│   └── views/
└── README.md
```

## 8. Where to Add New Code

- Put API wrappers in `api/`.
- Put reusable UI parts in `components/`.
- Put full pages in `views/`.
- Put global styles in `styles/`.
- Put shared types in `types/`.
- Put helper functions in `utils/`.

