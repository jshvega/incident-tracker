-- A role that can connect
CREATE ROLE sla_engine LOGIN;

-- Reach the schema
GRANT USAGE ON SCHEMA public TO sla_engine;

-- Read incidents
GRANT SELECT ON incidents TO sla_engine;

-- Change only the sweep's columns
GRANT UPDATE (incident_severity, sla_breached, updated_at) ON incidents TO sla_engine;

-- Record escalations
GRANT INSERT ON escalations TO sla_engine;