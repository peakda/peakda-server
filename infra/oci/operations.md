# peakda prod — OCI 서버 운영

AWS 무료 플랜 크레딧이 2026-10-14 전후로 소진되어 prod 를 OCI Always Free 서버 한 대로 옮긴다.

| 항목 | 값 |
|---|---|
| 서버 | OCI 오사카 `VM.Standard.A1.Flex` 2 OCPU / 12GB (ARM64), Ubuntu 24.04, 부트 200GB |
| 같은 서버의 다른 서비스 | 마인크래프트(최대 5GB). peakda prod 는 compose 메모리 상한 합계 2,560MB |
| 구성 | Caddy(HTTPS) · 앱 · PostgreSQL 16 · Redis · Alloy(로그 수집), `/opt/peakda` (ubuntu 소유, 750) |
| 로그 | Alloy → Grafana Cloud Loki(dev 와 같은 스택, `env="prod"`). 아래 "로그 보기" |
| 이미지 | `ghcr.io/peakda/peakda-server:<커밋 SHA>` (ARM64) |
| 배포 | `.github/workflows/deploy-prod-oci.yml` → SSH(배포 전용 키, `deploy-gate.sh` 로만 실행) → `deploy.sh` |
| 미디어 | OCI Object Storage `peakda-prod-media`(공개 읽기). `cdn.peakda.com` → Caddy → `media-cache`(nginx 디스크 캐시 2GB) → Object Storage |
| 백업 | 6시간마다 PostgreSQL 덤프·Redis 스냅샷 → `peakda-prod-backup`(30일 보관) |
| DNS | 가비아 DNS (등록처와 같은 곳) |

`/opt/peakda` 파일

| 파일 | 출처 | 비고 |
|---|---|---|
| `docker-compose.yml`, `Caddyfile`, `media-cache.conf.template`, `alloy-config.alloy`, `backup.sh` | 배포 때 그 커밋에서 자동 갱신 | |
| `deploy.sh`, `deploy-gate.sh` | 수동 복사 | 실행 중 덮어쓰지 않기 위해 자동 갱신하지 않는다 |
| `.env` | `render-env.sh` 로 생성 | 권한 600. `APP_IMAGE` 줄만 `deploy.sh` 가 관리한다 |
| `data/`, `logs/` | 서버 | PostgreSQL·Redis 데이터, 백업 로그 |

Always Free 범위를 지킨다. 새 자원을 만들 때는 예산 알림과 무료 한도(블록 스토리지 200GB, Object Storage 20GB·월 5만 요청, A1 4 OCPU·24GB 합계)를 먼저 확인한다.

## 1. 초기 설정 (다운타임 없음)

명령은 저장소 루트에서 실행한다. `HOST=129.225.200.201`

### 1-1. 서버 파일

```sh
ssh ubuntu@$HOST 'mkdir -p /opt/peakda/data /opt/peakda/logs'
scp infra/oci/{deploy.sh,deploy-gate.sh,backup.sh,docker-compose.yml,Caddyfile,media-cache.conf.template,alloy-config.alloy} ubuntu@$HOST:/opt/peakda/
ssh ubuntu@$HOST 'chmod 750 /opt/peakda/*.sh'
```

### 1-2. 배포 전용 SSH 키

키는 `deploy-gate.sh` 만 실행할 수 있다(`restrict` 로 포워딩·터미널도 막는다).

```sh
ssh-keygen -t ed25519 -N '' -C peakda-deploy -f ~/.ssh/peakda_deploy
ssh ubuntu@$HOST "echo 'restrict,command=\"/opt/peakda/deploy-gate.sh\" $(cat ~/.ssh/peakda_deploy.pub)' >> ~/.ssh/authorized_keys"

# 호스트 키는 서버에서 직접 본 지문과 비교한 뒤 등록한다
ssh ubuntu@$HOST 'ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub'
ssh-keyscan -t ed25519 $HOST 2>/dev/null | tee /dev/stderr | ssh-keygen -lf -

gh secret set OCI_DEPLOY_SSH_KEY --env production < ~/.ssh/peakda_deploy
ssh-keyscan -t ed25519 $HOST 2>/dev/null | gh secret set OCI_SSH_KNOWN_HOSTS --env production
gh variable set OCI_HOST --env production --body $HOST
rm ~/.ssh/peakda_deploy   # GitHub 에만 남긴다
```

### 1-3. Object Storage

```sh
NS=$(oci os ns get --query data --raw-output)
C=<compartment OCID>
oci os bucket create -c $C --name peakda-prod-media --public-access-type ObjectRead
oci os bucket create -c $C --name peakda-prod-backup
oci os object-lifecycle-policy put -bn peakda-prod-backup --force --items \
  '[{"name":"expire-30d","action":"DELETE","timeAmount":30,"timeUnit":"DAYS","isEnabled":true}]'
# S3 호환 키. 사용자당 2개까지라 이름을 남겨 둔다. secret 은 이때 한 번만 보인다.
oci iam customer-secret-key create --user-id <user OCID> --display-name peakda-prod-storage
```

미디어 복사와 대조 (AWS 계정이 닫히기 전에, 컷오버 직전에 한 번 더 `sync` 한다)

```sh
MEDIA=~/peakda-media    # 저장소 밖
aws s3 sync s3://peakda-prod-media-421438965126 $MEDIA
AWS_ACCESS_KEY_ID=<key> AWS_SECRET_ACCESS_KEY=<secret> AWS_REQUEST_CHECKSUM_CALCULATION=when_required \
  aws s3 sync $MEDIA s3://peakda-prod-media --region ap-osaka-1 \
  --endpoint-url https://$NS.compat.objectstorage.ap-osaka-1.oraclecloud.com
# 양쪽 개수·총 크기가 같아야 한다
aws s3 ls s3://peakda-prod-media-421438965126 --recursive --summarize | tail -2
oci os object list -bn peakda-prod-media --all --query 'length(data)'
```

### 1-4. `.env`

AWS prod SSM 값을 그대로 읽고 AWS 전용 항목만 바꾼다. `JWT_SECRET`·OAuth·FCM 키는 원래 값 그대로 옮겨야 한다.

```sh
OCI_STORAGE_ACCESS_KEY=<key> OCI_STORAGE_SECRET_KEY=<secret> ACME_EMAIL=<메일> \
  infra/oci/render-env.sh ~/peakda-oci.env ghcr.io/peakda/peakda-server:<배포할 커밋 SHA>
scp ~/peakda-oci.env ubuntu@$HOST:/opt/peakda/.env && ssh ubuntu@$HOST chmod 600 /opt/peakda/.env
rm ~/peakda-oci.env
```

`.env` 를 통째로 새로 쓰므로 나중에 붙인 값(운영 "Grafana Cloud 연결" 의 GRAFANA_CLOUD_*)은 다시 넣어야 한다.

### 1-5. 백업 cron

```sh
ssh ubuntu@$HOST '(crontab -l 2>/dev/null; echo "0 */6 * * * /opt/peakda/backup.sh >> /opt/peakda/logs/backup.log 2>&1") | crontab -'
```

### 1-6. DNS 를 가비아로 (전파에 최대 48시간)

1. 가비아 DNS 관리 툴에 Route53 레코드(`aws route53 list-resource-record-sets --hosted-zone-id Z0047731D5LTIA2YFLB8`)를
   **지금 대상 그대로** 옮긴다. `api` 는 ALB DNS 이름으로 CNAME, `cdn` 은 CloudFront 도메인으로 CNAME, apex·www 는 Vercel 값.
   ACM 검증용 CNAME 은 옮기지 않는다. TTL 은 가비아가 허용하는 가장 짧은 값으로 둔다(컷오버·롤백 반영 속도를 정한다)
2. 가비아에서 도메인 네임서버를 Route53 에서 가비아 DNS 로 바꾼다
3. `dig NS peakda.com +short` 가 가비아를 가리키고 `dig api.peakda.com` 이 그대로 ALB 로 풀리면 끝

## 2. 컷오버 (다운타임 20~30분)

데이터 이관은 `infra/scripts/migrate-data.sh`(PR #117)로 한다. 절차의 근거는 `infra/scripts/data-migration.md`.
RDS·ElastiCache 는 프라이빗이라 원본 쪽은 RDS 접근이 이미 허용된 dev EC2 에서 실행한다.

```sh
# 2-0. 서버에 PostgreSQL·Redis 만 먼저 띄운다 (앱은 아직)
ssh ubuntu@$HOST 'cd /opt/peakda && docker compose up -d postgres redis'

# 2-1. 원본 동결 + 안전망 (다운타임 시작)
aws application-autoscaling register-scalable-target --service-namespace ecs \
  --resource-id service/peakda-prod/peakda-prod --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 0 --max-capacity 0
aws ecs update-service --cluster peakda-prod --service peakda-prod --desired-count 0 > /dev/null
aws ecs wait services-stable --cluster peakda-prod --services peakda-prod
aws rds create-db-snapshot --db-instance-identifier peakda-prod --db-snapshot-identifier peakda-prod-pre-oci

# 2-2. 컷오버 동안만 dev EC2 가 ElastiCache 에 닿게 한다 (legacy 라 terraform 과 어긋나도 된다)
aws ec2 authorize-security-group-ingress --group-id <peakda-prod-redis-sg> \
  --protocol tcp --port 6379 --source-group <peakda-dev-app-sg>
```

2-3. dev EC2 에서 export (SSM Session Manager 로 접속)

```sh
git clone --depth 1 -b feature/117 https://github.com/peakda/peakda-server.git /tmp/mig
docker build -f /tmp/mig/infra/scripts/Dockerfile.data-migration --build-arg PG_MAJOR=16 \
  -t peakda-data-migration /tmp/mig/infra/scripts
# SOURCE_* 값은 data-migration.md "준비" 대로 로컬에서 구해 세션에 export 한다
docker run --rm --network host -v /tmp/out:/work/out -e SOURCE_PGHOST -e SOURCE_PGUSER -e SOURCE_PGPASSWORD \
  -e SOURCE_PGSSLMODE -e SOURCE_REDIS_URL peakda-data-migration export --dir /work/out
# 마지막 줄의 --expect-sums 값을 적어 둔다. 산출물은 S3 를 거쳐 로컬로 받는다
aws s3 cp /tmp/out s3://peakda-dev-storage-421438965126/postgres/oci-cutover/ --recursive
```

2-4. OCI 서버에서 import

```sh
aws s3 cp s3://peakda-dev-storage-421438965126/postgres/oci-cutover/ ~/peakda-mig/ --recursive
scp -r ~/peakda-mig ubuntu@$HOST:/opt/peakda/data/migration
ssh ubuntu@$HOST
  git clone --depth 1 -b feature/117 https://github.com/peakda/peakda-server.git /tmp/mig
  docker build -f /tmp/mig/infra/scripts/Dockerfile.data-migration --build-arg PG_MAJOR=16 \
    -t peakda-data-migration /tmp/mig/infra/scripts
  # .env 를 셸로 source 하지 않는다. 공백이 든 값(JAVA_OPTS)이 명령으로 실행된다.
  export TARGET_PGPASSWORD="$(sed -n 's/^SPRING_DATASOURCE_PASSWORD=//p' /opt/peakda/.env)"
  docker run --rm --network peakda_default -v /opt/peakda/data/migration:/work/out \
    -e TARGET_PGHOST=postgres -e TARGET_PGUSER=peakda -e TARGET_PGPASSWORD \
    -e TARGET_PGSSLMODE=disable -e TARGET_REDIS_URL=redis://redis:6379 \
    peakda-data-migration import --dir /work/out --expect-sums <2-3 의 값>
```

`PostgreSQL: 원본 지문과 일치`, `Redis 되읽기: Redis 일치` 가 나와야 한다. 아니면 멈추고 롤백한다.

```sh
# 2-5. dev EC2 에서 export 이후 원본에 쓰기가 없었는지 확인
docker run --rm --network host -v /tmp/out:/work/out -e SOURCE_PGHOST -e SOURCE_PGUSER -e SOURCE_PGPASSWORD \
  -e SOURCE_PGSSLMODE -e SOURCE_REDIS_URL peakda-data-migration verify --dir /work/out --side source
```

2-6. 가비아 DNS 에서 `api`·`cdn` 을 서버 IP 로 가는 A 레코드로 바꾼다.
2-7. GitHub Actions 에서 **Deploy Production (OCI)** 를 실행한다(`gh workflow run deploy-prod-oci.yml --ref main`).
     Caddy 가 이때 인증서를 받으므로 2-6 이 먼저다. 워크플로는 서버 IP 로 고정해 헬스체크한다.
2-8. 앱에서 로그인 유지, 명소 목록, 이미지 표시·업로드를 확인한다. 여기까지가 다운타임이다.
2-9. 옛 DNS TTL 이 지난 뒤 2-5 를 한 번 더 돌려 옛 주소로 들어간 쓰기가 없는지 확인한다.

## 3. 롤백

- **2-7 전**: 가비아 레코드를 원래 대상(ALB·CloudFront)으로 되돌리고, 오토스케일링 2~8·desired 2 로 ECS 를 올린다.
  원본은 읽기만 했으므로 무손실이다
- **2-7 뒤(서버가 쓰기를 받은 뒤)**: 서버 앱을 멈추고(`docker compose stop app caddy`), 같은 도구로 방향을 바꿔
  서버 → RDS 로 옮긴 뒤 위와 같이 되돌린다(`data-migration.md` 의 롤백)

## 4. 정리 (전환 확인 후)

- `deploy-prod.yml`(ECS)을 지우고 `deploy-prod-oci.yml` 을 기본 배포로 둔다
- AWS 계정이 닫히기 전에 RDS 최종 스냅샷의 덤프와 서버 백업 한 부를 **Oracle·AWS 밖**에 보관한다
- 서버의 `/opt/peakda/data/migration` 과 로컬 `~/peakda-mig`, `~/peakda-media` 를 지운다(개인정보)
- dev EC2 의 `/tmp/out`, S3 `postgres/oci-cutover/` 를 지운다

## 운영

| 할 일 | 명령 |
|---|---|
| 상태 | `ssh ubuntu@$HOST 'cd /opt/peakda && docker compose ps && free -m'` |
| 로그 | Grafana Cloud Explore (아래 "로그 보기"). 급할 때는 `ssh ubuntu@$HOST 'cd /opt/peakda && docker compose logs --tail 200 app'` |
| 수집기 상태 | `ssh ubuntu@$HOST 'cd /opt/peakda && docker compose logs --tail 50 alloy'` (warn 이상만 찍힌다) |
| DB 접속 | `ssh -L 15432:127.0.0.1:5432 ubuntu@$HOST` 후 `psql -h 127.0.0.1 -p 15432 -U peakda peakda` |
| 백업 확인 | `oci os object list -bn peakda-prod-backup --all --query 'data[-4:].name'` |
| 스크립트 갱신 | `deploy.sh`·`deploy-gate.sh` 가 바뀌면 1-1 의 `scp` 를 다시 한다 |

### 로그 보기 (Grafana Cloud)

Grafana Cloud → Explore → Loki 데이터 소스에서 시간 범위를 고르고 아래 쿼리를 쓴다. 보존 기간은 무료 플랜 기준(14일)이다.
컨테이너 json-file 로그는 배포로 컨테이너가 바뀌면 지워지지만 Loki 로 이미 보낸 로그는 남는다.

| 보고 싶은 것 | 쿼리 |
|---|---|
| 앱 에러 (스택트레이스 포함) | `{env="prod", container="peakda-app", level="ERROR"}` |
| 경고까지 | `{env="prod", container="peakda-app", level=~"ERROR\|WARN"}` |
| 요청 하나의 전체 흐름 (앱 + Caddy) | `{env="prod"} \| request_id="<ID>"` |
| 5xx 응답 | `{env="prod", container="peakda-caddy"} \| status=~"5.."` |
| 특정 로거 | `{env="prod", container="peakda-app"} \| logger=~".*scheduler.*"` |
| 본문 검색 | `{env="prod", container="peakda-app"} \|= "FCM"` |

- 요청 ID 는 Caddy 가 요청마다 만들어 앱에 넘기고, 앱이 로그(`requestId`)와 응답 헤더 `X-Request-Id` 에 싣는다.
  클라이언트가 오류 화면에 이 값을 보여 주면 제보 하나로 바로 찾을 수 있다. `@Async` 작업(알림 발송·수동 잡)도 같은 ID 를 잇는다
- 라벨은 `env`·`container`·`level` 만 있다. `request_id`·`logger`·`thread`·`status` 는 structured metadata 라 `|` 뒤에서 거른다
- 같은 서버의 마인크래프트 로그는 보내지 않는다(compose 프로젝트 `peakda` 컨테이너만 수집)
- 외부로 나가는 로그에서 개인정보를 가린다. Caddy 접근 로그는 IP 를 /24(IPv6 /48) 대역까지만 남기고 쿼리의
  `lat`·`lng`·`min/maxLat`·`min/maxLng`·`email`·`code`·`state` 를 지운다(`Caddyfile`). 앱 로그는 토큰·OAuth 코드를 가리고
  좌표를 소수 둘째 자리(약 1km), 이메일을 첫 글자+도메인으로 줄인다(`SensitiveLogMasker`). PostgreSQL 느린 쿼리의
  바인드 값 줄은 버린다(`alloy-config.alloy`). 새 API 에 위치·연락처 파라미터를 추가하면 이 세 곳을 같이 본다

### Grafana Cloud 연결

dev 와 같은 스택을 쓴다. URL·사용자 ID 는 `infra/envs/dev/terraform.tfvars` 의 값이고 토큰은 dev SSM 의 값과 같다.
**`deploy.sh` 를 먼저 올린 뒤에 이 커밋을 배포한다.** 옛 `deploy.sh` 는 `alloy-config.alloy` 를 받지 않는다.
새 `deploy.sh` 는 이 파일이 없는 옛 커밋으로 되돌려 배포해도 멈추지 않는다.

```sh
# 1. 스크립트와 수집기 설정을 먼저 올린다. 순서를 어겨 compose 가 빈 디렉터리를 만들어 뒀다면 먼저 지운다
ssh ubuntu@$HOST 'rmdir /opt/peakda/alloy-config.alloy 2>/dev/null || true'
scp infra/oci/{deploy.sh,alloy-config.alloy} ubuntu@$HOST:/opt/peakda/ && ssh ubuntu@$HOST 'chmod 750 /opt/peakda/*.sh'

# 2. .env 의 GRAFANA_CLOUD_* 를 바꿔 넣는다(다시 돌려도 중복되지 않는다). 토큰은 명령행 인자가 아니라 stdin 으로 넘긴다
KEY="$(aws ssm get-parameter --name /peakda/dev/GRAFANA_CLOUD_API_KEY --with-decryption \
  --query Parameter.Value --output text --region ap-northeast-2)"
ssh ubuntu@$HOST 'cd /opt/peakda && umask 077 && t=$(mktemp .env.XXXXXX) && { grep -v "^GRAFANA_CLOUD_" .env; cat; } > "$t" && mv "$t" .env' << EOF
GRAFANA_CLOUD_LOKI_URL=https://logs-prod-030.grafana.net/loki/api/v1/push
GRAFANA_CLOUD_LOKI_USER=1699725
GRAFANA_CLOUD_API_KEY=$KEY
EOF
unset KEY

# 3. 배포한다 (gh workflow run deploy-prod-oci.yml --ref main). 몇 분 뒤 Explore 에서 {env="prod"} 가 보이면 끝
# 4. 하루쯤 뒤 수집기 메모리를 본다. 상한 128m 에 붙어 있으면 올리고 compose 머리 주석의 합계도 고친다
ssh ubuntu@$HOST 'docker stats --no-stream peakda-alloy'
```

- `render-env.sh`(1-4)로 `.env` 를 다시 만들면 GRAFANA_CLOUD_* 가 빠진다. 그 뒤에는 2 를 다시 한다
- 토큰을 바꿀 때도 2 를 다시 하고 `docker compose up -d alloy` 를 한다. AWS 계정을 닫으면 SSM 의 토큰도 사라지므로
  그 뒤로는 Grafana Cloud → Access Policies 에서 `logs:write` 토큰을 새로 만들어 `KEY` 에 넣는다
- `.env` 에 값이 없거나 틀려도 수집기만 전송에 실패하고 서비스는 그대로 돈다. 로그가 안 보이면 위 "수집기 상태" 를 본다

### 백업 복원

```sh
# 덤프를 받아 서버로 옮긴 뒤, 앱을 멈추고 빈 DB 에 복원한다
oci os object get -bn peakda-prod-backup --name <ts>/peakda-<ts>.dump --file peakda.dump
scp peakda.dump ubuntu@$HOST:/opt/peakda/data/
ssh ubuntu@$HOST
  cd /opt/peakda && docker compose stop app
  docker exec peakda-postgres psql -U peakda -d postgres -c 'DROP DATABASE peakda WITH (FORCE)' -c 'CREATE DATABASE peakda'
  docker cp data/peakda.dump peakda-postgres:/tmp/ && docker exec peakda-postgres \
    pg_restore -U peakda -d peakda --no-owner --no-acl --single-transaction --exit-on-error /tmp/peakda.dump
  docker compose up -d app
```
