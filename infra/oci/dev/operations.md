# OCI 개발 배포

GitHub Actions에서 **Deploy Dev (OCI)**를 선택하고 브랜치를 `develop`로 지정해 실행한다.

```sh
gh workflow run deploy-dev.yml --ref develop
```

ARM64 러너에서 테스트와 빌드를 실행한다. 이미지는 GHCR의 `dev-<커밋 SHA>` 태그로 발행하며 OCI 서버는 빌드가 반환한 digest로 이미지를 받는다. 개발 Compose 프로젝트 `peakda-dev`의 앱만 교체한다. 앱이 준비되지 않으면 직전 이미지로 되돌린다. 실행 결과에서 [개발 readiness](https://api-dev.peakda.com/actuator/health/readiness)를 확인할 수 있다.

## 최초 연결

- OCI 서버의 `/opt/peakda-dev`에 개발용 `docker-compose.yml`과 `.env`가 필요하다. Compose 앱의 readiness 헬스체크와 루프백 포트 `18081`을 사용한다.
- 이 디렉터리의 `deploy.sh`와 `deploy-gate.sh`를 서버의 `/opt/peakda-dev`에 복사하고 권한을 `750`으로 설정한다.
- 개발 전용 SSH 키를 만들고 서버의 `~ubuntu/.ssh/authorized_keys`에 공개 키를 `restrict,command="/opt/peakda-dev/deploy-gate.sh"` 옵션으로 등록한다.
- GitHub 환경 `oci-development`에서 배포 가능 브랜치를 `develop`로 제한한다. 변수 `OCI_HOST`에 OCI 서버 주소를, 시크릿 `OCI_DEPLOY_SSH_KEY`에 개발 배포 키를, `OCI_SSH_KNOWN_HOSTS`에 검증한 서버 호스트 키를 저장한다.

SSH 게이트는 고정된 GHCR 저장소에서만 이미지를 받고, 단기 GitHub 토큰은 표준 입력으로 받아 pull에 사용한다. 수동 배포는 서버에 `peakda-dev-local:<커밋 SHA>` 태그와 `org.opencontainers.image.revision` 라벨을 붙인 이미지를 올리고 `/opt/peakda-dev/deploy.sh <커밋 SHA>`를 실행한다.
