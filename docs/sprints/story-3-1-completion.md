# Story 3.1 – Lambda CSV Parser & Schema Validation

This log captures the step-by-step implementation for Story 3.1, organized feature-first and layer-by-layer.

## Feature: Event-driven CSV Validation

### Handler Layer
1. **Create backend Lambda package (`backend/src/main/java/com/paul/csvpipeline/backend/lambda`)** – colocated with the Spring Boot codebase but framework-free so it can still be packaged as a Lambda handler artifact.
2. **Implement `CsvValidationLambda` handler** – listens to S3 events, parses the S3 key to discover `uploadId`, streams the CSV from S3, and orchestrates validation + database updates.
3. **Wire AWS + infra dependencies** – instantiate AWS SDK v2 S3 client and load JDBC credentials from environment variables so the function can run locally (LocalStack) and in AWS.

### Parser Layer
1. **Add `S3KeyParser`** – enforces the `uploads/{userEmail}/{uploadId}/{filename}` convention so downstream code can reliably find the upload record.
2. **Unit tests** – `S3KeyParserTest` covers happy-path parsing and failure scenarios for malformed keys.

### Validation Layer
1. **Model schema definitions** – `ColumnDefinition` + `ColumnType` describe `id (INTEGER)`, `name (STRING)`, `email (EMAIL)`, `amount (DECIMAL)`, with optional `DATE` type support for future schemas.
2. **Implement `CsvValidator`** – uses Apache Commons CSV to enforce header order, then validates each row and column with typed rules (integer, decimal, email, ISO date) and captures `ValidationError` objects (row, column, message).
3. **Return structured results** – `CsvValidationResult` communicates success/failure and a collection of detailed errors for persistence.
4. **Unit tests** – `CsvValidatorTest` verifies success cases, header mismatches, per-column rule enforcement, and the new DATE validation branch.

### Persistence Layer
1. **Create `UploadStatusRepository`** – thin JDBC wrapper that updates the `csv_uploads` table statuses and `error_message` column using env-provided credentials.
2. **Status transitions** – `markValidating`, `markValidated`, `markFailed` align with `CsvUploadStatus` semantics already used in the Spring service.
3. **Error payload serialization** – first 25 validation errors are serialized into a JSON array so UI/diagnostics can surface row/column/message triples.

### Error Handling & Observability
1. **Structured logging** – `CsvValidationLambda` logs each transition (start/success/failure) with `uploadId`, bucket, and key for root-cause tracing.
2. **Failure safety** – IO/read errors short-circuit to `VALIDATION_FAILED` with a synthetic `file` validation error, keeping the pipeline consistent even if S3 is unavailable.

### Integration Test
- `CsvValidationLambdaIT` boots the Spring context with Testcontainers (Postgres + LocalStack), seeds `csv_uploads`, uploads CSV files into LocalStack, invokes the Lambda via an `S3Event`, and asserts DB status transitions (`VALIDATED` vs `VALIDATION_FAILED`).

### Documentation & Traceability
1. **Architecture doc** – `docs/architecture.md` now reflects the implemented lambda, schema details, and DB collaboration contract.
2. **Sprint log** – `docs/sprints/sprint-03.md` has Story 3.1 acceptance criteria checked off with references to the new components.

### Testing & Verification Steps
1. `cd backend && ./mvnw test -Dtest="*Lambda*"` (or run the full suite) – executes the validator, parser, and integration suites (requires network to download dependencies).
2. Deploy Lambda (via Terraform) with env vars `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`; configure S3 trigger on the uploads bucket.
3. Upload CSV via frontend/backend – file is stored in S3, Lambda fires, statuses move `PENDING → VALIDATING → VALIDATED/VALIDATION_FAILED`, and details surface via `/api/uploads` endpoints.

This layered breakdown can be pasted into internal docs or PR descriptions to explain how Story 3.1 was delivered end-to-end.
