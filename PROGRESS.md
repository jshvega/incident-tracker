
## Toolchain
Windows 11 / AMD64
- JDK: Temurin 21.0.12 (LTS)
- Node: v22.19.0 / npm 11.17.0
- Python: 3.12 available via `py -3.12` (3.13.7 on PATH)
- git: 2.55.0.windows.3
- Docker Desktop: 29.7.2
- AWS CLI: 2.36.21
- Terraform: v1.15.8
- kubectl: v1.36.1 (Kustomize v5.8.1)
- kind: v0.32.0


---


## Phase 0

### 2026-08-17 — Repo initialized
- Layout per Master Plan §3: `api/`, `web/`, `lambdas/`, `infra/`, `k8s/`, `db/`
- Empty dirs held by `.gitkeep`, removed once real content lands
- `.gitignore` covers Java, Node, Python, Terraform before any of it exists, so `.tfstate` and `.env` are never in a state where they could be staged
- `.gitattributes` normalizes line endings to LF (Windows local, Linux CI)

### 2026-08-19 — AWS account
- **Region: `us-east-1`.** Chosen over `mx-central-1` despite being further from Mexico City. Broadest service coverage and every Terraform example assumes it. Latency is irrelevant for an async job queue. Every ARN carries the region, and resources created in the wrong one are invisible from the right one.
- **Free Plan**, not Paid. Hard ceiling against a surprise bill.
- **Plan expires ~2027-02-19** (six months from signup, or sooner if the $200 credit runs out). Account auto-closes after that with a 90-day grace period to retrieve data. Everything deployed disappears. Decide by 2027-01-19 whether to upgrade or tear down.
- Root locked with MFA, unused after setup. Daily work via IAM user in an `admins` group with `AdministratorAccess`.
- IAM billing access activated (root-only toggle, off by default; without it even an admin user gets denied on the billing console).
- $1 monthly budget with an email alert. Credits should cover everything, so if it fires, something is running that shouldn't be. Billing data lags ~24h, so it's a tripwire, not a circuit breaker.
- CLI profile: `incident-tracker`, region `us-east-1`, output `json`. Verify with `aws sts get-caller-identity --profile incident-tracker`.

### Open
- Credits: 3 of 5 onboarding tasks left (EC2 launch+terminate, RDS, Bedrock prompt) for the second $100. Budget and Lambda covered.
- Consider IAM Identity Center (`aws sso login`) to replace the long-lived access key with short-lived credentials.
- `docker-compose.yml` still empty, fills in with Postgres + LocalStack.


---


## Phase 1

### Built
- Lambdas/sla_sweep_query.sql - the sweep query, EXPLAIN ANALYZE confirmed Index Scan using idx_incidents_due_at_unresolved.
- db/checks.sql, verification queries (incident state dump).

### Reps 
(a) three-table library schema from blank file, enum + 2 FKs + CHECK + partial index, ran clean, dropped. 
(b) three-table join with filter and aggregate. 
Teach-aloud done.

### What was hard
- Rep (a) reproduced the NOT NULL + ON DELETE SET NULL contradiction from the original schema — the exact mistake, unprompted. That's the one to watch for.
- Rep (a) also got the partial index backwards: indexed the enum column and predicated on the same enum. The pattern is sort on the range-queried column, filter on the status.
- Rep (b) joined users via comments.author_id instead of incidents.reporter_id — plausible result, wrong question. users reaches incidents by two FKs and the choice determines meaning.
- Aggregates can't go in WHERE; HAVING filters after grouping.
- Teach-aloud stalled on why status_history/escalations are tables not columns, and on reciting the three decisions cold.

### Gotchas
- Seed data ages after insertion. Timestamps freeze at INSERT; not-yet-due rows cross their deadlines as real time passes. Re-seed when test cases need to be meaningful.
- A partial index is used only when the query's conditions imply the index predicate. Narrower is fine; wider means the index is skipped.
- EXPLAIN ANALYZE shows estimated vs actual rows. A fresh table has no statistics, ANALYZE <table> updates them.


---


## Phase 2

### Built
Spring Boot 4.1.1 project in /api; GET /incidents returning six rows from Supabase through controller → service → repository → Hibernate.

### Decisions
- Maven: The Spring Initializr default
- Session pooler: Spring is one long-lived instance
- ddl-auto as validate: SQL owns the schema, Hibernate only checks the entity matches at startup
- Layered packages: Convention
- DTOs from the first endpoint rather than Phase 3: Doing it now means Phase 3 grows instead of being rewritten

*These are one-line reasons

### Reps 
(a) Blank project, hardcoded endpoint, ran clean, one typo caught from the compiler (RequestController vs RestController)
(b) Teach-aloud, done after building the flow diagram

### What was hard
- The enum case mismatch
- Java fundamentals (record syntax, method signatures, object.method() vs static)
- Wrapping my head around the big picture concepts and how it all connects (very limited prior experience with Java, Spring Boot and Postgres)

### What clicked
- The layering rule (Jackson knows JSON↔DTO, Hibernate knows entity↔table, the service is the only thing that knows both)
- auto-configuration as conditional rules you can switch off

### Gotchas
- Validate checks column names and broad types only. Not enum values, not nullability. The enum mismatch passed startup and blew up on the first read.
- Java enum constants are lowercase here on purpose, mirroring the Postgres enum. Phase 6's Python must match too.
- Boot 4 renamed starters and packages: spring-boot-starter-webmvc, org.springframework.boot.jdbc.autoconfigure. Most tutorials show Boot 3 names.
- $env:VAR is per-terminal-session. New terminal, no variable, auth failure.
- Pooler auth failures always echo user postgres regardless of the real cause. Check username and password.
- The four-layer error index: javac (layers disagree), Hikari/Postgres (can't connect), Hibernate validate (entity vs schema), runtime (data can't convert).


---


## Phase 3

### Built
- Four remaining entities (User, StatusHistory, Comment, Escalation) and their repositories.
- Unidirectional LAZY @ManyToOne on all seven FKs; open-in-view disabled.
- Centralized error handling: @RestControllerAdvice returning ProblemDetail (404, 409, 400).
- Full CRUD: GET list, GET detail, POST create, PATCH update, PATCH assignee, DELETE.
- Bean Validation on request DTOs, separate from response DTOs.
- State machine as a plain class: open → investigating | closed, investigating → resolved,
  resolved → closed | investigating, closed terminal.
- POST /incidents/{id}/transitions, writing status_history in the same transaction.
- Comments: POST and GET.
- Tests: 9 state machine (pure JUnit), 3 service (Mockito).

### Decisions
- X-User-Id header as the temporary acting-user seam; Phase 4 replaces it.
- Transitions on their own endpoint, so the general edit cannot touch status.
- open → closed allowed for false alarms; admin-only gating deferred to Phase 4.
- Hard delete with FK cascade; soft delete is the production answer, documented not implemented.
- sla_minutes not editable yet; editing it must recompute due_at from created_at.
- Comments and history as separate endpoints, not embedded in the detail response.

### Reps
- (a) State machine + exception + test from a blank file.
- (b) Service + repository interface + Mockito test from a blank file.

### What was hard / what clicked
- Had one User-instead-of-Role crash: compiled fine, failed at getEnumConstants.
- Atomicity experiment: what you predicted vs what you saw.
- Both reps had vacuous assertion lessons.
- Getting the overall map of the concepts and how each piece works together. Repetition was key.

### Open items
- Three flush() calls not yet fully justified. I added them for safety, but certain if they are needed.
- setIncidentTitle / setIncidentDescription naming.
- Test 2 uses open → closed, which is legal in the real machine.