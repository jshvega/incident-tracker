# Incident Tracker

A role-based incident management API for platform and SRE teams, built with **Java 21 and Spring Boot**, containerized with Docker, and deployed on Render with PostgreSQL on Supabase.

Users report incidents, assign them, and move them through a lifecycle (`open → investigating → resolved → closed`). Every action is checked against the user's role, and every status change is recorded in an audit history.

**Status:** the synchronous core (data model, REST API, auth, containerization, production deploy) is complete and live. It is also the foundation for a larger system: a serverless SLA engine on AWS that escalates overdue incidents automatically. See [Roadmap](#roadmap).

**Live API:** https://incident-tracker-5zpj.onrender.com
> Free hosting: the first request after 15 idle minutes takes about 75 seconds while the service wakes up. Every request after that takes about 0.2 seconds. See [Known limitations](#known-limitations).

---

## What it does

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

---

## Tech stack

| Layer | Choice |
|---|---|
| Language / framework | Java 21, Spring Boot 4, Spring Data JPA (Hibernate 7) |
| Security | Spring Security, a custom JWT filter (jjwt, HS256), BCrypt |
| Database | PostgreSQL 17 (Supabase in production, Docker Compose locally) |
| Build | Maven |
| Container | Multi-stage Docker image (Eclipse Temurin 21 JRE, non-root) |
| Hosting | Render (free web service, 512 MB RAM, 0.1 CPU) |
| Tests | JUnit 5 and Mockito |

---

## Design decisions

Each decision was made before the code was written, with the alternatives and tradeoffs documented. The most interesting ones:

**A custom JWT filter instead of Spring's OAuth2 resource server.** The resource server would have hidden the exact mechanism this project set out to build: read the header, verify the signature, check expiry, and populate the security context. In production I would use the resource server. Here I wanted to own the mechanism.

**Authorization lives in the service layer.** Rules that apply to a whole endpoint (only admins delete) live in the security config as URL rules. Rules that depend on the data (is this user the reporter of this incident?) live in the service, where the data is. The state machine itself knows which roles may make each transition.

**Stateless tokens, with honest tradeoffs.** Tokens last 60 minutes with no refresh, and the role travels inside the token. That means no session store, but also no instant revocation: a role change takes effect at the user's next login. That is acceptable at this scale and a known limitation at a larger one.

**One JWT secret per environment.** A token minted by the local stack is rejected by production (verified). Sharing a secret would let anyone who can create an admin locally mint a token production accepts.

**Development never touches production data.** Locally, the app runs against Postgres in Docker Compose, built from scratch from the SQL files in `/db`. Supabase is production only. Hibernate runs in `validate` mode, so any drift between the SQL files and the entities stops the app from booting.

**A liveness health check that ignores the database.** Render restarts the service when its health check fails. A check that included the database would restart a healthy app every time the database blipped, which fixes nothing.

---

## Performance: fitting Spring Boot into 512 MB and 0.1 CPU

The free tier is small, so this was measured rather than guessed. All numbers come from runs at production limits (`--memory=512m --cpus=0.1`, prod profile).

**Image size and build speed.** A naive single-stage image was 1.13 GB and re-downloaded every dependency on any code change (about 30 s). The multi-stage build ships only the JRE and the jar: **584 MB**, with **7.5 s** rebuilds after a code change, because dependencies are cached in their own layer.

**Memory.** Non-heap memory (metaspace, code cache, threads) turned out to be about 240 MB, while the app's live heap fits in about 50 MB. The heap is set to **46%** of the container (`-XX:MaxRAMPercentage=46`), leaving about 37 MB of headroom. The initial 60% setting looked reasonable but was unsafe: 307 MB of heap plus 240 MB of non-heap exceeds the limit, which ends in a kernel kill (exit 137).

**Startup.** At 0.1 CPU, the JIT compiler competes with Spring for the same sliver of CPU. One flag (`-XX:TieredStopAtLevel=1`) cut local startup from **155 s to 87 s**. On Render it starts in about 60 to 70 s. Class Data Sharing is the documented next lever if startup ever needs to go further.

---

## Run it locally

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

---

## Deployment

| Setting | Value |
|---|---|
| Platform | Render, Docker runtime, built from this repo (root directory `api`) |
| Region | Virginia, the same region as the Supabase database |
| Health check | `/actuator/health/liveness` |
| Environment variables | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod` |

Secrets exist only as environment variables on Render. Nothing secret is in the image or the repository. The `prod` profile sets INFO logging, turns SQL logging off, and sizes the connection pool and thread count for a 0.1 CPU instance.

---

## Known limitations

These are deliberate tradeoffs of a zero-cost deployment, or work that is planned but not done yet.

- **Cold starts.** The free instance sleeps after 15 idle minutes; the next request takes about 75 seconds.
- **Database pausing.** Supabase pauses free projects after 7 days without queries. Until the SLA engine exists (it queries the database every few minutes), this is handled manually.
- **No production memory metrics.** Render's free tier doesn't expose them, so memory was measured locally at identical limits.
- **No token revocation.** Covered under design decisions.
- **Test coverage is focused, not complete.** The state machine and the service's role and ownership rules are unit tested. Tests of the security wiring itself (401/403 responses, URL rules) are planned with CI.
- **API only.** There is no frontend yet.

---

## Roadmap

The core principle behind the full design: **synchronous, business-facing work runs in Java; scheduled and asynchronous work runs serverless on AWS.** What exists today is the synchronous half.

```
Today                                   Next
-----                                   ----
Client ──HTTPS/JWT──> Spring Boot API   EventBridge Scheduler ──> SLA Lambda (Python)
                          │                                           │
                          v                                           v
                     PostgreSQL  <────────────────────────── escalates overdue incidents
```

Planned, in order:

1. **SLA engine:** a Python job that finds incidents past their deadline and escalates them, built and verified locally first.
2. **AWS:** the SLA engine on Lambda, first by hand, then entirely in **Terraform**, triggered by **EventBridge Scheduler**.
3. **Event-driven notifications:** the API publishes an event when a critical incident is created; a second Lambda processes it.
4. **LocalStack:** the AWS layer runnable and testable locally.
5. **Next.js frontend** on Vercel.
6. **CI/CD with GitHub Actions:** tests, image build to ghcr.io, deploy.
7. **Kubernetes manifests**, tested on a local kind cluster.

---

## How this was built

This project is structured as deliberate practice. I started it with SQL and React experience and no hands-on Java, Spring, Docker, or cloud deployment. It is built in phases, and each phase follows the same loop: learn the concepts, decide the design with written tradeoffs, write the code, then rebuild the key pattern from a blank file without references.

I wrote the project's code myself, with AI as a tutor and reviewer: it pointed me to official docs, questioned my decisions, reviewed what I wrote, and helped draft the documentation.

`PROGRESS.md` logs each phase: what was built, what was hard, and what clicked.