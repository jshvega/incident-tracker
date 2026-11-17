resource "aws_sqs_queue" "dlq" {
  name                      = "${local.project}-dlq"
  message_retention_seconds = 14 * 24 * 60 * 60
}