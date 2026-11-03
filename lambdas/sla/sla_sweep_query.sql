EXPLAIN ANALYZE 
SELECT 
    id,
    title,
    incident_severity,
    due_at
FROM incidents
WHERE incident_status IN ('open', 'investigating')
AND due_at < now()
AND NOT sla_breached
ORDER BY id;