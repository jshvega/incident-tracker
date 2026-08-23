CREATE TYPE severity AS ENUM ('low', 'medium', 'high', 'critical');
CREATE TYPE status AS ENUM ('open', 'investigating', 'resolved', 'closed');
CREATE TYPE role AS ENUM ('reporter', 'assignee', 'admin');

CREATE TABLE users (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    email VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    user_role role NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL
);

CREATE TABLE incidents (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    title VARCHAR(50) NOT NULL,
    description VARCHAR(250) NOT NULL,
    incident_severity severity NOT NULL,
    incident_status status DEFAULT 'open' NOT NULL,
    reporter_id BIGINT NOT NULL,
    assignee_id BIGINT,
    sla_minutes INT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    resolved_at TIMESTAMPTZ,
    due_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT valid_sla CHECK (sla_minutes > 0),

    FOREIGN KEY(reporter_id) REFERENCES users(id),
    FOREIGN KEY(assignee_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE status_history (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    incident_id BIGINT NOT NULL,
    from_status status NOT NULL,
    to_status status NOT NULL,
    changed_by BIGINT,
    changed_at TIMESTAMPTZ DEFAULT now() NOT NULL,

    CONSTRAINT from_to CHECK (from_status <> to_status),

    FOREIGN KEY(incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    FOREIGN KEY(changed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE comments (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    incident_id BIGINT NOT NULL,
    author_id BIGINT,
    body VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT now() NOT NULL,

    FOREIGN KEY(incident_id) REFERENCES incidents(id) ON DELETE CASCADE,
    FOREIGN KEY(author_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE escalations (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    incident_id BIGINT NOT NULL,
    reason VARCHAR(250),
    escalated_at TIMESTAMPTZ DEFAULT now() NOT NULL,
    previous_severity severity NOT NULL,
    new_severity severity NOT NULL,

    CONSTRAINT previous_new CHECK (previous_severity <> new_severity),

    FOREIGN KEY(incident_id) REFERENCES incidents(id) ON DELETE CASCADE
);

CREATE INDEX idx_incidents_due_at_unresolved ON incidents(due_at)
WHERE (incident_status = 'open' OR incident_status = 'investigating');