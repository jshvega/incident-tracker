EXPLAIN ANALYZE 
SELECT 
    id,
    incident_severity
FROM incidents
WHERE incident_status IN ('open', 'investigating')
AND due_at < now();