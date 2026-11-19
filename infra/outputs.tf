output "sla_function_name" {
  value = aws_lambda_function.sla_sweep.function_name
}

output "sla_function_arn" {
  value = aws_lambda_function.sla_sweep.arn
}

output "dlq_url" {
  value = aws_sqs_queue.dlq.url
}

output "schedule_name" {
  value = aws_scheduler_schedule.sla_sweep.name
}

output "event_bus_name" {
  value = aws_cloudwatch_event_bus.main.name
}

output "event_bus_arn" {
  value = aws_cloudwatch_event_bus.main.arn
}

output "notifications_bucket" {
  value = aws_s3_bucket.notifications.bucket
}