# System Architecture

## Architectural Constraints
- Single-page application frontend (Angular)
- Stateless backend (Spring Boot, JWT-ready)
- Layered backend architecture (controller → service → repository)
- RESTful JSON APIs
- Password-based authentication with secure hashing (BCrypt)
- Event-driven processing for CSV workflows (Sprint 3+)
- Infrastructure defined via Terraform (later sprint)

## Authentication Architecture (Current State – Sprint 2)
- Users can register and log in via REST endpoints
- Passwords are stored as BCrypt hashes
- JWT issued on successful login
- JWT validated on protected endpoints
- User identity propagated end-to-end
- No server-side sessions
- Security boundaries are explicitly defined

## Authentication & Security (Sprint 2)

- Stateless JWT-based authentication
- Login endpoint (`/auth/login`) issues signed JWTs
- JWT validated on every protected request
- No server-side sessions
- User identity reconstructed per request
- Angular client uses HTTP interceptor to attach JWT
- CORS configured via Spring Security to support browser clients

## CSV Upload & Validation Pipeline (Planned – Sprint 3)

### Overview
Sprint 3 introduces the core CSV ingestion capability using an event-driven architecture. Users upload CSV files via the Angular UI, files are stored in S3, and AWS Lambda + Step Functions process and validate the data.

### Components

#### Backend (Spring Boot)
- `CsvUpload` entity: tracks upload metadata (userId, filename, status, S3 key)
- `CsvUploadController`: REST API for upload and status queries
- `CsvUploadService`: handles upload logic and S3 integration
- `S3Service`: abstracts AWS S3 operations

#### AWS Services
| Service | Purpose |
|---------|---------|
| S3 | Store raw CSV uploads |
| Lambda | Validate CSV schema and data |
| Step Functions | Orchestrate processing workflow |

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
- `VALIDATION_FAILED`: Schema or data type errors detected

### API Endpoints (Sprint 3)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/uploads` | Upload a CSV file |
| GET | `/api/uploads` | List user's uploads |
| GET | `/api/uploads/{id}` | Get upload status |

### Security Considerations
- All upload endpoints require JWT authentication
- Files are stored with user-scoped S3 keys: `uploads/{userId}/{uploadId}/{filename}`
- Users can only access their own uploads

## Out of Scope for Sprint 3
- Data transformation/normalization
- Persisting validated records to PostgreSQL
- EventBridge notifications
- Email notifications via SES

