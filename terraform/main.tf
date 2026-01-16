locals {
  common_tags = merge({
    Project = var.project_name
  }, var.tags)

  orchestrator_function_name = "${var.lambda_function_name}-orchestrator"
  status_function_name       = "${var.lambda_function_name}-status"
}

resource "aws_s3_bucket" "csv_uploads" {
  bucket = var.s3_bucket_name
  tags   = local.common_tags
}

resource "aws_s3_bucket_public_access_block" "csv_uploads" {
  bucket = aws_s3_bucket.csv_uploads.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "csv_uploads" {
  bucket = aws_s3_bucket.csv_uploads.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "csv_uploads" {
  bucket = aws_s3_bucket.csv_uploads.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${var.lambda_function_name}"
  retention_in_days = var.log_retention_days
  tags              = local.common_tags
}

resource "aws_cloudwatch_log_group" "lambda_orchestrator" {
  name              = "/aws/lambda/${local.orchestrator_function_name}"
  retention_in_days = var.log_retention_days
  tags              = local.common_tags
}

resource "aws_cloudwatch_log_group" "lambda_status" {
  name              = "/aws/lambda/${local.status_function_name}"
  retention_in_days = var.log_retention_days
  tags              = local.common_tags
}

resource "aws_cloudwatch_log_group" "state_machine" {
  name              = "/aws/states/${var.project_name}-csv-processing"
  retention_in_days = var.log_retention_days
  tags              = local.common_tags
}

# IAM role + policy for Lambda

data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "lambda" {
  name               = "${var.lambda_function_name}-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
  tags               = local.common_tags
}

resource "aws_iam_role" "lambda_orchestrator" {
  name               = "${local.orchestrator_function_name}-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
  tags               = local.common_tags
}

resource "aws_iam_role" "lambda_status" {
  name               = "${local.status_function_name}-role"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
  tags               = local.common_tags
}

data "aws_iam_policy_document" "lambda_permissions" {
  statement {
    sid    = "AllowS3ReadUploads"
    effect = "Allow"

    actions = [
      "s3:GetObject"
    ]

    resources = ["${aws_s3_bucket.csv_uploads.arn}/${var.s3_notification_prefix}*"]
  }

  statement {
    sid    = "AllowLogging"
    effect = "Allow"

    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]

    resources = [
      aws_cloudwatch_log_group.lambda.arn,
      "${aws_cloudwatch_log_group.lambda.arn}:*"
    ]
  }
}

resource "aws_iam_role_policy" "lambda_inline" {
  name   = "${var.lambda_function_name}-policy"
  role   = aws_iam_role.lambda.id
  policy = data.aws_iam_policy_document.lambda_permissions.json
}

data "aws_iam_policy_document" "lambda_orchestrator_permissions" {
  statement {
    sid    = "AllowLogging"
    effect = "Allow"

    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]

    resources = [
      aws_cloudwatch_log_group.lambda_orchestrator.arn,
      "${aws_cloudwatch_log_group.lambda_orchestrator.arn}:*"
    ]
  }

  statement {
    sid    = "AllowStateMachineStart"
    effect = "Allow"

    actions = [
      "states:StartExecution"
    ]

    resources = [aws_sfn_state_machine.csv_processing.arn]
  }
}

resource "aws_iam_role_policy" "lambda_orchestrator_inline" {
  name   = "${local.orchestrator_function_name}-policy"
  role   = aws_iam_role.lambda_orchestrator.id
  policy = data.aws_iam_policy_document.lambda_orchestrator_permissions.json
}

data "aws_iam_policy_document" "lambda_status_permissions" {
  statement {
    sid    = "AllowLogging"
    effect = "Allow"

    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]

    resources = [
      aws_cloudwatch_log_group.lambda_status.arn,
      "${aws_cloudwatch_log_group.lambda_status.arn}:*"
    ]
  }
}

resource "aws_iam_role_policy" "lambda_status_inline" {
  name   = "${local.status_function_name}-policy"
  role   = aws_iam_role.lambda_status.id
  policy = data.aws_iam_policy_document.lambda_status_permissions.json
}

resource "aws_lambda_function" "csv_validator" {
  function_name = var.lambda_function_name
  role          = aws_iam_role.lambda.arn
  runtime       = "java17"
  handler       = "com.paul.csvpipeline.backend.lambda.handler.CsvValidationLambda::handleRequest"

  filename         = var.lambda_artifact_path
  source_code_hash = filebase64sha256(var.lambda_artifact_path)

  timeout     = var.lambda_timeout
  memory_size = var.lambda_memory_size
  publish     = true
  tags        = local.common_tags

  environment {
    variables = {
      DB_URL      = var.db_url
      DB_USERNAME = var.db_username
      DB_PASSWORD = var.db_password
    }
  }

  dynamic "vpc_config" {
    for_each = length(var.lambda_subnet_ids) > 0 && length(var.lambda_security_group_ids) > 0 ? [1] : []
    content {
      subnet_ids         = var.lambda_subnet_ids
      security_group_ids = var.lambda_security_group_ids
    }
  }
}

resource "aws_lambda_function" "csv_orchestrator" {
  function_name = local.orchestrator_function_name
  role          = aws_iam_role.lambda_orchestrator.arn
  runtime       = "java17"
  handler       = "com.paul.csvpipeline.backend.lambda.handler.StepFunctionOrchestratorLambda::handleRequest"

  filename         = var.lambda_artifact_path
  source_code_hash = filebase64sha256(var.lambda_artifact_path)

  timeout     = 30
  memory_size = 512
  publish     = true
  tags        = local.common_tags

  environment {
    variables = {
      DB_URL            = var.db_url
      DB_USERNAME       = var.db_username
      DB_PASSWORD       = var.db_password
      STATE_MACHINE_ARN = aws_sfn_state_machine.csv_processing.arn
    }
  }

  dynamic "vpc_config" {
    for_each = length(var.lambda_subnet_ids) > 0 && length(var.lambda_security_group_ids) > 0 ? [1] : []
    content {
      subnet_ids         = var.lambda_subnet_ids
      security_group_ids = var.lambda_security_group_ids
    }
  }
}

resource "aws_lambda_function" "upload_status" {
  function_name = local.status_function_name
  role          = aws_iam_role.lambda_status.arn
  runtime       = "java17"
  handler       = "com.paul.csvpipeline.backend.lambda.handler.UploadStatusLambda::handleRequest"

  filename         = var.lambda_artifact_path
  source_code_hash = filebase64sha256(var.lambda_artifact_path)

  timeout     = 30
  memory_size = 512
  publish     = true
  tags        = local.common_tags

  environment {
    variables = {
      DB_URL      = var.db_url
      DB_USERNAME = var.db_username
      DB_PASSWORD = var.db_password
    }
  }

  dynamic "vpc_config" {
    for_each = length(var.lambda_subnet_ids) > 0 && length(var.lambda_security_group_ids) > 0 ? [1] : []
    content {
      subnet_ids         = var.lambda_subnet_ids
      security_group_ids = var.lambda_security_group_ids
    }
  }
}

resource "aws_lambda_permission" "allow_s3_invoke" {
  statement_id  = "AllowExecutionFromS3"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.csv_orchestrator.arn
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.csv_uploads.arn
}

resource "aws_s3_bucket_notification" "csv_notifications" {
  bucket = aws_s3_bucket.csv_uploads.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.csv_orchestrator.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = var.s3_notification_prefix
    filter_suffix       = var.s3_notification_suffix
  }

  depends_on = [aws_lambda_permission.allow_s3_invoke]
}

data "aws_iam_policy_document" "step_functions_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["states.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "step_functions" {
  name               = "${var.project_name}-csv-processing-role"
  assume_role_policy = data.aws_iam_policy_document.step_functions_assume_role.json
  tags               = local.common_tags
}

data "aws_iam_policy_document" "step_functions_permissions" {
  statement {
    sid    = "AllowLambdaInvocation"
    effect = "Allow"

    actions = [
      "lambda:InvokeFunction"
    ]

    resources = [
      aws_lambda_function.csv_validator.arn,
      aws_lambda_function.upload_status.arn
    ]
  }

  statement {
    sid    = "AllowLogging"
    effect = "Allow"

    actions = [
      "logs:CreateLogDelivery",
      "logs:GetLogDelivery",
      "logs:UpdateLogDelivery",
      "logs:DeleteLogDelivery",
      "logs:ListLogDeliveries",
      "logs:PutLogEvents",
      "logs:DescribeLogGroups"
    ]

    resources = [
      aws_cloudwatch_log_group.state_machine.arn,
      "${aws_cloudwatch_log_group.state_machine.arn}:*"
    ]
  }
}

resource "aws_iam_role_policy" "step_functions_inline" {
  name   = "${var.project_name}-csv-processing-policy"
  role   = aws_iam_role.step_functions.id
  policy = data.aws_iam_policy_document.step_functions_permissions.json
}

resource "aws_sfn_state_machine" "csv_processing" {
  name     = "${var.project_name}-csv-processing"
  role_arn = aws_iam_role.step_functions.arn
  definition = templatefile("${path.module}/state_machine/csv_processing.asl.json", {
    validator_lambda_arn = aws_lambda_function.csv_validator.arn,
    status_lambda_arn    = aws_lambda_function.upload_status.arn
  })

  logging_configuration {
    include_execution_data = true
    level                  = "ALL"
    log_destination        = aws_cloudwatch_log_group.state_machine.arn
  }

  tags = local.common_tags
}
