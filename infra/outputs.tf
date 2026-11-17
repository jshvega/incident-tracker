output "function_name" {
  value = aws_lambda_function.sla_sweep.function_name
}

output "function_arn" {
  value = aws_lambda_function.sla_sweep.arn
}

output "dlq_url" {
  value = aws_sqs_queue.dlq.url
}

output "schedule_name" {
  value = aws_scheduler_schedule.sla_sweep.name
}