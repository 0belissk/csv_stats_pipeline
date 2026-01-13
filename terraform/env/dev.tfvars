aws_region             = "us-east-1"
project_name           = "csv-stats-pipeline"
s3_bucket_name         = "csv-stats-pipeline-dev-uploads"
lambda_function_name   = "csv-validator-dev"
lambda_artifact_path   = "../lambda/target/csv-validation-lambda.jar"
lambda_timeout         = 60
lambda_memory_size     = 512
log_retention_days     = 14
s3_notification_prefix = "uploads/"
s3_notification_suffix = ".csv"
db_url                 = "jdbc:postgresql://db.example.dev:5432/csvpipeline"
db_username            = "csvpipeline"
db_password            = "changeme"
tags = {
  Environment = "dev"
}
