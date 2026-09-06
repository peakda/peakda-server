# peakda-server

계절 여행 타이밍 안내 서비스 PEAKDA 백엔드.

## 브랜치와 릴리스

- `feature/*`는 `develop`에서 분기하고 PR로 병합한다.
- 배포 범위를 확정하면 `develop`에서 임시 `release/<버전>`을 만든다. 후보 검증 중에는 버그 수정과 릴리스 준비만 반영한다.
- 검증된 릴리스는 PR로 `main`에 병합한다. 릴리스 중 수정한 내용은 `develop`에도 반영한다.
- 운영에 배포한 정확한 커밋에 `vX.Y.Z` 태그와 GitHub Release를 만들고, 이미지 digest와 배포 실행 링크를 기록한다.
- 긴급 수정은 현재 운영 태그에서 `hotfix/*`로 분기하고, 패치 릴리스 후 `main`과 `develop`에 반영한다.

현재 배포는 GitHub Actions의 수동 실행이다. `Deploy Production (AWS)`의 실행 브랜치를 명시하고 `image_tag`는 비워 커밋 SHA로 이미지를 빌드한다. `image_tag`는 새 이미지에 붙일 이름이며, 기존 이미지나 Git 태그의 소스를 선택하는 입력이 아니다.

운영 환경과 프론트 도메인 전환 절차는 [운영 환경 문서](infra/envs/prod/README.md)를 참고한다.
