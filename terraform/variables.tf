variable "aws_region" {
  description = "AWS region for the CSV pipeline stack"
  type        = string
}

variable "project_name" {
  description = "Base name used for tagging and resource naming"
  type        = string
}

variable "s3_bucket_name" {
  description = "Name of the S3 bucket that stores CSV uploads"
  type        = string
}

variable "lambda_function_name" {
  description = "Name of the CSV validation Lambda function"
  type        = string
}

variable "lambda_artifact_path" {
  description = "Filesystem path to the built Lambda JAR"
  type        = string
}

variable "db_url" {
  description = "JDBC URL used by the Lambda function"
  type        = string
}

variable "db_username" {
  description = "Database username for Lambda"
  type        = string
}

variable "db_password" {
  description = "Database password for Lambda"
  type        = string
  sensitive   = true
}

variable "lambda_timeout" {
  description = "Lambda timeout in seconds"
  type        = number
  default     = 60
}

variable "lambda_memory_size" {
  description = "Lambda memory size in MB"
  type        = number
  default     = 512
}

variable "log_retention_days" {
  description = "CloudWatch log retention"
  type        = number
  default     = 14
}

variable "s3_notification_prefix" {
  description = "Prefix filter for S3 event notifications"
  type        = string
  default     = "uploads/"
}

variable "s3_notification_suffix" {
  description = "Suffix filter for S3 event notifications"
  type        = string
  default     = ".csv"
}

variable "tags" {
  description = "Common resource tags"
  type        = map(string)
  default     = {}
}

variable "lambda_subnet_ids" {
  description = "Optional subnet IDs for Lambda VPC config"
  type        = list(string)
  default     = []
}

variable "lambda_security_group_ids" {
  description = "Optional security group IDs for Lambda VPC config"
  type        = list(string)
  default     = []
}
