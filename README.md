# Incident Tracker

[![CI](https://github.com/jshvega/incident-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/jshvega/incident-tracker/actions/workflows/ci.yml)

A role-based incident management system for platform and SRE teams. The API is built with **Java 21 and Spring Boot**, containerized with Docker, and deployed on Render with PostgreSQL on Supabase. A **Python SLA engine on AWS Lambda** runs every five minutes and escalates incidents that missed their deadline, and critical incidents flow through an **EventBridge event bus** to a second Lambda. All AWS infrastructure is defined in **Terraform**, and **GitHub Actions** checks every push.

Users report incidents, assign them, and move them through a lifecycle (`open → investigating → resolved → closed`). Every action is checked against the user's role, and every status change is recorded in an audit history. When an incident misses its deadline, the SLA engine raises its severity and records the escalation, with no human involved.

**Live API:** https://incident-tracker-5zpj.onrender.com
> Free hosting: the first request after 15 idle minutes takes about 75 seconds while the service wakes up. Every request after that takes well under a second. See [Known limitations](#known-limitations).

---

## Architecture

**Synchronous, business-facing work runs in Java. Scheduled and asynchronous work runs serverless on AWS.**

```
Client ──HTTPS/JWT──> Spring Boot API (Render) ──SQL──> PostgreSQL (Supabase)
                            │                               ▲
                            │ IncidentCreated               │ reads and escalates
                            │ (after commit)                │ overdue incidents
                            ▼                               │
                EventBridge bus "incident-tracker"   SLA Lambda (Python)
                            │                               ▲
                            │ rule: severity = critical     │ every 5 minutes
                            ▼                               │
                  Notification Lambda (Python)     EventBridge Scheduler
                            │
                            ▼
                   S3 archive (one summary per event)

   Failures from both Lambdas land in one SQS dead-letter queue.
   Everything on AWS is provisioned with Terraform.
```

---

## What it does

### The API

- **Authentication:** registration and login with BCrypt-hashed passwords and stateless JWTs.
- **Three roles with genuinely different permissions:**
  - **Reporter:** creates incidents, comments, reads all incidents.
  - **Assignee:** additionally takes incidents, changes their status, resolves them.
  - **Admin:** additionally reassigns, closes any incident, deletes incidents, manages user roles.
- **A lifecycle state machine:** illegal transitions are rejected, and each legal transition is limited to the roles allowed to make it.
- **Ownership rules:** for example, a reporter can edit their own incident but not someone else's.
- **An audit trail:** every transition writes a `status_history` row (who, from, to, when).
- **Consistent errors:** RFC 9457 `ProblemDetail` responses, with 401 for "who are you?" and 403 for "you can't do that."
- **Events:** after an incident is created and committed, the API publishes an `IncidentCreated` event to EventBridge.

| Method | Path | Who |
|---|---|---|
| POST | `/auth/register` | Public (always creates a reporter) |
| POST | `/auth/login` | Public |
| GET | `/incidents` | Any authenticated user |
| POST | `/incidents` | Any authenticated user |
| GET | `/incidents/{id}` | Any authenticated user |
| PATCH | `/incidents/{id}` | Admin, or the incident's reporter or assignee |
| POST | `/incidents/{id}/transitions` | Admin, or the incident's assignee (each transition also limits which roles may make it) |
| PATCH | `/incidents/{id}/assignee` | Admin (any assignee or admin, or unassign), or an assignee taking an unassigned incident for themselves. Assigning to a reporter returns 409. |
| GET | `/incidents/{id}/history` | Any authenticated user |
| POST | `/incidents/{id}/comments` | Any authenticated user |
| GET | `/incidents/{id}/comments` | Any authenticated user |
| DELETE | `/incidents/{id}` | Admin |
| PATCH | `/users/{id}/role` | Admin |
| GET | `/actuator/health/liveness` | Public |

### The SLA engine

- **Runs on its own:** EventBridge Scheduler invokes it on AWS Lambda every five minutes.
- **Finds breached incidents:** open or investigating incidents past their `due_at` that haven't been flagged yet.
- **Escalates on a fixed ladder:** low → high, medium → high, high → critical. Critical incidents are flagged without a severity change.
- **Records what it did:** sets `sla_breached` on the incident and writes an `escalations` row (previous severity, new severity, reason).
- **Leaves the lifecycle alone:** status and assignee belong to the API and its rules.
- **Is safe to run any number of times:** see [Design decisions](#design-decisions).
- **Logs structured JSON:** one line per incident and one summary per run, all sharing a `run_id`, so a single run can be pulled up on its own:

```json
{"level": "INFO", "msg": "sweep_complete", "run_id": "2b6afbb8-ba65-4f1a-b41a-ba26d1f61994", "candidates": 1, "escalated": 1, "breach_only": 0, "skipped": 0, "failed": 0, "duration_ms": 171}
```

Verified in production: an incident created with a one-minute SLA was escalated by the next scheduled run, about 90 seconds after its deadline, and the run after that found nothing to do.

### Notifications

- **Every creation is published;** a rule on the bus routes only `severity: critical` to the notification Lambda.
- **The notification Lambda archives a summary to S3,** at `notifications/<event date>/<event id>.json`. Real delivery (Slack, email through SES) is the next consumer, added as a rule target with no change to the API.

---

## Tech stack

| Layer | Choice |
|---|---|
| API | Java 21, Spring Boot 4, Spring Data JPA (Hibernate 7) |
| Security | Spring Security, a custom JWT filter (jjwt, HS256), BCrypt |
| Events (API side) | AWS SDK for Java 2 (EventBridge module, `url-connection-client`) |
| Lambdas | Python 3.12 on arm64; pg8000 (pure-Python Postgres driver) for the SLA engine, boto3 for the notification handler |
| AWS | Lambda, EventBridge Scheduler, EventBridge custom bus, SQS (dead-letter queue), S3, SSM Parameter Store, IAM, CloudWatch Logs |
| Infrastructure as code | Terraform 1.15, AWS provider 6 |
| Database | PostgreSQL 17 (Supabase in production, Docker Compose locally) |
| Build | Maven (API), pip with pinned requirements and a `build.py` zip script per Lambda |
| Container | Multi-stage Docker image (Eclipse Temurin 21 JRE, non-root) |
| Hosting | Render (free web service, 512 MB RAM, 0.1 CPU), AWS `us-east-1` |
| CI | GitHub Actions: API tests against a Postgres service container, `terraform fmt -check` and `validate` |
| Tests | JUnit 5 and Mockito (API), fixture-driven verification (SLA engine) |

---

## Repository layout

```
api/                 Spring Boot API
lambdas/sla/         SLA engine, with a test fixture in fixtures/
lambdas/notify/      notification handler, with sample events in events/
infra/               Terraform for everything on AWS
db/                  SQL schema, migrations and dev seed data, loaded by Docker Compose
.github/workflows/   CI
docker-compose.yml
PROGRESS.md          a log of each build phase
```

---

## Design decisions

Each decision was made before the code was written, with the alternatives and tradeoffs documented. The most interesting ones:

### The split

**Synchronous work in Java, scheduled and asynchronous work on AWS.** The API answers requests; the SLA engine runs on a timer; notifications react to events. Keeping scheduled work out of Spring (no `@Scheduled` anywhere) keeps the API a pure request handler. The Lambdas talk to Postgres directly and never call the API, and the API never invokes a Lambda: the halves share a database and an event bus, not a call graph.

### The API

**A custom JWT filter instead of Spring's OAuth2 resource server.** The resource server would have hidden the exact mechanism this project set out to build: read the header, verify the signature, check expiry, and populate the security context. In production I would use the resource server. Here I wanted to own the mechanism.

**Authorization lives in the service layer.** Rules that apply to a whole endpoint (only admins delete) live in the security config as URL rules. Rules that depend on the data (is this user the reporter of this incident?) live in the service, where the data is. The state machine itself knows which roles may make each transition.

**Stateless tokens, with honest tradeoffs.** Tokens last 60 minutes with no refresh, and the role travels inside the token. That means no session store, but also no instant revocation: a role change takes effect at the user's next login. That is acceptable at this scale and a known limitation at a larger one.

**One JWT secret per environment.** A token minted by the local stack is rejected by production (verified). Sharing a secret would let anyone who can create an admin locally mint a token production accepts.

**Development never touches production data.** Locally, everything runs against Postgres in Docker Compose, built from scratch from the versioned SQL files in `/db`. Supabase is production only. Hibernate runs in `validate` mode, so any drift between the SQL files and the entities stops the app from booting.

**A liveness health check that ignores the database.** Render restarts the service when its health check fails. A check that included the database would restart a healthy app every time the database blipped, which fixes nothing.

### The SLA engine

**Idempotent by construction.** A scheduler can fire twice, runs can overlap, and a failed run can be retried, so the sweep has to be safe to repeat. The guard is a database column, `sla_breached`, checked and set in the same UPDATE. Verified two ways: running the sweep twice in a row (the second run finds nothing), and forcing two runs to overlap (the second read the same four incidents before the first committed, and changed none of them).

**A flag instead of a moved deadline.** An earlier design pushed `due_at` forward after each escalation. That would have erased the record of when the breach happened, so `due_at` stays the real deadline. The flag records the one thing that can't be derived from it: that the sweep already acted.

**One transaction per incident.** The severity change and the escalation record commit together or not at all. A failing incident is rolled back and logged without stopping the others, and a half-finished run is harmless because the next one picks up the rest.

**The database is the only clock.** Every timestamp and comparison uses Postgres `now()`, so a laptop, a Lambda and the database can never disagree about what time it is.

**A pure-Python database driver.** pg8000 installs the same way on Windows and on Lambda's Linux. The usual choice, psycopg2, ships compiled C that has to be built for Lambda specifically, a classic first-deploy failure. Its speed advantage doesn't matter at a handful of rows per run.

**A least-privilege database role.** The Lambda logs in as `sla_engine`, which can read incidents, update only severity, the breach flag and `updated_at`, and insert escalations. Any other write is rejected by Postgres itself.

### Events and failure handling

**EventBridge Scheduler, not a classic scheduled rule.** It's the dedicated service for the job, with its own retry policy and dead-letter queue per schedule, and a free allowance of 14 million invocations a month.

**The next tick is the retry.** A failed sweep is not retried by Lambda, because the next run five minutes later does the same work. The failure is recorded in the dead-letter queue instead.

**The API publishes facts, the bus decides who cares.** Every creation is published, and the critical filter lives in an EventBridge rule. A new consumer is a new rule, with zero Java changes. The API only needs one permission, `events:PutEvents` on one bus, and knows nothing about Lambdas.

**Publish after commit, never fail the request.** The event is published only after the incident is committed, so there are no events for incidents that rolled back. If publishing fails, the error is logged with the incident id and the request still returns 201: failing it after the commit would invite a client retry and a duplicate incident. The cost is that a failed publish loses that notification. A transactional outbox is the fix if that ever matters.

**Idempotent by design, not by guard.** EventBridge delivers at least once. The notification Lambda writes to a key built from the event's id and timestamp, so a redelivery overwrites the same object with the same content.

**Two failure layers, one dead-letter queue.** Scheduler and the bus rule retry when they can't hand an event to Lambda. Lambda retries when the code throws (twice for notifications, never for the sweep). Both layers send what's left to one SQS queue. Drilled on purpose: an event with an unsupported schema version ran three times, then landed in the queue with the error and the original event, ready to redrive.

### Infrastructure

**Everything on AWS is Terraform.** The SLA Lambda was first built by hand in the console to learn the pieces, then imported into Terraform; since then the console is not the source of truth, and code reaches a Lambda only through `build.py`, `terraform plan` and `apply`.

**No secrets in Terraform state.** The database password is set through a write-only argument from an ephemeral variable, so it never reaches the plan or the state file (verified by searching state). The API's AWS access key is created outside Terraform for the same reason.

**A scoped publisher identity.** The key the API uses can publish to one bus, from one source, and nothing else (verified: the default bus and S3 are refused). In production this would be OIDC or role federation instead of a long-lived key.

---

## Performance

### Fitting Spring Boot into 512 MB and 0.1 CPU

The free tier is small, so this was measured rather than guessed. All numbers come from runs at production limits (`--memory=512m --cpus=0.1`, prod profile).

**Image size and build speed.** A naive single-stage image was 1.13 GB and re-downloaded every dependency on any code change (about 30 s). The multi-stage build ships only the JRE and the jar: **584 MB**, with **7.5 s** rebuilds after a code change, because dependencies are cached in their own layer.

**Memory.** Non-heap memory (metaspace, code cache, threads) turned out to be about 240 MB, while the app's live heap fits in about 50 MB. The heap is set to **46%** of the container (`-XX:MaxRAMPercentage=46`), leaving about 37 MB of headroom. The initial 60% setting looked reasonable but was unsafe: 307 MB of heap plus 240 MB of non-heap exceeds the limit, which ends in a kernel kill (exit 137).

**Startup.** At 0.1 CPU, the JIT compiler competes with Spring for the same sliver of CPU. One flag (`-XX:TieredStopAtLevel=1`) cut local startup from **155 s to 87 s**. On Render it starts in about 60 to 70 s, unchanged after adding the AWS SDK (only the EventBridge module, with a lightweight HTTP client). Class Data Sharing is the documented next lever if startup ever needs to go further.

**Publishing cost.** Publishing runs on the request thread after commit. A warm create takes about **0.4 s** instead of 0.18 s, and the first create after a start pays about **3 s** of one-time SDK setup.

### The Lambdas

**SLA engine (256 MB, arm64).** A cold run takes about **2.3 s**, almost all of it opening the first connection (fetching the password from SSM, then TLS through Supabase's pooler). Warm runs take **20 to 40 ms** because the connection is reused. At 128 MB the cold run doubled for the same cost, so 256 MB stays.

**Notification handler (128 MB, arm64).** About **0.5 s** of cold init and **0.3 s** for a cold run.

---

## Run it locally

### The API

Requirements: Docker Desktop.

```bash
git clone https://github.com/jshvega/incident-tracker.git
cd incident-tracker
cp api/.env.example api/.env    # then set JWT_SECRET to any long random Base64 string
docker compose up --build
```

This starts Postgres 17, loads the schema and seed data from `/db`, and starts the API on `http://localhost:8080` once the database is healthy. Event publishing is off by default (`events.enabled=false`), so no AWS account is needed; the API logs that publishing is disabled instead.

Seeded dev users (one admin, two assignees, two reporters) are listed in [`db/seed_dev.sql`](db/seed_dev.sql). They all use the dev-only password `hello123`.

Try it:

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<seeded user>","password":"<dev password>"}' | jq -r .token)

curl localhost:8080/incidents -H "Authorization: Bearer $TOKEN"
```

To reset the local database to its seed state: `docker compose down -v && docker compose up`.

### The SLA engine

Requirements: Python 3.12, and the database from the step above running (`docker compose up -d db` is enough).

```bash
cd lambdas/sla
python -m venv .venv
source .venv/bin/activate            # Windows: .venv\Scripts\Activate.ps1
pip install -r requirements-dev.txt
cp .env.example .env                 # fill in the local database values from docker-compose.yml
```

Load the fixture, which **replaces all incidents** with eight test cases (one per scenario: overdue at each severity, not yet due, resolved, closed, already breached), then run the sweep:

```bash
docker compose exec -T db psql -U admin -d incident_tracker < fixtures/sweep_cases.sql
python sweep.py
```

On Windows PowerShell, load the fixture with `Get-Content fixtures/sweep_cases.sql | docker compose exec -T db psql -U admin -d incident_tracker`.

The script calls the handler twice in one process, the way a warm Lambda would be invoked twice. The first run escalates three incidents and flags the critical one. The second finds nothing to do and reuses the same connection.

### The notification handler

The handler is split into a pure function (event in, S3 key and summary out) and a thin wrapper that writes to S3, so the logic runs locally with no AWS:

```bash
cd lambdas/notify
python notify.py events/critical.json    # prints the key and the summary
python notify.py events/v2.json          # an unsupported schema version raises on purpose
```

### The AWS infrastructure

Requires an AWS account and credentials. Build both Lambda zips, provide the database password (it is never stored in state), then plan and apply:

```bash
python lambdas/sla/build.py
python lambdas/notify/build.py
cd infra
cp terraform.tfvars.example terraform.tfvars   # database host, port, user, name
export TF_VAR_db_password='<sla_engine password>'
terraform init
terraform plan
terraform apply
```

---

## Deployment

| Setting | Value |
|---|---|
| API platform | Render, Docker runtime, built from this repo (root directory `api`) |
| Region | Virginia (Render) and `us-east-1` (AWS), the same region as the Supabase database |
| Health check | `/actuator/health/liveness` |
| API environment variables | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`, `EVENTS_ENABLED=true`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` |
| AWS | Terraform in `infra/`: both Lambdas, their roles and log groups (14-day retention), the schedule, the bus and rule, the dead-letter queue, the S3 bucket, the SSM parameter, and the publisher's IAM user |

Secrets exist only as environment variables on Render and as an encrypted SSM parameter on AWS. Nothing secret is in the image, the repository, or Terraform state. The `prod` profile sets INFO logging, turns SQL logging off, and sizes the connection pool and thread count for a 0.1 CPU instance.

**Cost:** observed **$0** per month. Everything runs inside free allowances, except EventBridge custom events at $1 per million, which at this volume is a fraction of a cent.

---

## Known limitations

These are deliberate tradeoffs of a zero-cost deployment, or work that is planned but not done yet.

- **Cold starts.** The free instance sleeps after 15 idle minutes; the next request takes about 75 seconds.
- **No production memory metrics.** Render's free tier doesn't expose them, so memory was measured locally at identical limits.
- **No token revocation.** Covered under design decisions.
- **One column written by both halves.** The API and the SLA engine both write an incident's severity, and Hibernate rewrites every mapped column on save. An API save that overlaps a sweep could silently undo an escalation. The window is milliseconds; the fix, optimistic locking with a `@Version` column, is planned.
- **A dropped connection costs one run.** If the SLA engine's cached connection dies between invocations, that run fails and the next one reconnects. Retrying once after reconnecting is the upgrade if it ever matters.
- **A failed publish loses one notification.** Covered under design decisions; a transactional outbox is the fix.
- **Notifications are archived, not delivered.** The S3 summary stands in for a real channel (Slack, SES).
- **A long-lived AWS key on Render.** Scoped to one action on one bus, but OIDC or role federation is the production answer.
- **Every Lambda build redeploys.** The zip isn't byte-identical between builds, so each rebuild forces one cold start even with no code change.
- **Terraform state is local.** Remote state comes with plan and apply in CI.
- **Test coverage is focused, not complete.** The API's state machine, the service's role and ownership rules, and the event publisher are unit tested, and CI runs them on every push. The SLA engine is verified against a fixture covering every case, plus repeated and overlapping runs. Automated tests for the security wiring and for the Lambdas are planned.
- **No frontend yet.**

---

## Roadmap

Done: the API, auth and roles, the production deploy, the SLA engine on Lambda on a schedule, event-driven notifications, all AWS infrastructure in Terraform, and CI on every push.

Planned, in order:

1. **LocalStack:** the AWS layer runnable and testable locally, with the Scheduler emulation gap documented.
2. **Next.js frontend** on Vercel.
3. **Full CI/CD with GitHub Actions:** image build to ghcr.io, deploy, Terraform plan and apply with remote state.
4. **Kubernetes manifests**, tested on a local kind cluster.

Future work: a transactional outbox for events, OIDC for the API's AWS access, real notification delivery, deterministic Lambda builds, optimistic locking on incidents, retry-once on reconnect in the SLA engine, a re-escalation ladder for incidents that stay breached, and an `updated_at` trigger in the database.

---

## How this was built

This project is structured as deliberate practice. I started it with SQL and React experience and no hands-on Java, Spring, Python, Docker, or cloud deployment. It is built in phases, and each phase follows the same loop: learn the concepts, decide the design with written tradeoffs, write the code, then rebuild the key pattern from a blank file without references.

I wrote the project's code myself, with AI as a tutor and reviewer: it pointed me to official docs, questioned my decisions, reviewed what I wrote, and helped draft the documentation.

`PROGRESS.md` logs each phase: what was built, what was hard, and what clicked.