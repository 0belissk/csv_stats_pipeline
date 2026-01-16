output "s3_bucket_name" {
  value       = aws_s3_bucket.csv_uploads.id
  description = "CSV uploads bucket name"
}

output "s3_bucket_arn" {
  value       = aws_s3_bucket.csv_uploads.arn
  description = "CSV uploads bucket ARN"
}

output "lambda_function_name" {
  value       = aws_lambda_function.csv_validator.function_name
  description = "CSV validation Lambda name"
}

output "lambda_function_arn" {
  value       = aws_lambda_function.csv_validator.arn
  description = "CSV validation Lambda ARN"
}

output "state_machine_arn" {
  value       = aws_sfn_state_machine.csv_processing.arn
  description = "Step Functions state machine ARN"
}

output "orchestrator_lambda_name" {
  value       = aws_lambda_function.csv_orchestrator.function_name
  description = "Lambda that launches the Step Functions execution"
}

output "status_lambda_name" {
  value       = aws_lambda_function.upload_status.function_name
  description = "Lambda that persists upload status transitions"
}
