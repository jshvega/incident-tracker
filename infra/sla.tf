locals {
  name_prefix = "incident-tracker-sla-sweep"
}

resource "aws_cloudwatch_log_group" "sla_sweep" {
  name              = "/aws/lambda/${local.name_prefix}"
  retention_in_days = 14
}