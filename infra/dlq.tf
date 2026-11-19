resource "aws_sqs_queue" "dlq" {
  name                      = "${local.project}-dlq"
  message_retention_seconds = 14 * 24 * 60 * 60
}

resource "aws_sqs_queue_policy" "dlq" {
  queue_url = aws_sqs_queue.dlq.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [{
    Sid       = "EventBridgeRuleDlq"
    Effect    = "Allow"
    Action    = "sqs:SendMessage"
    Resource  = aws_sqs_queue.dlq.arn
    Principal = { Service = "events.amazonaws.com" }
    Condition = { ArnEquals = { "aws:SourceArn" = aws_cloudwatch_event_rule.critical_created.arn } }
  }] })
}