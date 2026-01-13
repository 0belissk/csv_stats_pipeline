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
| Lambda | Validate CSV schema/data, update DB status |
| Step Functions | Orchestrate processing workflow (future increment) |

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
- `VALIDATING`: Lambda is parsing and validating
- `VALIDATED`: Schema validation passed
- `VALIDATION_FAILED`: Schema or data type errors detected; JSON payload persisted to `csv_uploads.error_message`

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
- Lambda derives uploadId from S3 keys: `uploads/{userEmail}/{uploadId}/{filename}`
- Status is set to `VALIDATING` when parse begins, `VALIDATED` on success, `VALIDATION_FAILED` with JSON error array otherwise
- Updates happen via direct JDBC access to PostgreSQL using credentials stored in Lambda environment variables

### Security Considerations
- All upload endpoints require JWT authentication
- Files are stored with user-scoped S3 keys: `uploads/{userEmail}/{uploadId}/{filename}`
- Users can only access their own uploads



## Out of Scope for Sprint 3
- Data transformation/normalization
- Persisting validated records to PostgreSQL
- EventBridge notifications
- Email notifications via SES
