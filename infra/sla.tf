locals {
  name_prefix = "incident-tracker-sla-sweep"
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