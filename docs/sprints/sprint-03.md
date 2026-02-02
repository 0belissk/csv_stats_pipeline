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
- Create `CsvUpload` entity to track upload metadata (userEmail, filename, status, S3 key, timestamps)
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

#### Story 3.1: Lambda CSV Parser & Schema Validation (Completed)
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
- [ ] File is stored in S3 with a unique key pattern: `uploads/{userEmail}/{uploadId}/{filename}`
- [ ] Upload metadata is saved to PostgreSQL (id, userEmail, filename, s3Key, status, createdAt)
- [ ] Response includes uploadId and status `PENDING`
- [ ] Only CSV files are accepted (content-type validation)
- [ ] Unauthenticated requests return 401

#### Story 2.2: Upload Status Tracking
- [ ] GET `/api/uploads/{id}` returns current status for a specific upload
- [ ] GET `/api/uploads` returns a list of the current user's uploads
- [ ] Users can only see their own uploads (user-scoped data)
- [ ] Response includes: id, filename, status, createdAt, updatedAt

#### Story 3.1: Lambda CSV Parser & Schema Validation
- [x] Lambda triggers automatically when a file is uploaded to the S3 bucket (AWS Lambda → S3 event source backed by the handler in `backend/src/main/java/com/paul/csvpipeline/backend/lambda`)
- [x] CSV is parsed and headers are validated against expected schema
- [x] Data types in each column are validated (integer, email, decimal, date)
- [x] Upload status is updated to `VALIDATING` when processing starts
- [x] Status is updated to `VALIDATED` on success
- [x] Status is updated to `VALIDATION_FAILED` on schema/type errors
- [x] Validation errors are captured (row number, column, error type) and persisted for UI consumption

#### Story 3.2: Step Functions Orchestration
- [x] Step Functions state machine is created and deployed (`aws_sfn_state_machine.csv_processing` + `terraform/state_machine/csv_processing.asl.json`)
- [x] Validation Lambda is invoked as the first step (`ValidateCsv` task calls `CsvValidationLambda` through the `lambda:invoke` integration)
- [x] Success path transitions to next state (placeholder for Sprint 4) via the `PersistPlaceholder` pass state
- [x] Failure path handles validation errors gracefully via the `MarkValidationFailed` and `MarkSystemFailure` branches that call `UploadStatusLambda`
- [x] State machine execution is traceable and logged (CloudWatch log group `/aws/states/${project}-csv-processing` with `ALL` logging)

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
    user_email VARCHAR(255) NOT NULL,
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

## LocalStack End-to-End Setup (Sprint 3)

To prove the pipeline end-to-end without touching AWS, we wired everything into LocalStack. These are the exact steps we followed so future devs can reproduce the sprint-3 run:

1. **Build the Lambda artifact** – from `backend/`, run `./mvnw clean package -DskipTests`. This emits `target/backend-0.0.1-SNAPSHOT-lambda.jar` used for all three Lambda functions.

2. **Deploy/update the Lambdas in LocalStack** – for each function (`csv-validation`, `csv-orchestrator`, `csv-status`):
   ```bash
   aws --endpoint-url=http://localhost:4566 --region us-east-1 lambda update-function-code \
     --function-name <name> \
     --zip-file fileb://backend/target/backend-0.0.1-SNAPSHOT-lambda.jar
   ```
   Then push the LocalStack-friendly environment variables (DB URL/creds, `STATE_MACHINE_ARN`, `SFN_ENDPOINT`, `S3_ENDPOINT`, region, test credentials) using `lambda update-function-configuration --environment file://lambda-env.json`.

3. **Step Functions definition** – edit `csv_processing.asl.json` if needed, then apply it:
   ```bash
   aws --endpoint-url=http://localhost:4566 --region us-east-1 stepfunctions update-state-machine \
     --state-machine-arn arn:aws:states:us-east-1:000000000000:stateMachine:csv-processing \
     --definition file:///Users/<user>/csv_stats_pipeline/csv_processing.asl.json
   ```

4. **S3 notification wiring** – ensure the bucket `csvpipeline-dev-uploads` has the notification shown below (created via Terraform or the CLI):
   ```json
   {
     "LambdaFunctionConfigurations": [
       {
         "Id": "csv-orchestrator-trigger",
         "LambdaFunctionArn": "arn:aws:lambda:us-east-1:000000000000:function:csv-orchestrator",
         "Events": ["s3:ObjectCreated:*"],
         "Filter": {
           "Key": {
             "FilterRules": [
               { "Name": "Prefix", "Value": "uploads/" },
               { "Name": "Suffix", "Value": ".csv" }
             ]
           }
         }
       }
     ]
   }
   ```

5. **Backend & frontend** – run `./mvnw spring-boot:run` (backend, profile `dev` with `DB_URL` env vars) and `pnpm dev` (frontend). Uploading a CSV through the UI now writes to LocalStack S3, fires the notification, and kicks off the Step Functions execution.

6. **Manual trigger (if S3 event needs debugging)** – craft `test-data/orchestrator-event.json` with a full S3 notification payload, then run:
   ```bash
   aws --endpoint-url=http://localhost:4566 --region us-east-1 lambda invoke \
     --cli-binary-format raw-in-base64-out \
     --function-name csv-orchestrator \
     --payload file://test-data/orchestrator-event.json \
     /tmp/orchestrator-response.json
   ```
   This reproduces the S3 trigger to verify Step Functions wiring without re-uploading.

7. **Observability** – tail logs via `aws --endpoint-url=http://localhost:4566 logs tail /aws/lambda/<function> --follow` and inspect Step Functions runs with `aws stepfunctions list-executions ...`. Use `psql` to confirm status transitions in `csv_uploads`.

Documenting these commands ensures anyone can re-run the sprint-3 E2E test locally.



---
