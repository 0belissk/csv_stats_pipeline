# Story 2.3 – Angular Upload UI

Detailed implementation log for Story 2.3 showing how the Angular upload experience was delivered feature-first and layer-by-layer.

## Feature: Authenticated Dashboard Shell
1. **Standalone routing + guards** – Replaced the single-component shell with Angular Router routes (`/login`, `/dashboard/uploads`) plus `authGuard`/`authChildGuard` so the upload page is only reachable with a JWT.
2. **Dashboard component** – Added a reusable dashboard wrapper with header, logout action, and scoped content slot so the upload page is “accessible from the dashboard” while keeping layout concerns isolated.
3. **Global providers** – Centralized router + HTTP interceptor registration in `app.config.ts` so every feature automatically benefits from JWT attachment and navigation without wiring duplication.

## Feature: Auth + Login Refresh
1. **Core auth service** – Moved auth logic into `core/auth`, storing both JWT and email in reactive signals so downstream components can react to login/logout events.
2. **Login UI** – Rebuilt `LoginComponent` with form validation, loading states, error feedback, and automatic redirect to the dashboard when authentication already exists.
3. **Auth interceptor tests** – Added targeted specs proving that JWT headers are attached/omitted correctly, preventing regressions as more HTTP services are added.

## Feature: CSV Upload Experience
### Service Layer
1. **`UploadService`** – New service that wraps `/api/uploads` for both `GET` history and multipart uploads with `HttpClient.request` progress events.
2. **Models** – Created `CsvUploadRecord`/`CsvUploadStatus` types so templates and tests have strongly-typed access to backend responses.

### Component Layer
1. **Upload page component** – Implements file selection, upload button, live progress bar, success/error feedback, and history refresh button.
2. **History table + status pills** – Renders the user’s upload history with human-friendly labels (`Pending`, `Validated`, etc.) and color-coded pills per acceptance criteria.
3. **UX niceties** – Resetting the file input after successful uploads, disabling actions while requests are in-flight, and exposing refresh controls so users can re-fetch statuses on demand.

### Integration Layer
1. **Dashboard route wiring** – The upload page is a child route of the dashboard, ensuring deep links like `/dashboard/uploads` respect auth guards while inheriting the dashboard layout.
2. **JWT propagation** – Upload requests automatically carry the JWT via the interceptor, satisfying the “Upload button sends file to backend API with JWT authentication” requirement.

## Testing & Verification
1. **Unit specs** – Added Vitest suites for the login component, upload component, upload service, auth service, auth interceptor, and root app shell to cover the new behavior.
2. **Command** – `cd frontend && npm run test -- --watch=false` runs the automated suite; it was executed successfully as part of this story.

## Documentation & Structure
1. **Feature-based layout** – Frontend code now follows `features/<feature>/<layer>` and `core/<capability>` conventions per project guidelines.
2. **Story log** – This file plus the updated frontend README explain how to exercise the upload UI and why the architectural choices were made.
