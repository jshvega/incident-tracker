
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