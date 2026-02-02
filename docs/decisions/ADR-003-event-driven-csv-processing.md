# ADR-003: Event-Driven CSV Processing Architecture

## Context
 Sprint 3 introduces CSV file upload and validation functionality. Users need to upload CSV files, have them validated, and eventually see processed results. Several architectural approaches were considered:



1. **Synchronous processing in Spring Boot**: Process CSV directly in the backend on upload
2. **Message queue (SQS) based**: Use SQS to enqueue and process files
3. **Event-driven with S3 + Lambda + Step Functions**: Store files in S3, trigger Lambda for validation, orchestrate with Step Functions

The system needs to handle:
- Files of varying sizes
- Validation that may take non-trivial time
- Multi-step processing (validate → transform → persist → notify)
- Reliable status tracking for users

## Decision
- Use an **event-driven architecture** with AWS S3, Lambda, and Step Functions
- Store uploaded CSV files in S3 with user-scoped keys: `uploads/{userEmail}/{uploadId}/{filename}`
- Trigger Lambda function automatically on S3 upload events
- Use Step Functions to orchestrate the multi-step processing workflow
- Track upload status in PostgreSQL: `PENDING → VALIDATING → VALIDATED / VALIDATION_FAILED`
- Backend provides REST APIs for upload and status polling

### Processing Flow
```
User uploads CSV → Backend stores in S3 → S3 triggers Lambda
→ Lambda validates schema → Step Functions orchestrates workflow
→ Status updated in DB → User polls for status
```

## Consequences

### Positive
- **Decoupled processing**: Upload returns immediately; validation happens asynchronously
- **Scalable**: Lambda scales automatically with upload volume
- **Reliable**: Step Functions provides built-in retry, error handling, and execution history
- **Observable**: Each step is independently traceable and logged
- **Cost-efficient**: Pay only for actual processing time (Lambda + Step Functions)

### Trade-offs
- **Eventual consistency**: Users must poll for status; no immediate result
- **AWS coupling**: Architecture is tied to AWS-specific services
- **Cold starts**: Lambda may have initial latency on first invocation
- **Infrastructure complexity**: More moving parts to deploy and monitor
- **Testing complexity**: Requires LocalStack or AWS account for integration tests

## Alternatives Considered

### Synchronous Processing in Spring Boot
- **Rejected because**: Blocks HTTP request during processing; doesn't scale for large files; ties up server threads

### SQS-Based Queue Processing
- **Rejected because**: Adds unnecessary indirection; S3 events are sufficient for this use case; Step Functions provides better orchestration than manual queue consumers

## Notes
- EventBridge integration for `ImportCompleted/Failed` events is planned for Sprint 4/6
- Email notifications via SES will be added in Sprint 6
- LocalStack will be used for local development and CI testing of AWS components
- Lambda reads `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from environment variables and updates `csv_uploads.status` directly (`VALIDATING` → `VALIDATED` / `VALIDATION_FAILED`).
- S3 notifications are filtered to the `uploads/` prefix (and `.csv` suffix) to avoid noisy invocations.
- Terraform stack (in `terraform/`) provisions the S3 bucket, IAM role/policy, CloudWatch log group, Lambda function, and S3→Lambda wiring; Terraform consumes a pre-built JAR artifact and never compiles code.

### Local E2E Setup Checklist (Sprint 3)
1. **LocalStack** – start it with S3, Lambda, Step Functions, and CloudWatch Logs enabled.
2. **Build Lambda jar** – `cd backend && ./mvnw clean package -DskipTests`.
3. **Update lambdas** – for each of `csv-orchestrator`, `csv-validation`, and `csv-status`:
   ```bash
   aws --endpoint-url=http://localhost:4566 --region us-east-1 lambda update-function-code \
     --function-name <name> \
     --zip-file fileb://backend/target/backend-0.0.1-SNAPSHOT-lambda.jar
   aws --endpoint-url=http://localhost:4566 --region us-east-1 lambda update-function-configuration \
     --function-name <name> \
     --environment file://lambda-env.json
   ```
   (`lambda-env.json` carries DB creds plus `STATE_MACHINE_ARN`, `SFN_ENDPOINT`, `S3_ENDPOINT`, region, `AWS_ACCESS_KEY_ID/SECRET`.)
4. **Apply state machine** – `aws --endpoint-url=http://localhost:4566 --region us-east-1 stepfunctions update-state-machine --state-machine-arn arn:aws:states:us-east-1:000000000000:stateMachine:csv-processing --definition file://csv_processing.asl.json`.
5. **Verify S3 notification** – run `aws ... s3api get-bucket-notification-configuration --bucket csvpipeline-dev-uploads` and confirm the prefix/suffix filter targets the orchestrator.
6. **Run services** – start Spring Boot (`SPRING_PROFILES_ACTIVE=dev` with Postgres env vars) and the frontend (`pnpm dev`). Uploading a CSV now exercises the full event-driven path locally.
7. **Manual test payload (optional)** – to trigger the orchestrator without re-uploading, use `test-data/orchestrator-event.json` and `aws ... lambda invoke --cli-binary-format raw-in-base64-out ...` as documented in sprint-03 notes.
8. **Observe** – tail logs via `aws logs tail /aws/lambda/<function>` and inspect executions with `aws stepfunctions list-executions`. Use `psql` to confirm status transitions in `csv_uploads`.

These steps give anyone a reproducible, purely local way to validate the event-driven architecture described in this ADR.
