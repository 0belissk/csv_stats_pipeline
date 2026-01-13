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
