locals {
  notify_name = "incident-tracker-notify"
  notify_zip  = "${path.module}/../lambdas/notify/build/notify.zip"
}

resource "aws_s3_bucket" "notifications" {
  bucket        = "incident-tracker-notifications-${data.aws_caller_identity.current.account_id}"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "notifications" {
  bucket                  = aws_s3_bucket.notifications.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudwatch_log_group" "notify" {
  name              = "/aws/lambda/${local.notify_name}"
  retention_in_days = 14
}

resource "aws_iam_role" "notify" {
  name = "${local.notify_name}-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      },
    ]
  })
}

resource "aws_iam_role_policy" "notify" {
  name = "${local.notify_name}-policy"
  role = aws_iam_role.notify.name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "WriteOwnLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "${aws_cloudwatch_log_group.notify.arn}:*"
      },
      {
        Sid      = "SendFailuresToDlq"
        Effect   = "Allow"
        Action   = "sqs:SendMessage"
        Resource = aws_sqs_queue.dlq.arn
      },
      {
        Sid      = "WriteNotifications"
        Effect   = "Allow"
        Action   = "s3:PutObject"
        Resource = "${aws_s3_bucket.notifications.arn}/notifications/*"
      }
    ]
  })
}

resource "aws_lambda_function" "notify" {
  function_name    = local.notify_name
  role             = aws_iam_role.notify.arn
  handler          = "notify.handler"
  runtime          = "python3.12"
  architectures    = ["arm64"]
  memory_size      = 128
  timeout          = 10
  filename         = local.notify_zip
  source_code_hash = filebase64sha256(local.notify_zip)
  environment {
    variables = {
      NOTIFY_BUCKET = aws_s3_bucket.notifications.bucket
    }
  }
  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.notify.name
  }
}

resource "aws_lambda_function_event_invoke_config" "notify" {
  function_name                = aws_lambda_function.notify.function_name
  maximum_retry_attempts       = 2
  maximum_event_age_in_seconds = 3600
  destination_config {
    on_failure {
      destination = aws_sqs_queue.dlq.arn
    }
  }
  depends_on = [aws_iam_role_policy.notify]
}