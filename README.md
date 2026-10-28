# Incident Tracker

A role-based incident management system for platform and SRE teams. The API is built with **Java 21 and Spring Boot**, containerized with Docker, and deployed on Render with PostgreSQL on Supabase. A **Python SLA engine** finds incidents that missed their deadline and escalates them automatically.

Users report incidents, assign them, and move them through a lifecycle (`open → investigating → resolved → closed`). Every action is checked against the user's role, and every status change is recorded in an audit history. When an incident misses its deadline, the SLA engine raises its severity and records the escalation, with no human involved.

**Status:** the synchronous core (data model, REST API, auth, containerization, production deploy) is complete and live. The SLA engine is built and verified locally against Postgres; running it on AWS Lambda on a schedule is the next step. See [Roadmap](#roadmap).

**Live API:** https://incident-tracker-5zpj.onrender.com
> Free hosting: the first request after 15 idle minutes takes about 75 seconds while the service wakes up. Every request after that takes about 0.2 seconds. See [Known limitations](#known-limitations).

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

- **Finds breached incidents:** open or investigating incidents past their `due_at` that haven't been flagged yet.
- **Escalates on a fixed ladder:** low → high, medium → high, high → critical. Critical incidents are flagged without a severity change.
- **Records what it did:** sets `sla_breached` on the incident and writes an `escalations` row (previous severity, new severity, reason).
- **Leaves the lifecycle alone:** status and assignee belong to the API and its rules.
- **Is safe to run any number of times:** see [Design decisions](#design-decisions).
- **Logs structured JSON:** one line per incident and one summary per run, all sharing a `run_id`, so a single run can be pulled up on its own:

```json
{"level": "INFO", "msg": "sweep_complete", "run_id": "e27c064ce2d440fb8250b09cdb8789b5", "candidates": 4, "escalated": 3, "breach_only": 1, "skipped": 0, "failed": 0, "duration_ms": 46}
```

---

## Tech stack

| Layer | Choice |
|---|---|
| API | Java 21, Spring Boot 4, Spring Data JPA (Hibernate 7) |
| Security | Spring Security, a custom JWT filter (jjwt, HS256), BCrypt |
| SLA engine | Python 3.12, pg8000 (pure-Python Postgres driver), standard-library logging |
| Database | PostgreSQL 17 (Supabase in production, Docker Compose locally) |
| Build | Maven (API), pip with pinned requirements (SLA engine) |
| Container | Multi-stage Docker image (Eclipse Temurin 21 JRE, non-root) |
| Hosting | Render (free web service, 512 MB RAM, 0.1 CPU) |
| Tests | JUnit 5 and Mockito (API), fixture-driven verification (SLA engine) |

---

## Repository layout

```
api/              Spring Boot API
lambdas/sla/      SLA engine, with a test fixture in fixtures/
db/               SQL schema, migrations and dev seed data, loaded by Docker Compose
docker-compose.yml
PROGRESS.md       a log of each build phase
```

---

## Design decisions

Each decision was made before the code was written, with the alternatives and tradeoffs documented. The most interesting ones:

### The split

**Synchronous work in Java, scheduled work in Python.** The API answers requests; the SLA engine runs on a timer. Keeping scheduled work out of Spring (no `@Scheduled` anywhere) keeps the API a pure request handler and lets the sweep run serverless. The two halves share a database, not a call graph: the sweep talks to Postgres directly and never calls the API.

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

**Lambda-shaped from the start.** The sweep is a `handler(event, context)`, with the database connection opened outside it, so warm invocations reuse one connection instead of opening a new one every time.

---

## Performance: fitting Spring Boot into 512 MB and 0.1 CPU

The free tier is small, so this was measured rather than guessed. All numbers come from runs at production limits (`--memory=512m --cpus=0.1`, prod profile).

**Image size and build speed.** A naive single-stage image was 1.13 GB and re-downloaded every dependency on any code change (about 30 s). The multi-stage build ships only the JRE and the jar: **584 MB**, with **7.5 s** rebuilds after a code change, because dependencies are cached in their own layer.

**Memory.** Non-heap memory (metaspace, code cache, threads) turned out to be about 240 MB, while the app's live heap fits in about 50 MB. The heap is set to **46%** of the container (`-XX:MaxRAMPercentage=46`), leaving about 37 MB of headroom. The initial 60% setting looked reasonable but was unsafe: 307 MB of heap plus 240 MB of non-heap exceeds the limit, which ends in a kernel kill (exit 137).

**Startup.** At 0.1 CPU, the JIT compiler competes with Spring for the same sliver of CPU. One flag (`-XX:TieredStopAtLevel=1`) cut local startup from **155 s to 87 s**. On Render it starts in about 60 to 70 s. Class Data Sharing is the documented next lever if startup ever needs to go further.

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

This starts Postgres 17, loads the schema and seed data from `/db`, and starts the API on `http://localhost:8080` once the database is healthy.

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

---

## Deployment

| Setting | Value |
|---|---|
| Platform | Render, Docker runtime, built from this repo (root directory `api`) |
| Region | Virginia, the same region as the Supabase database |
| Health check | `/actuator/health/liveness` |
| Environment variables | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod` |

Secrets exist only as environment variables on Render. Nothing secret is in the image or the repository. The `prod` profile sets INFO logging, turns SQL logging off, and sizes the connection pool and thread count for a 0.1 CPU instance.

The SLA engine is not deployed yet. It will run on AWS Lambda (see [Roadmap](#roadmap)).

---

## Known limitations

These are deliberate tradeoffs of a zero-cost deployment, or work that is planned but not done yet.

- **Cold starts.** The free instance sleeps after 15 idle minutes; the next request takes about 75 seconds.
- **Database pausing.** Supabase pauses free projects after 7 days without queries. Once the SLA engine runs on a schedule in AWS, its queries keep the database awake. Until then, this is handled manually.
- **No production memory metrics.** Render's free tier doesn't expose them, so memory was measured locally at identical limits.
- **No token revocation.** Covered under design decisions.
- **One column written by both halves.** The API and the SLA engine both write an incident's severity, and Hibernate rewrites every mapped column on save. An API save that overlaps a sweep could silently undo an escalation. The window is milliseconds; the fix, optimistic locking with a `@Version` column, is planned.
- **A dropped connection costs one run.** If the SLA engine's cached connection dies between invocations, that invocation fails and the next one reconnects. Retrying once after reconnecting is the upgrade if it ever matters.
- **Test coverage is focused, not complete.** The API's state machine and the service's role and ownership rules are unit tested. The SLA engine is verified against a fixture covering every case, plus repeated and overlapping runs. Automated tests for the security wiring and for the SLA engine are planned with CI.
- **No frontend yet.**

---

## Roadmap

The core principle behind the full design: **synchronous, business-facing work runs in Java; scheduled and asynchronous work runs serverless on AWS.** What exists today is the synchronous half, plus the SLA engine running locally.

```
Today
-----
Client ──HTTPS/JWT──> Spring Boot API ──> PostgreSQL <── SLA engine (Python, run locally)

Next
----
EventBridge Scheduler ──every N minutes──> SLA Lambda (the same code) ──> PostgreSQL
```

Planned, in order:

1. **AWS:** the SLA engine on Lambda, first by hand, then entirely in **Terraform**, triggered by **EventBridge Scheduler**.
2. **Event-driven notifications:** the API publishes an event when a critical incident is created; a second Lambda processes it.
3. **LocalStack:** the AWS layer runnable and testable locally.
4. **Next.js frontend** on Vercel.
5. **CI/CD with GitHub Actions:** tests, image build to ghcr.io, deploy.
6. **Kubernetes manifests**, tested on a local kind cluster.

---

## How this was built

This project is structured as deliberate practice. I started it with SQL and React experience and no hands-on Java, Spring, Python, Docker, or cloud deployment. It is built in phases, and each phase follows the same loop: learn the concepts, decide the design with written tradeoffs, write the code, then rebuild the key pattern from a blank file without references.

I wrote the project's code myself, with AI as a tutor and reviewer: it pointed me to official docs, questioned my decisions, reviewed what I wrote, and helped draft the documentation.

`PROGRESS.md` logs each phase: what was built, what was hard, and what clicked.