# Sprint 3 – CSV Upload & Validation Pipeline

## Sprint Planning

### 1. Sprint Goal

Enable users to upload CSV files and automatically validate them through an event-driven processing pipeline.

This sprint introduces the core CSV ingestion capability: users upload a file via the Angular UI, the file is stored in S3, and a Lambda function is triggered to parse and validate the CSV schema. Step Functions will orchestrate the multi-step processing flow. Sprint 3 focuses on upload → validate → status tracking.

---

### 2. Sprint Backlog (Sprint-level)

The following user stories are selected from **Epic 2 (CSV Upload & Storage)** and **Epic 3 (CSV Processing Pipeline)**:

#### Story 2.1: CSV Upload Endpoint
**As a** logged-in user,  
**I want to** upload a CSV file through the application,  
**So that** I can have my data processed and analyzed.

**Tasks:**
- Create `CsvUpload` entity to track upload metadata (userId, filename, status, S3 key, timestamps)
- Create `CsvUploadRepository`
- Create `CsvUploadService` to handle upload logic
- Create `CsvUploadController` with POST `/api/uploads` endpoint
- Integrate with AWS S3 to store uploaded files
- Return upload ID and initial status to client

#### Story 2.2: Upload Status Tracking
**As a** user,  
**I want to** see the processing status of my uploaded CSV,  
**So that** I know when my data is ready or if there was an error.

**Tasks:**
- Add GET `/api/uploads/{id}` endpoint to retrieve upload status
- Add GET `/api/uploads` endpoint to list user's uploads
- Status values: `PENDING`, `VALIDATING`, `VALIDATED`, `VALIDATION_FAILED`

#### Story 3.1: Lambda CSV Parser & Schema Validation
**As a** system,  
**I want to** automatically validate uploaded CSV files against a defined schema,  
**So that** only valid data proceeds to the next processing stage.

**Tasks:**
- Create AWS Lambda function triggered by S3 upload event
- Parse CSV file and validate against expected schema (column headers, data types)
- Update upload status in database via callback or direct DB update
- Capture validation errors for invalid rows

#### Story 3.2: Step Functions Orchestration
**As a** system,  
**I want to** use Step Functions to orchestrate the CSV processing workflow,  
**So that** each processing stage is reliably executed and failures are handled.

**Tasks:**
- Define Step Functions state machine for: Validate → (success/failure branching)
- Integrate Lambda validation function as first step
- Configure error handling and retry policies
- Update upload status at each stage transition

#### Story 2.3: Angular Upload UI
**As a** user,  
**I want to** have a simple upload page in the application,  
**So that** I can easily select and upload my CSV files.

**Tasks:**
- Create Upload component in Angular
- Implement file selection and upload to backend API
- Display upload progress and confirmation
- Show list of user's uploads with status

---

### 3. Acceptance Criteria

#### Story 2.1: CSV Upload Endpoint
- [ ] Authenticated users can POST a CSV file to `/api/uploads`
- [ ] File is stored in S3 with a unique key pattern: `uploads/{userId}/{uploadId}/{filename}`
- [ ] Upload metadata is saved to PostgreSQL (id, userId, filename, s3Key, status, createdAt)
- [ ] Response includes uploadId and status `PENDING`
- [ ] Only CSV files are accepted (content-type validation)
- [ ] Unauthenticated requests return 401

#### Story 2.2: Upload Status Tracking
- [ ] GET `/api/uploads/{id}` returns current status for a specific upload
- [ ] GET `/api/uploads` returns a list of the current user's uploads
- [ ] Users can only see their own uploads (user-scoped data)
- [ ] Response includes: id, filename, status, createdAt, updatedAt

#### Story 3.1: Lambda CSV Parser & Schema Validation
- [ ] Lambda triggers automatically when a file is uploaded to the S3 bucket
- [ ] CSV is parsed and headers are validated against expected schema
- [ ] Data types in each column are validated (e.g., numeric, date, string)
- [ ] Upload status is updated to `VALIDATING` when processing starts
- [ ] Status is updated to `VALIDATED` on success
- [ ] Status is updated to `VALIDATION_FAILED` on schema/type errors
- [ ] Validation errors are captured (row number, column, error type)

#### Story 3.2: Step Functions Orchestration
- [ ] Step Functions state machine is created and deployed
- [ ] Validation Lambda is invoked as the first step
- [ ] Success path transitions to next state (placeholder for Sprint 4)
- [ ] Failure path handles validation errors gracefully
- [ ] State machine execution is traceable and logged

#### Story 2.3: Angular Upload UI
- [ ] Upload page is accessible from the dashboard
- [ ] User can select a CSV file using a file input
- [ ] Upload button sends file to backend API with JWT authentication
- [ ] Success/error feedback is displayed after upload
- [ ] User's upload history is displayed with status indicators

---

### 4. Definition of Done

A story is considered **Done** when:

- [ ] Code is complete and follows project coding standards
- [ ] Unit tests are written and passing (JUnit 5, Jasmine)
- [ ] Integration tests pass (Testcontainers for DB, localstack for AWS)
- [ ] Code has been reviewed (self-review for solo sprint)
- [ ] Acceptance criteria are met and verified
- [ ] Documentation is updated (architecture.md, ADR if applicable)
- [ ] No new linting errors introduced
- [ ] Feature is manually verified end-to-end
- [ ] Code is committed and pushed to the repository

---

## Technical Design Notes

### Backend Components

```
CsvUploadController
    └── CsvUploadService
            ├── S3Service (upload file to S3)
            ├── CsvUploadRepository (persist metadata)
            └── Status updates via direct writes or event callback
```

### AWS Components (Infrastructure)

| Component | Purpose |
|-----------|---------|
| S3 Bucket | Store raw CSV uploads |
| Lambda Function | Validate CSV schema and data |
| Step Functions | Orchestrate processing workflow |
| EventBridge (later) | Emit `ImportCompleted/Failed` events |

### Database Schema Addition

```sql
CREATE TABLE csv_uploads (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    filename VARCHAR(255) NOT NULL,
    s3_key VARCHAR(512) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### API Endpoints (Sprint 3)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/uploads` | Upload a CSV file | Required |
| GET | `/api/uploads` | List user's uploads | Required |
| GET | `/api/uploads/{id}` | Get upload status | Required |

### CSV Schema (Initial MVP)

For Sprint 3, we will validate against a generic schema:
- File must have a header row
- Headers must be non-empty strings
- Data rows must have the same number of columns as the header
- Future sprints will support custom schema definitions

---

## Out of Scope for Sprint 3

- Data transformation/normalization
- Persisting validated records to PostgreSQL (Sprint 4)
- ImportCompleted/Failed events via EventBridge (Sprint 4/6)
- Email notifications (Sprint 6)
- Custom schema definition per upload

---

## Dependencies

- Sprint 1 & 2 must be complete (authentication, user domain)
- AWS credentials configured for S3, Lambda, Step Functions
- LocalStack or AWS account for testing AWS components

---

## Sprint 3 Success Criteria

Sprint 3 is successful when:
1. Users can upload CSV files via the Angular UI
2. Files are stored securely in S3
3. Upload status is trackable in the UI
4. Lambda automatically validates uploaded CSVs
5. Step Functions orchestrates the validation workflow
6. The system correctly distinguishes valid vs. invalid CSVs

---

## Interview-Ready Summary

"In Sprint 3, I implemented CSV file upload with S3 storage and automatic validation using an event-driven architecture. When a user uploads a file, it's stored in S3, which triggers a Lambda function for schema validation. Step Functions orchestrates the workflow, handling success and failure paths. The upload status is tracked end-to-end, allowing users to see when their data is validated and ready for analysis."
