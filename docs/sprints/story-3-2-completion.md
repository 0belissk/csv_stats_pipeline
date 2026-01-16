# Story 3.2 – Step Functions Orchestration

This log captures the Story 3.2 work in a feature-first, layer-based breakdown.

## Feature: Step Functions CSV Workflow

### Handler Layer
1. **`CsvValidationLambda` response contract** – now accepts a typed `ValidationRequest` (`uploadId`, `bucket`, `key`) and returns `ValidationResponse` with `valid`, `errorCount`, and trimmed `ValidationError` details so Step Functions can branch without re-reading the DB.
2. **`StepFunctionOrchestratorLambda`** – new S3-triggered handler that parses the upload key, marks the record `VALIDATING`, and starts the state machine with a serialized `{uploadId, bucket, key}` payload while logging execution names.
3. **`UploadStatusLambda`** – new handler dedicated to persisting status transitions (`VALIDATING`, `VALIDATED`, `VALIDATION_FAILED`) via the existing `UploadStatusRepository`, ensuring Step Functions owns success/failure transitions.

### Orchestration Layer
1. **State machine definition** – `terraform/state_machine/csv_processing.asl.json` models `ValidateCsv → PersistPlaceholder/Failure`. `ValidateCsv` uses `lambda:invoke`, retries transient Lambda errors, and stores the response under `$.validation` for downstream choices.
2. **Success branch** – `PersistPlaceholder` is the Sprint 4 hook, followed by `MarkValidated` (calls `UploadStatusLambda`) and a `Succeed` terminator.
3. **Failure branches** – `MarkValidationFailed` handles schema/data issues with the validator’s `errors`, while `MarkSystemFailure` catches thrown exceptions and synthesizes a `file` error before ending in `Fail`.
4. **Observability** – executions log to `/aws/states/${project}-csv-processing` with `ALL` data so every transition is traceable.

### Infrastructure Layer
1. **Terraform wiring** – `aws_sfn_state_machine.csv_processing`, IAM service role, and dedicated CloudWatch log group provision the Step Functions side.
2. **Lambda fleet** – new `aws_lambda_function.csv_orchestrator` and `aws_lambda_function.upload_status` share the Java artifact with distinct IAM roles, log groups, and VPC settings. The existing validator Lambda now receives work only via the state machine.
3. **S3 notifications** – bucket notifications invoke the orchestrator Lambda (not the validator) and pass through the configured CSV prefix/suffix filters.
4. **Outputs** – state machine/Lambda ARNs are exported for CI/CD visibility.

### Testing Layer
1. **`CsvValidationLambdaIT`** – validates that S3 reads succeed/fail as expected with the new request/response signature.
2. **`UploadStatusLambdaIT`** – exercises `VALIDATING`, `VALIDATED`, and `VALIDATION_FAILED` transitions against Testcontainers Postgres, confirming DB persistence and error payloads.
3. **`StepFunctionOrchestratorLambdaTest`** – mocks the SFN client to ensure `markValidating` is invoked and the `StartExecution` input contains the right bucket/key/uploadId tuple.

### Documentation Layer
1. **`docs/sprints/sprint-03.md`** – acceptance criteria for Story 3.2 marked complete with links to the Terraform definition and logging details.
2. **`docs/architecture.md`** – updated the CSV pipeline section to describe the new Step Functions orchestration, lifecycle statuses, and logging surface.
