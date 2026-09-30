# 계정 간 데이터 이관

`infra/scripts/migrate-data.sh` 는 prod PostgreSQL·Redis 를 다른 AWS 계정(또는 환경)으로 옮긴다.
원본과 대상이 서로 다른 VPC 에 있어 한 곳에서 둘 다 닿기 어려우므로 단계를 나눈다.

| 명령 | 실행 위치 | 하는 일 |
|---|---|---|
| `preflight --side source\|target` | 각 쪽 | 다운타임 전 점검. 접속·버전 규칙·권한·Redis 기능(PEXPIRETIME, PXAT, cluster mode) |
| `export` | 원본에 닿는 곳 | 세션 점검 → `pg_dump` → 원본 지문 → Redis 키(만료 절대 시각 포함) → `SHA256SUMS` |
| `import` | 대상에 닿는 곳 | 무결성 검사 → 대상이 비었는지 점검 → 단일 트랜잭션 복원 → 원본 지문과 대조 → Redis 쓰기·되읽기 대조 |
| `verify --side source` | 원본에 닿는 곳 | export 이후 원본(PostgreSQL·Redis)에 쓰기가 있었는지 판정 |
| `verify --side target` | 대상에 닿는 곳 | import 이후 대상에 쓰기가 있었는지 판정. 대상 앱을 띄우기 전까지만 의미가 있다 |

지문은 테이블별 행 수·행 체크섬(행 텍스트 md5)·시퀀스 값이다. 하나라도 다르면 실패로 끝난다.
Redis 는 `refresh:*`(리프레시 토큰)·`quota:*`(외부 API 일일 쿼터) 문자열 키를 만료 시각째 옮긴다.
리프레시 토큰을 빠뜨리면 전 사용자가 로그아웃된다. 원본은 읽기만 한다.

## 이관 체크리스트

이 스크립트는 DB·Redis 만 옮긴다. 무손실 이전에는 아래도 함께 필요하다.

- [ ] **미디어 버킷**: 사용자 업로드 이미지(`peakda-prod-media-*`)를 `aws s3 sync` 로 복사하고,
      양쪽 객체 수·총 크기(`aws s3 ls --recursive --summarize`)를 대조한다. 버저닝이 켜져 있으니 현재 버전만 옮기면 된다
- [ ] **이미지 주소**: DB 에 CDN 절대 주소가 저장돼 있다면 같은 CDN 도메인(`cdn.peakda.com`)을 새 계정에서도 유지한다
- [ ] **시크릿은 값 그대로**: `JWT_SECRET` 이 다르면 옮긴 리프레시 토큰이 서명 검증에서 전부 실패한다.
      OAuth 클라이언트 ID·시크릿, `FCM_SERVICE_ACCOUNT_BASE64`, 외부 API 키도 원값을 옮긴다
- [ ] **OAuth 앱은 그대로**: 카카오·네이버 사용자 ID 는 앱마다 다르다. 앱을 새로 만들면 기존 계정과 연결이 끊긴다.
      같은 앱에 새 환경의 Redirect URI 만 추가한다
- [ ] **DNS**: 전환 하루 전에 `api` 레코드 TTL 을 60초로 낮춘다. Route53 호스팅 존도 옮긴다면 NS 변경 시점을 따로 잡는다
- [ ] **이미지**: 대상 계정 ECR 에 원본과 **같은 이미지 태그**를 올린다. Liquibase changelog 가 같아야 한다

## 준비

명령은 저장소 루트에서 실행한다.

```sh
# PG_MAJOR 는 대상 서버 메이저 버전. 원본(지금 RDS 16) 이상이어야 한다. export·import 는 같은 이미지로 한다.
# 실행할 호스트에서 빌드한다(아키텍처가 맞아야 한다).
docker build -f infra/scripts/Dockerfile.data-migration --build-arg PG_MAJOR=16 \
  -t peakda-data-migration infra/scripts
```

지금 prod 의 RDS·ElastiCache 는 ECS 태스크 보안 그룹(`peakda-prod-task-sg`)에서 오는 접속만 받는다.
export 는 이 SG 를 붙인 임시 EC2 에서 실행한다. import 도 대상 DB·Redis 에 닿는 곳에서 실행한다.
EC2 에서 `--s3` 를 쓰면 컨테이너가 인스턴스 역할을 받아야 하므로 `docker run --network host` 로 실행한다
(IMDSv2 hop limit 이 1 이면 브리지 네트워크에서는 자격증명을 못 받는다).

접속 정보는 환경변수로 넘긴다(`docker run -e 이름` 은 셸의 값을 그대로 넘긴다).
산출물 디렉터리는 **저장소 밖**에 둔다. 덤프에 개인정보가 들어 있어 실수로 커밋되면 안 된다.

```sh
MIG="$HOME/peakda-migration/$(date +%Y%m%d)"   # 산출물 위치 (저장소 밖)

# 원본 (옛 계정). RDS 인증서까지 검증한다.
export SOURCE_PGHOST=<RDS 엔드포인트> SOURCE_PGUSER=peakda
export SOURCE_PGSSLMODE=verify-full SOURCE_PGSSLROOTCERT=/opt/migration/rds-global-bundle.pem
export SOURCE_PGPASSWORD="$(aws secretsmanager get-secret-value --query SecretString --output text \
  --secret-id "$(aws rds describe-db-instances --db-instance-identifier peakda-prod \
    --query 'DBInstances[0].MasterUserSecret.SecretArn' --output text)" | jq -r .password)"
export SOURCE_REDIS_URL="$(aws ssm get-parameter --name /peakda/prod/SPRING_DATA_REDIS_URL \
  --with-decryption --query Parameter.Value --output text)"   # rediss://:<토큰>@<호스트>:6379

# 대상 (새 계정). TARGET_PGDATABASE(기본 peakda) 는 미리 만들어 둔 빈 DB 여야 한다.
# TARGET_PGUSER 는 대상 앱이 쓰는 역할이어야 한다(--no-owner 로 복원해 이 사용자가 소유자가 된다).
export TARGET_PGHOST=... TARGET_PGUSER=... TARGET_PGPASSWORD=... TARGET_REDIS_URL=rediss://...
export TARGET_PGSSLMODE=verify-full TARGET_PGSSLROOTCERT=/opt/migration/rds-global-bundle.pem
```

대상 Redis 는 cluster mode 가 꺼져 있어야 하고 메모리가 원본 이상이어야 한다. 부족하면 eviction 으로 토큰이 빠진다.
되읽기 대조가 잡아내지만 다운타임 중에 다시 해야 한다.

### 리허설 (권장)

실제 권한 모델(RDS `rds_superuser`, 파라미터 그룹, ElastiCache TLS)에서 한 번 돌려 본다.
원본 RDS 스냅샷을 대상 계정의 임시 RDS 로 복원하고, 그 임시 RDS 를 원본 삼아 export 부터 import 까지 끝까지 실행한다.
소요 시간도 이때 잰다. 로컬 측정으로는 행 3만·Redis 키 2만 기준 export 약 15초, import 30~60초였다.

## 순서

앱을 내리기 전:

1. 양쪽 `preflight` 가 통과해야 한다. 환경변수 전달은 아래 export·import 예시와 같다
   ```sh
   docker run --rm -e SOURCE_PGHOST -e SOURCE_PGUSER -e SOURCE_PGPASSWORD -e SOURCE_PGSSLMODE \
     -e SOURCE_PGSSLROOTCERT -e SOURCE_REDIS_URL peakda-data-migration preflight --side source
   ```
2. 대상 계정에 인프라를 만들고 빈 DB·Redis 를 준비한다. 대상 앱은 아직 띄우지 않는다
   (이미 떠서 Liquibase 가 스키마를 만들었다면 import 에 `--reset-target` 을 붙인다. 기존 대상 DB 를 덤프해 둔 뒤 비운다)

다운타임:

3. **원본 동결**. 오토스케일링 하한이 남아 있으면 태스크가 되살아나 쓰기를 받는다. 원본을 해체할 때까지
   원본 앱은 다시 올리지 않는다(롤백 제외). 원본 쪽 `terraform apply` 와 deploy-prod 실행도 멈춘다
   ```sh
   aws application-autoscaling register-scalable-target --service-namespace ecs \
     --resource-id service/peakda-prod/peakda-prod --scalable-dimension ecs:service:DesiredCount \
     --min-capacity 0 --max-capacity 0
   aws ecs update-service --cluster peakda-prod --service peakda-prod --desired-count 0 > /dev/null
   aws ecs wait services-stable --cluster peakda-prod --services peakda-prod
   aws ecs describe-services --cluster peakda-prod --services peakda-prod --query 'services[0].runningCount'   # 0
   ```
4. 안전망으로 RDS 수동 스냅샷과 ElastiCache 수동 백업을 남긴다
5. 원본 쪽에서 export 한다. 마지막 로그의 `--expect-sums <값>` 을 적어 둔다
   ```sh
   docker run --rm -v "$MIG:/work/out" -e SOURCE_PGHOST -e SOURCE_PGUSER -e SOURCE_PGPASSWORD \
     -e SOURCE_PGSSLMODE -e SOURCE_PGSSLROOTCERT -e SOURCE_REDIS_URL \
     peakda-data-migration export --dir /work/out
   ```
6. `$MIG` 를 대상 쪽으로 옮긴다. `--s3 s3://버킷/접두어` 를 export·import 양쪽에 주면 S3 로 주고받는다
   (대상 계정이 읽을 수 있는 버킷이어야 한다)
7. 대상 쪽에서 import 한다. `PostgreSQL: 원본 지문과 일치`, `Redis 되읽기: Redis 일치` 가 나와야 한다
   ```sh
   docker run --rm -v "$MIG:/work/out" -e TARGET_PGHOST -e TARGET_PGUSER -e TARGET_PGPASSWORD \
     -e TARGET_PGSSLMODE -e TARGET_PGSSLROOTCERT -e TARGET_REDIS_URL \
     peakda-data-migration import --dir /work/out --expect-sums <5번의 값>
   ```
8. 원본 쪽에서 `verify --side source --dir /work/out` (5번과 같은 환경변수) 으로 export 이후 원본에 쓰기가 없었는지 확인한다
9. 대상 앱을 띄우고 DNS 를 넘긴다. 다운타임이 끝난다
10. 옛 DNS 응답의 TTL 이 다 지난 뒤 `verify --side source` 를 한 번 더 돌린다. 이때도 일치해야 옛 주소로 들어간 쓰기가 없다

정리:

11. 양쪽의 `$MIG` 와 S3 사본을 지운다. 컨테이너가 root 로 만든 파일이라 `sudo rm -rf "$MIG"` 가 필요할 수 있다

## 롤백

- **대상 앱이 쓰기를 받기 전(9번 전)**: 원본 오토스케일링·desired count 를 원래대로 올린다. 원본은 읽기만 했으므로 무손실이다
- **대상 앱이 쓰기를 받은 뒤**: 대상 앱을 내리고 같은 도구를 방향만 바꿔 실행한다. 새 계정을 `SOURCE_*`, 옛 계정을 `TARGET_*`
  로 두고 export 한 뒤 옛 계정에 `import --reset-target`(기존 옛 DB 는 덤프로 남는다)을 하고 원본 앱을 올린다.
  대상 앱이 뜨면 스케줄러가 곧바로 DB 에 쓰므로 `verify --side target` 은 차이를 보고한다. 이 시점부터는 역방향 이관이 기준이다

ElastiCache 인증서는 이미지의 시스템 CA 로 검증된다. 다른 CA 면 `*_REDIS_CACERT`, 검증을 생략하려면
`*_REDIS_INSECURE=1` 을 준다.
