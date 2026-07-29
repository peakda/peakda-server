# infra — peakda AWS 인프라 (Terraform)

develop 환경은 **생성 완료** 상태다. 남은 것은 시크릿 주입과 네임서버 변경이다.

- 설계 배경: [../docs/aws-배포-마이그레이션-plan.md](../docs/aws-배포-마이그레이션-plan.md)
- 옵저버빌리티(다음 PR): [../docs/옵저버빌리티-plan.md](../docs/옵저버빌리티-plan.md)

## 구조

```
infra/
├── bootstrap/        state 버킷 (로컬 state, 최초 1회 apply 완료)
├── modules/
│   ├── network/      VPC · public subnet ×2 · IGW · SG (NAT 없음)
│   ├── registry/     ECR + lifecycle(최근 5개)
│   ├── config/       SSM Parameter Store (일반 + 시크릿)
│   ├── backup/       S3 (postgres/ 30일 만료, assets/ 영구)
│   ├── media/        S3 이미지 버킷 + 앱용 IAM 사용자
│   ├── app-server/   EC2 · EIP · IAM · 데이터 EBS · user_data
│   ├── github-oidc/  OIDC provider + 배포 role
│   └── dns/          Route53 zone + A레코드
├── scripts/
│   ├── put-secrets.sh    시크릿 대화형 주입
│   └── migrate-media.sh  Railway Bucket → S3 이미지 이관
├── envs/
│   ├── dev/          ← 현재 운영 중
│   └── prod/         설계만 (README)
└── server/           EC2 에 배포되는 자산 (S3 assets/ 로 업로드됨)
    ├── docker-compose.yml
    ├── Caddyfile
    ├── deploy.sh
    └── backup.sh
```

## 생성된 리소스

| 항목 | 값 |
|---|---|
| 인스턴스 | `i-08cf4b59fd351c6d2` (t4g.small) |
| 고정 IP | `15.164.44.17` |
| 서비스 URL | https://api-dev.peakda.com |
| ECR | `421438965126.dkr.ecr.ap-northeast-2.amazonaws.com/peakda-server` |
| 배포 role | `arn:aws:iam::421438965126:role/peakda-dev-github-deploy` |
| 이미지 버킷 | `peakda-dev-media-421438965126` (앱 업로드용) |
| 백업·자산 버킷 | `peakda-dev-storage-421438965126` (앱 이미지 아님) |
| state | `peakda-tfstate-421438965126` |

> 버킷이 둘이다. **`-media-`가 앱이 쓰는 이미지 저장소**이고, `-storage-`는 DB 덤프와 서버 구성 파일을 담는 운영용이다.

## 남은 수동 절차

### 1. 시크릿 주입 (필수)

`deploy.sh` 는 값이 주입되지 않은 시크릿이 있으면 배포를 거부한다.

이미 주입된 것:

- `SPRING_DATASOURCE_PASSWORD` — 새 PG 컨테이너용이라 랜덤 생성해 주입
- `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` — S3 IAM 사용자 키를 발급해 주입

**남은 8개**는 Railway 대시보드의 Variables 탭에서 가져온다.

```bash
./scripts/put-secrets.sh
```

값이 화면에 표시되지 않고 셸 히스토리에도 남지 않는다. 빈 값으로 엔터하면 건너뛴다.

| 남은 시크릿 |
|---|
| `JWT_SECRET` |
| `KAKAO_CLIENT_ID` · `KAKAO_CLIENT_SECRET` |
| `NAVER_CLIENT_ID` · `NAVER_CLIENT_SECRET` |
| `KTO_SERVICE_KEY` · `KMA_SERVICE_KEY` · `PUBDATA_FESTIVAL_SERVICE_KEY` |

주입 여부 확인:

```bash
aws ssm get-parameters-by-path --path /peakda/dev --with-decryption --recursive \
  --region ap-northeast-2 --query 'Parameters[?Value==`PLACEHOLDER_SET_ME_VIA_CLI`].Name' --output text
```

빈 결과가 나오면 완료다.

### 2. 네임서버 변경 (필수)

도메인 등록기관에서 `peakda.com` 의 네임서버를 아래로 바꾼다. 전파에 최대 48시간이 걸리므로 먼저 해두는 편이 좋다.

```
ns-103.awsdns-12.com
ns-1297.awsdns-34.org
ns-1570.awsdns-04.co.uk
ns-725.awsdns-26.net
```

전파 확인:

```bash
dig +short NS peakda.com
dig +short api-dev.peakda.com   # 15.164.44.17 이 나와야 한다
```

**Caddy 의 인증서 발급은 DNS 가 이 서버를 가리킨 뒤에야 성공한다.** 전파 전에는 HTTPS 가 뜨지 않는 것이 정상이다.

### 3. 첫 배포

1·2번이 끝난 뒤 `develop` 브랜치에 푸시하거나 Actions 에서 `Deploy Dev (AWS)` 를 수동 실행한다.

### 4. OAuth Redirect URI 등록

카카오·네이버 개발자 콘솔에 아래를 추가한다. **누락하면 소셜 로그인이 전부 실패한다.**

```
https://api-dev.peakda.com/login/oauth2/code/kakao
https://api-dev.peakda.com/login/oauth2/code/naver
```

### 5. 데이터 이관

**DB** — 사용자 생성 데이터만 옮기고 외부 API 데이터는 스케줄러가 재수집한다.
상세는 배포 계획 문서 11장을 참고한다.

**이미지** — Railway Bucket 에 있는 기존 업로드를 S3 로 옮긴다.

```bash
export RAILWAY_BUCKET_ENDPOINT='https://...'
export RAILWAY_BUCKET_NAME='...'
export RAILWAY_ACCESS_KEY='...'
export RAILWAY_SECRET_KEY='...'
./scripts/migrate-media.sh
```

값은 Railway 대시보드 → 프로젝트 → Bucket → Variables 에서 확인한다.
로컬 디스크를 경유하므로 데이터가 수 GB 이상이면 `rclone` 을 쓰는 편이 빠르다.

## 자주 쓰는 명령

### 서버 접속 (SSH 아님)

```bash
aws ssm start-session --target i-08cf4b59fd351c6d2 --region ap-northeast-2
```

### 컨테이너 상태 확인

```bash
aws ssm send-command --instance-ids i-08cf4b59fd351c6d2 \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["cd /opt/peakda && docker compose ps"]' \
  --region ap-northeast-2 --query Command.CommandId --output text
```

### 서버 자산 수정 반영

`server/` 의 파일을 고친 뒤:

```bash
cd envs/dev && terraform apply   # S3 assets/ 갱신
```

다음 배포 때 `deploy.sh` 가 `s3 sync` 로 가져간다.

### 인프라 변경

```bash
cd envs/dev
terraform plan -out=dev.tfplan
terraform apply dev.tfplan
```

## 주의사항

- **`prevent_destroy` 가 걸린 리소스**: 데이터 EBS 볼륨, Route53 존, state 버킷. 정말 지워야 한다면 코드에서 해당 lifecycle 블록을 먼저 제거해야 한다.
- **IAM role 의 `description` 에 한글을 쓰지 않는다** (Latin-1 범위만 허용). 보안 그룹 규칙 `description` 에는 작은따옴표도 쓸 수 없다.
- **NAT Gateway 를 추가하지 않는다.** 월 $43 로 develop 예산 전체를 넘긴다.
- `terraform.tfvars` 의 `github_allowed_subjects` 에 `feature/59` 가 임시로 들어있다. 컷오버 후 제거한다.
