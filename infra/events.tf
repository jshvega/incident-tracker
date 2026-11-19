resource "aws_cloudwatch_event_bus" "main" {
  name = "incident-tracker"
}

resource "aws_lambda_permission" "events_invoke_notify" {
  statement_id  = "AllowEventBridgeInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.notify.function_name
  principal     = "events.amazonaws.com"
  source_arn    = aws_cloudwatch_event_rule.critical_created.arn
}

resource "aws_cloudwatch_event_rule" "critical_created" {
  name           = "incident-created-critical"
  event_bus_name = aws_cloudwatch_event_bus.main.name
  event_pattern = jsonencode({
    source        = ["incident-tracker.api"]
    "detail-type" = ["IncidentCreated"]
    detail        = { severity = ["critical"] }
  })
}

resource "aws_cloudwatch_event_target" "notify" {
  rule           = aws_cloudwatch_event_rule.critical_created.name
  event_bus_name = aws_cloudwatch_event_bus.main.name
  arn            = aws_lambda_function.notify.arn
  retry_policy {
    maximum_event_age_in_seconds = 3600
    maximum_retry_attempts       = 185
  }
  dead_letter_config {
    arn = aws_sqs_queue.dlq.arn
  }
}

resource "aws_iam_user" "publisher" {
  name          = "incident-tracker-api-publisher"
  force_destroy = true
}

resource "aws_iam_user_policy" "publisher" {
  name = "put-events-incident-tracker-bus"
  user = aws_iam_user.publisher.name
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PutEventsOnOwnBus"
        Effect    = "Allow"
        Action    = "events:PutEvents"
        Resource  = aws_cloudwatch_event_bus.main.arn
        Condition = { StringEquals = { "events:source" = "incident-tracker.api" } }
      }
    ]
  })
}