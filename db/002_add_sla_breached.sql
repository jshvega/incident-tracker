ALTER TABLE incidents
    ADD COLUMN sla_breached BOOLEAN NOT NULL DEFAULT false;

DROP INDEX idx_incidents_due_at_unresolved;

CREATE INDEX idx_incidents_due_at_unresolved ON incidents(due_at)
WHERE incident_status IN ('open', 'investigating') AND NOT sla_breached;