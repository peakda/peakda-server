# 계정 간 데이터 이관

`infra/scripts/migrate-data.sh` 는 prod PostgreSQL·Redis 를 다른 AWS 계정(또는 환경)으로 옮긴다.
원본과 대상이 서로 다른 VPC 에 있어 한 곳에서 둘 다 닿기 어려우므로 단계를 나눈다.

| 단계 | 실행 위치 | 하는 일 |
|---|---|---|
| `export` | 원본에 닿는 곳 | 세션 점검 → `pg_dump` → 원본 지문 → Redis 키(만료 절대 시각 포함) → `SHA256SUMS` |
| `import` | 대상에 닿는 곳 | 무결성 검사 → 대상이 비었는지 점검 → 단일 트랜잭션 복원 → 원본 지문과 대조 → Redis 쓰기·대조 |
| `verify --side source` | 원본에 닿는 곳 | export 이후 원본에 쓰기가 있었는지 판정 |
| `verify --side target` | 대상에 닿는 곳 | import 이후 대상에 쓰기가 있었는지 판정 |

지문은 테이블별 행 수·행 체크섬(행 텍스트 md5)·시퀀스 값이다. 하나라도 다르면 실패로 끝난다.
Redis 는 `refresh:*`(리프레시 토큰)·`quota:*`(외부 API 일일 쿼터) 문자열 키만 옮긴다. 리프레시 토큰을 빠뜨리면
전 사용자가 로그아웃된다. 원본은 읽기만 한다.

## 준비

명령은 저장소 루트에서 실행한다.

```sh
# PG_MAJOR 는 원본 서버 메이저 버전(지금 RDS 는 16). 대상은 같거나 새 버전이어야 한다.
docker build -f infra/scripts/Dockerfile.data-migration --build-arg PG_MAJOR=16 \
  -t peakda-data-migration infra/scripts
```

지금 prod 의 RDS·ElastiCache 는 ECS 태스크 보안 그룹(`peakda-prod-task-sg`)에서 오는 접속만 받는다.
export 는 이 SG 를 붙인 임시 EC2(또는 ECS 태스크)에서 실행한다. import 도 대상 DB·Redis 에 닿는 곳에서 실행한다.

접속 정보는 환경변수로 넘긴다(`docker run -e 이름` 은 셸의 값을 그대로 넘긴다).
산출물 디렉터리는 **저장소 밖**에 둔다. 덤프에 개인정보가 들어 있어 실수로 커밋되면 안 된다.

```sh
MIG="$HOME/peakda-migration/$(date +%Y%m%d)"   # 산출물 위치 (저장소 밖)

# 원본 (옛 계정)
export SOURCE_PGHOST=<RDS 엔드포인트> SOURCE_PGUSER=peakda
export SOURCE_PGPASSWORD="$(aws secretsmanager get-secret-value --query SecretString --output text \
  --secret-id "$(aws rds describe-db-instances --db-instance-identifier peakda-prod \
    --query 'DBInstances[0].MasterUserSecret.SecretArn' --output text)" | jq -r .password)"
export SOURCE_REDIS_URL="$(aws ssm get-parameter --name /peakda/prod/SPRING_DATA_REDIS_URL \
  --with-decryption --query Parameter.Value --output text)"   # rediss://:<토큰>@<호스트>:6379

# 대상 (새 계정). TARGET_PGDATABASE(기본 peakda) 는 미리 만들어 둔 빈 DB 여야 한다.
export TARGET_PGHOST=... TARGET_PGUSER=... TARGET_PGPASSWORD=... TARGET_REDIS_URL=rediss://...
```

## 순서

1. 대상 계정에 인프라를 만들고 빈 DB·Redis 를 준비한다. 대상 앱은 아직 띄우지 않는다
   (이미 떠서 Liquibase 가 스키마를 만들었다면 import 에 `--reset-target` 을 붙인다. 기존 대상 DB 를 덤프해 둔 뒤 비운다)
2. 원본 앱을 내린다(ECS 태스크 0). 다운타임이 시작된다. 안전망으로 RDS 수동 스냅샷을 남긴다
3. 원본 쪽에서 export
   ```sh
   docker run --rm -v "$MIG:/work/out" -e SOURCE_PGHOST -e SOURCE_PGUSER -e SOURCE_PGPASSWORD \
     -e SOURCE_REDIS_URL peakda-data-migration export --dir /work/out
   ```
4. `$MIG` 를 대상 쪽으로 옮긴다. `--s3 s3://버킷/접두어` 를 export·import 양쪽에 주면 S3 로 주고받는다
   (대상 계정이 읽을 수 있는 버킷이어야 한다). 덤프에는 개인정보가 들어 있다
5. 대상 쪽에서 import. `PostgreSQL: 원본 지문과 일치`, `Redis: 원본과 일치` 가 나와야 한다
   ```sh
   docker run --rm -v "$MIG:/work/out" -e TARGET_PGHOST -e TARGET_PGUSER -e TARGET_PGPASSWORD \
     -e TARGET_REDIS_URL peakda-data-migration import --dir /work/out
   ```
6. 원본 쪽에서 `verify --side source` 로 export 이후 원본에 쓰기가 없었는지 확인한다
7. 대상 앱을 띄우고 DNS 를 넘긴다. 다운타임이 끝난다
8. 양쪽의 `$MIG` 와 S3 사본을 지운다

대상 앱이 쓰기를 받기 전이라면 원본 ECS 를 다시 올리는 것으로 무손실 롤백된다. 쓰기를 받은 뒤라면
`verify --side target` 으로 대상에 쓰기가 있었는지 먼저 판정한다.

ElastiCache 인증서는 이미지의 시스템 CA 로 검증된다. 다른 CA 면 `*_REDIS_CACERT`, 검증을 생략하려면
`*_REDIS_INSECURE=1` 을 준다.
