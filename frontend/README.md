# Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.0.4.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## CSV Upload Dashboard (Story 2.3)

Story 2.3 introduced a full dashboard + upload workflow. Highlights:

- `http://localhost:4200/login` – Sign in with the same credentials you use against the Spring Boot backend. Successful login redirects to `/dashboard/uploads` and persists the JWT + email locally.
- Dashboard shell – Provides navigation chrome, logout action, and wraps the upload page. Routes are protected by `authGuard` so unauthenticated users are pushed back to `/login`.
- Upload page – Choose a `.csv` file, click **Upload file**, and watch the live progress bar update while the file is posted to `/api/uploads` with your JWT header. After completion you get success/error feedback and the history table refreshes automatically.
- History table – Lists the authenticated user’s uploads with status chips (Pending, Validating, Validated, Validation Failed) and timestamps so it is easy to match UI state to backend processing.
- Refresh button – Allows manual re-fetching of statuses if you are waiting for downstream pipeline events.

To exercise the feature locally:

1. Start the backend at `http://localhost:8080` (the Angular app reads this base URL from `core/api/api.config.ts`).
2. From this directory run `npm start` and browse to `/login`.
3. Upload any small CSV file (extension `.csv` is required). Watch the console/backend logs for `/api/uploads` requests with JWT headers to confirm Story 2.3 acceptance criteria.

Unit tests that cover the login, dashboard shell, upload service, and upload page can be re-run with:

```bash
npm run test -- --watch=false
```

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
