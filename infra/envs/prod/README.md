# Production environment

## Runtime shape

- Region: `ap-northeast-2`
- Application: ECS Fargate ARM64, `0.5 vCPU / 2 GiB` per task
- Availability: two-task floor with availability-zone rebalancing enabled
- Scaling: 2–8 tasks, CPU target 60%, ALB target 2,340 requests/minute/task
- Data: private PostgreSQL 16 RDS and TLS-enabled Redis 7.1 ElastiCache
- Edge: HTTPS ALB and Route53 at `api.peakda.com`
- Backups: one-day RDS automated retention plus encrypted custom dumps with a
  30-day S3 lifecycle

The AWS free-plan account limits RDS automated retention to one day. Redis 7.1
is the newest engine version currently offered to this account in Seoul.

## Bootstrap and deployment

Create infrastructure without application tasks first:

```sh
cd infra/envs/prod
cp terraform.tfvars.example terraform.tfvars
# Set alert_email, github_allowed_subjects and real OAuth/CORS values.
terraform init
terraform apply -var='desired_count=0' -var='min_capacity=0'
```

Populate every `/peakda/prod/*` SecureString, push an ARM64 image, and then keep
two tasks running:

```sh
terraform apply \
  -var='image_tag=<immutable-tag>' \
  -var='desired_count=2' \
  -var='min_capacity=2'
```

Application revisions are deployed by `.github/workflows/deploy-prod.yml`.
Terraform owns the service shape while the workflow registers immutable task
definition revisions and performs the rolling update.

## Database operations

`migration_task_definition_arn` identifies the one-off Fargate database task.
It reaches private RDS from inside the VPC and can run in two explicit modes:

- Dev-to-prod migration: restore a reviewed custom dump, remove spots whose
  Korean name contains `테스트` and their dependent data, verify references,
  and upload the pre-restore dump and sanitization log to encrypted S3.
- Hash-pinned SQL job: download a SQL artifact from the private migration
  prefix, verify `DATABASE_JOB_SHA256`, and run as `READ_ONLY` or as `APPLY`
  with the additional `DATABASE_JOB_CONFIRM=PROD_DATABASE_APPLY` guard.

Generated dumps, SQL payloads, Figma exports, Terraform variable files, state,
and plans are ignored. The reusable migration scripts and Terraform lock file
remain versioned because they contain no environment data or credentials.

## Portfolio performance baseline

These numbers are deliberately separated into capacity hypotheses and test
targets. Do not report a target as a measured result.

| Metric | Current hypothesis or target | Basis |
| --- | ---: | --- |
| Base scaling threshold | about 78 RPS total | 2 tasks × 2,340 requests/minute/task ÷ 60 |
| Eight-task scaling threshold | about 312 RPS total | 8 tasks × 39 RPS/task; not a proven maximum |
| Steady read target | 100 RPS for 15 minutes | p95 < 300 ms, p99 < 700 ms, errors < 1% |
| Promotional spike target | 200 RPS for 5 minutes | p95 < 500 ms, errors < 1%, scale-out verified |
| Transactional write target | 20 TPS for 10 minutes | p95 < 500 ms, errors < 1%, no pool exhaustion |
| Recovery target | one task loss without outage | healthy target remains; replacement becomes healthy within 180 seconds |

The first load-test report must record the image tag, task count, endpoint mix,
test duration, achieved RPS/TPS, p50/p95/p99, error rate, CPU, memory, DB
connections, Redis latency, and scale-out timestamps. RDS `db.t4g.micro` is the
expected first bottleneck, so measured results must be interpreted with burst
credits and connection-pool saturation visible.
