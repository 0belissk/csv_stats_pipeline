# CSV Validation Terraform Stack

“Write Terraform now, apply later.” This directory contains a full IaC definition for the Sprint 3 CSV upload pipeline, ready for review but not yet applied.

## Resources provisioned
- S3 bucket for CSV uploads (versioned, SSE-enabled, public access blocked)
- bucket notification filtered to `uploads/` prefix + `.csv` suffix
- Lambda execution role + inline policy (S3 read + CloudWatch logging)
- CloudWatch log group with retention controls
- Java 17 Lambda function wired to the Spring-based handler
- Lambda permission + S3 notification wiring

## Variables & contracts
Key inputs are defined in `variables.tf`:

| Variable | Purpose |
|----------|---------|
| `aws_region` | Deployment region |
| `project_name` | Applied to tags |
| `s3_bucket_name` | Must be globally unique |
| `lambda_function_name` | Name/identifier for the validator |
| `lambda_artifact_path` | Path to the pre-built JAR (Terraform never compiles code) |
| `db_url`, `db_username`, `db_password` | Injected into Lambda env vars |
| `lambda_timeout`, `lambda_memory_size` | Runtime tuning |
| `log_retention_days` | CloudWatch retention |
| `s3_notification_prefix/suffix` | Filters so only CSV uploads trigger the function |
| `lambda_subnet_ids`, `lambda_security_group_ids` | Optional VPC wiring for future private RDS |

Artifact contract: `lambda_artifact_path` should reference the jar produced by your build, e.g. `../lambda/target/csv-validation-lambda.jar`. Ensure CI builds the artifact before running Terraform.

## Usage (plan later)
```
cd terraform
terraform init
terraform fmt -recursive
terraform validate
# terraform plan/apply ==> wait until Sprint 3 E2E tests pass
```

State: stored locally for now; migrate to S3/Dynamo once apply is part of CI/CD.

## Dev variables
`env/dev.tfvars` contains placeholder values. Copy/update for other environments.

## TODO (future sprint)
- Wire Lambda into private subnets + security groups alongside RDS
- Add backend ECS task role + S3 permissions
- Configure remote Terraform state backend
