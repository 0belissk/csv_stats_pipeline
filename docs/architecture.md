# System Architecture

## Architectural Constraints
- Single-page application frontend (Angular)
- Stateless backend (Spring Boot, JWT-ready)
- Layered backend architecture (controller → service → repository)
- RESTful JSON APIs
- Password-based authentication with secure hashing (BCrypt)
- Event-driven processing for CSV workflows (future sprints)
- Infrastructure defined via Terraform (later sprint)

## Authentication Architecture (Current State – Sprint 1)
- Users can register and log in via REST endpoints
- Passwords are stored as BCrypt hashes
- No server-side sessions
- No JWT issuance or validation yet
- Security boundaries are explicitly defined
- System is designed to introduce JWT without refactoring

## Authentication Architecture (Planned – Sprint 2)
- JWT issued on successful login
- JWT validated on protected endpoints
- User identity propagated end-to-end
- Frontend stores and sends JWT on API requests

## Out of Scope for Sprint 1
- JWT issuance and validation
- Angular frontend implementation
- CSV upload
- Data processing pipeline
- AWS infrastructure provisioning

## Authentication & Security (Sprint 2)

- Stateless JWT-based authentication
- Login endpoint (`/auth/login`) issues signed JWTs
- JWT validated on every protected request
- No server-side sessions
- User identity reconstructed per request
- Angular client uses HTTP interceptor to attach JWT
- CORS configured via Spring Security to support browser clients

## CSV Upload & Validation Pipeline (Sprint 3 Increment)

### Overview
Sprint 3 delivers the core CSV ingestion capability using an event-driven architecture. Users upload CSV files via the Angular UI, files are stored in S3, and an AWS Lambda function validates schema + data types before downstream processing (Step Functions orchestration comes next).



### Components

#### Backend (Spring Boot)
- `CsvUpload` entity: tracks upload metadata (userEmail, filename, status, S3 key, validation errors)
- `CsvUploadController`: REST API for upload and status queries
- `CsvUploadService`: handles upload logic and S3 integration
- `S3Service`: abstracts AWS S3 operations

#### AWS Services
| Service | Purpose |
|---------|---------|
| S3 | Store raw CSV uploads |
| Lambda | Validate CSV schema/data, update DB status, launch Step Functions |
| Step Functions | Orchestrate processing workflow (validate now, transform/persist next sprint) |

#### Data Flow
```
User uploads CSV
    → Backend stores file in S3
    → S3 triggers Lambda
    → Lambda validates CSV schema
    → Step Functions orchestrates workflow
    → Status updated in database
    → User polls for status
```

### Upload Status Lifecycle
- `PENDING`: File uploaded, awaiting processing
- `VALIDATING`: `StepFunctionOrchestratorLambda` marks the record after S3 notifications arrive
- `VALIDATED`: Schema validation passed; `UploadStatusLambda` sets the flag after the success branch
- `VALIDATION_FAILED`: Schema/data errors detected or unexpected Lambda failure; serialized `ValidationError` payload persisted by `UploadStatusLambda`

### Step Functions Orchestration (Sprint 3.2)
- `StepFunctionOrchestratorLambda` (S3-triggered) updates status to `VALIDATING` and starts the `csv-processing` Step Functions state machine with `{uploadId, bucket, key}`.
- The state machine definition (`terraform/state_machine/csv_processing.asl.json`) uses `ValidateCsv → PersistPlaceholder/Failure` to coordinate the Lambda steps; infrastructure is provisioned through `aws_sfn_state_machine.csv_processing`.
- `ValidateCsv` synchronously invokes `CsvValidationLambda` with retries for transient errors and stores the response (`valid`, `errors`) under `$.validation` for downstream decisions.
- Success path hits the placeholder `PersistPlaceholder` (reserved for Sprint 4 transformation) and then invokes `UploadStatusLambda` to mark `VALIDATED`.
- Failure path routes either validation issues (`MarkValidationFailed`) or unexpected exceptions (`MarkSystemFailure`) to `UploadStatusLambda`, ensuring descriptive errors reach the `csv_uploads.error_message` column.
- CloudWatch log group `/aws/states/${project}-csv-processing` captures execution traces so every step transition is auditable.

### API Endpoints (Sprint 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/uploads` | Upload a CSV file |
| GET | `/api/uploads` | List user's uploads |
| GET | `/api/uploads/{id}` | Get upload status (status/error populated by Lambda)

### CSV Schema (Sprint 3 Validator)
- Header order: `id`, `name`, `email`, `amount` (case-insensitive match)
- `id`: integer, `name`: non-empty string, `email`: RFC-like regex, `amount`: decimal
- Every row validated column-by-column; errors capture row number, column, and reason

### Lambda collaboration
- `StepFunctionOrchestratorLambda` derives `uploadId` from S3 keys (`uploads/{userEmail}/{uploadId}/{filename}`) and starts the state machine.
- `CsvValidationLambda` performs the schema/data checks and returns `ValidationError` details without touching the database.
- `UploadStatusLambda` owns status persistence (`VALIDATING`, `VALIDATED`, `VALIDATION_FAILED`) using the shared JDBC credentials exposed via environment variables.

### Security Considerations
- All upload endpoints require JWT authentication
- Files are stored with user-scoped S3 keys: `uploads/{userEmail}/{uploadId}/{filename}`
- Users can only access their own uploads



## Out of Scope for Sprint 3
- Data transformation/normalization
- Persisting validated records to PostgreSQL
- EventBridge notifications
- Email notifications via SES
