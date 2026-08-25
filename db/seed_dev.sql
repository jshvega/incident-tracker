/*
    This file is not part of the migration sequence. 
    Its purpose is to give the SLA sweep both matching and non-matching rows.

    Plain text seed credentials for local testing only: hello123
*/



INSERT INTO users (email, password_hash, user_role)
VALUES 
('a@dom.com', '$2b$10$wplrOkunoSC9WnfwtNps9.UkqeVHr4VEuKFYit90ydDk1h82QUzUW', 'admin'),
('b@dom.com', '$2b$10$wplrOkunoSC9WnfwtNps9.UkqeVHr4VEuKFYit90ydDk1h82QUzUW', 'assignee'),
('c@dom.com', '$2b$10$wplrOkunoSC9WnfwtNps9.UkqeVHr4VEuKFYit90ydDk1h82QUzUW', 'assignee'),
('d@dom.com', '$2b$10$wplrOkunoSC9WnfwtNps9.UkqeVHr4VEuKFYit90ydDk1h82QUzUW', 'reporter'),
('e@dom.com', '$2b$10$wplrOkunoSC9WnfwtNps9.UkqeVHr4VEuKFYit90ydDk1h82QUzUW', 'reporter');



INSERT INTO incidents (title, description, incident_severity, incident_status, reporter_id, assignee_id, sla_minutes, created_at, resolved_at, due_at)
VALUES
(
    'API gateway 100', 
    'Lorem ipsum.', 
    'high', 
    'investigating', 
    (SELECT id FROM users WHERE email = 'd@dom.com'), 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    180,
    NOW() - interval '28 hours',
    NULL,
    NOW() - interval '28 hours' + (interval '1 minute' * 180)
),
(
    'API gateway 101', 
    'Lorem ipsum.', 
    'medium', 
    'open', 
    (SELECT id FROM users WHERE email = 'd@dom.com'), 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    1000, 
    NOW() - interval '25 hours',
    NULL,
    NOW() - interval '25 hours' + (interval '1 minute' * 1000)
),
(
    'API gateway 102', 
    'Lorem ipsum.', 
    'low', 
    'open', 
    (SELECT id FROM users WHERE email = 'd@dom.com'), 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    2880, 
    NOW() - interval '25 hours',
    NULL,
    NOW() - interval '25 hours' + (interval '1 minute' * 2880)
),
(
    'API gateway 103', 
    'Lorem ipsum.', 
    'low', 
    'investigating', 
    (SELECT id FROM users WHERE email = 'd@dom.com'), 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    2600, 
    NOW() - interval '24 hours',
    NULL,
    NOW() - interval '24 hours' + (interval '1 minute' * 2600)
),
(
    'API gateway 104', 
    'Lorem ipsum.', 
    'medium', 
    'resolved', 
    (SELECT id FROM users WHERE email = 'd@dom.com'), 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    1000, 
    NOW() - interval '26 hours',
    NOW() - interval '26 hours' + (interval '1 minute' * 800),
    NOW() - interval '26 hours' + (interval '1 minute' * 1000)
),
(
    'API gateway 105', 
    'Lorem ipsum.', 
    'low', 
    'closed', 
    (SELECT id FROM users WHERE email = 'e@dom.com'), 
    (SELECT id FROM users WHERE email = 'c@dom.com'), 
    1200, 
    NOW() - interval '23 hours',
    NOW() - interval '23 hours' + (interval '1 minute' * 1100),
    NOW() - interval '23 hours' + (interval '1 minute' * 1200)
);



INSERT INTO status_history (incident_id, from_status, to_status, changed_by, changed_at)
VALUES
(
    (SELECT id FROM incidents WHERE title = 'API gateway 100'), 
    'open', 
    'investigating', 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    now() - interval '23 hours'
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 103'), 
    'open', 
    'investigating', 
    (SELECT id FROM users WHERE email = 'c@dom.com'), 
    now()
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 104'), 
    'open', 
    'investigating', 
    (SELECT id FROM users WHERE email = 'c@dom.com'), 
    NOW() - interval '24 hours' + (interval '1 minute' * 600)
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 104'), 
    'investigating', 
    'resolved', 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    NOW() - interval '24 hours' + (interval '1 minute' * 800)
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 105'), 
    'open', 
    'investigating', 
    (SELECT id FROM users WHERE email = 'c@dom.com'), 
    NOW() - interval '24 hours' + (interval '1 minute' * 800)
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 105'), 
    'investigating', 
    'resolved', 
    (SELECT id FROM users WHERE email = 'b@dom.com'), 
    NOW() - interval '24 hours' + (interval '1 minute' * 1100)
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 105'), 
    'resolved', 
    'closed', 
    (SELECT id FROM users WHERE email = 'a@dom.com'), 
    NOW() - interval '24 hours' + (interval '1 minute' * 1150)
);



INSERT INTO comments (incident_id, author_id, body, created_at)
VALUES
(
    (SELECT id FROM incidents WHERE title = 'API gateway 100'), 
    (SELECT id FROM users WHERE email = 'e@dom.com'), 
    'Comment body, lorem ipsum.', 
    now() - interval '22 hours'
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 100'), 
    (SELECT id FROM users WHERE email = 'a@dom.com'), 
    'Comment body, lorem ipsum.', 
    now() - interval '21 hours'
),
(
    (SELECT id FROM incidents WHERE title = 'API gateway 102'), 
    (SELECT id FROM users WHERE email = 'a@dom.com'), 
    'Comment body, lorem ipsum.', 
    now() - interval '20 hours'
)
,
(
    (SELECT id FROM incidents WHERE title = 'API gateway 105'), 
    (SELECT id FROM users WHERE email = 'e@dom.com'), 
    'Comment body, lorem ipsum.', 
    now() - interval '19 hours'
);



INSERT INTO escalations (incident_id, reason, escalated_at, previous_severity, new_severity)
VALUES
(
    (SELECT id FROM incidents WHERE title = 'API gateway 100'),
    'Lorem ipsum.',
    NOW() - interval '28 hours' + (interval '1 minute' * 190),
    'medium',
    'high'
);