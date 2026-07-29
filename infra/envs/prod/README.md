# envs/prod — production 환경 (미구성)

이번 마이그레이션 범위는 develop 까지다. 이 디렉터리는 **아직 apply 하지 않는다.**
실수로 실행되는 것을 막기 위해 Terraform 파일을 두지 않고 전환 설계만 기록한다.

## 전환 시점

AWS 크레딧이 만료되는 **2026-12-13** 이전에 유료 플랜 전환 여부를 결정해야 한다.
production 구성은 그 결정과 함께 진행한다.

## 재사용할 모듈

develop 과 동일한 모듈을 파라미터만 바꿔 사용한다.

| 모듈 | 재사용 | 변경점 |
|---|---|---|
| `network` | 그대로 | CIDR 만 분리 (예: 10.1.0.0/16). subnet 이 이미 2 AZ 라 ALB 요건을 만족한다 |
| `registry` | **공유** | dev/prod 가 같은 ECR 저장소를 쓰고 태그로 구분한다 |
| `config` | 그대로 | `env = "prod"` → 파라미터 경로가 `/peakda/prod/` 로 분리된다 |
| `backup` | 그대로 | 버킷 이름만 다르다 |
| `github-oidc` | 그대로 | `create_oidc_provider = false` (provider 는 계정당 1개), 허용 subject 를 `refs/heads/main` 으로 |
| `dns` | 그대로 | `create_zone = false` 로 dev 가 만든 존을 참조하고 `api.peakda.com` 레코드만 추가 |
| `app-server` | **교체** | ECS Fargate + ALB + RDS + ElastiCache 로 대체 (`ecs-service` 모듈 신규 작성) |

## 컴퓨트 전환

develop 은 예산 제약(월 $40.8)으로 EC2 단일 인스턴스를 쓴다.
production 은 유료 전환 후 다음 구성으로 간다.

```
Route53 (api.peakda.com)
  └ ALB (ACM 인증서, 2 AZ)
      └ ECS Fargate Service (desired ≥ 2)
            ├→ RDS PostgreSQL (자동 백업 · PITR)
            └→ ElastiCache Redis
```

이때 develop 의 Caddy(Let's Encrypt)는 ALB + ACM 으로 대체된다.

## 예상 비용

| 항목 | 월 |
|---|---|
| ALB | $16.4 |
| Fargate 0.5 vCPU / 1GB × 2 | $42.0 |
| RDS db.t4g.micro | $14.3 |
| ElastiCache cache.t4g.micro | $9.0 |
| 합계 | **$81.7** |

## 참고

- 배포 계획: [../../../docs/aws-배포-마이그레이션-plan.md](../../../docs/aws-배포-마이그레이션-plan.md)
- 옵저버빌리티: [../../../docs/옵저버빌리티-plan.md](../../../docs/옵저버빌리티-plan.md)
