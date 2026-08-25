-- THROWAWAY VERIFICATION QUERIES


SELECT id, title, incident_status, sla_minutes, created_at, due_at,
       due_at < now() AS is_overdue
FROM incidents
ORDER BY id;