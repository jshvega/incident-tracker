-- Sweep test cases. Run by hand after schema + seed users exist.
-- CASCADE also clears status_history, comments, escalations. Users stay.
TRUNCATE incidents RESTART IDENTITY CASCADE;

INSERT INTO incidents (title, description, incident_severity, incident_status,
                       reporter_id, sla_minutes, created_at, resolved_at, due_at, sla_breached)
VALUES
('overdue low open',          'fixture', 'low',      'open',          (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now() - interval '2 hours', NULL,                         now() - interval '1 hour', false),
('overdue medium investigating','fixture','medium',  'investigating', (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now() - interval '2 hours', NULL,                         now() - interval '1 hour', false),
('overdue high open',         'fixture', 'high',     'open',          (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now() - interval '2 hours', NULL,                         now() - interval '1 hour', false),
('overdue critical open',     'fixture', 'critical', 'open',          (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now() - interval '2 hours', NULL,                         now() - interval '1 hour', false),
('not yet due open',          'fixture', 'medium',   'open',          (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now(),                      NULL,                         now() + interval '1 day',  false),
('overdue resolved',          'fixture', 'medium',   'resolved',      (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now() - interval '2 hours', now() - interval '30 minutes', now() - interval '1 hour', false),
('overdue closed',            'fixture', 'low',      'closed',        (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now() - interval '2 hours', now() - interval '30 minutes', now() - interval '1 hour', false),
('overdue already breached',  'fixture', 'high',     'open',          (SELECT id FROM users WHERE email = 'd@dom.com'), 60, now() - interval '2 hours', NULL,                         now() - interval '1 hour', true);