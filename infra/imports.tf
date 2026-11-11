import {
  to = aws_cloudwatch_log_group.sla_sweep
  id = "/aws/lambda/incident-tracker-sla-sweep"
}

import {
  to = aws_iam_role.sla_sweep
  id = "incident-tracker-sla-sweep-role"
}

import {
  to = aws_iam_role_policy.sla_sweep
  id = "incident-tracker-sla-sweep-role:incident-tracker-sla-sweep-policy"
}