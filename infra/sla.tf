locals {
  name_prefix = "incident-tracker-sla-sweep"
  sla_zip     = "${path.module}/../lambdas/sla/build/sla-sweep.zip"
}

resource "aws_cloudwatch_log_group" "sla_sweep" {
  name              = "/aws/lambda/${local.name_prefix}"
  retention_in_days = 14
}

resource "aws_ssm_parameter" "db_password" {
  name             = "/incident-tracker/prod/sla/db-password"
  type             = "SecureString"
  value_wo         = var.db_password
  value_wo_version = 1
}

resource "aws_iam_role" "sla_sweep" {
  name = "${local.name_prefix}-role"
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

resource "aws_iam_role_policy" "sla_sweep" {
  name = "${local.name_prefix}-policy"
  role = aws_iam_role.sla_sweep.name
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
        Resource = "${aws_cloudwatch_log_group.sla_sweep.arn}:*"
      },
      {
        Sid      = "ReadDbPassword"
        Effect   = "Allow"
        Action   = "ssm:GetParameter"
        Resource = "${aws_ssm_parameter.db_password.arn}"
      }
    ]
  })
}

resource "aws_lambda_function" "sla_sweep" {
  function_name    = local.name_prefix
  role             = aws_iam_role.sla_sweep.arn
  handler          = "sweep.handler"
  runtime          = "python3.12"
  architectures    = ["arm64"]
  memory_size      = 256
  timeout          = 10
  filename         = local.sla_zip
  source_code_hash = filebase64sha256(local.sla_zip)
  environment {
    variables = {
      DB_SSL_CA         = "prod-ca-2021.crt"
      DB_PORT           = var.db_port
      DB_USER           = var.db_user
      DB_NAME           = var.db_name
      DB_HOST           = var.db_host
      DB_PASSWORD_PARAM = aws_ssm_parameter.db_password.name
    }
  }
  logging_config {
    log_format = "Text"
    log_group  = aws_cloudwatch_log_group.sla_sweep.name
  }
}