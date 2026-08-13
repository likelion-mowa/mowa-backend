---
적용: 항상
---

# MOWA Backend Development Rules

## 1. 기준 문서

백엔드 구현 시 반드시 다음 문서를 실제로 확인하고 이를 기준으로 구현한다.

1. `docs/api-spec.md`
2. `docs/data-table.md`
3. `docs/erd.md`
4. `docs/feature-spec.md`

문서별 기준은 다음과 같다.

- API Endpoint, Request/Response, 상태 코드, 비즈니스 규칙 → `docs/api-spec.md`
- DB 테이블, 컬럼, 타입, 제약조건 → `docs/data-table.md`
- 테이블 관계 및 FK 구조 → `docs/erd.md`
- 기능 목적 및 범위 → `docs/feature-spec.md`

관련 내용을 기억에 의존하지 말고 작업 전에 실제 문서를 확인한다.

문서와 코드가 충돌하거나 문서 간 모순이 발견되면 임의로 판단하지 말고 먼저 보고한다.

## 2. MOWA 구현 원칙

- Java 21 및 현재 프로젝트의 Spring Boot/Gradle 구성을 따른다.
- 문서에 정의되지 않은 API, 테이블, 컬럼, Enum 값을 임의로 추가하지 않는다.
- API Request/Response 및 DB 구조를 임의로 변경하지 않는다.
- `userId`는 Request에서 받지 않고 인증 정보에서 추출한다.
- 다른 사용자의 리소스 접근은 API 명세에 정의된 소유권 정책을 따른다.
- WalkExperience 삭제는 Soft Delete 정책을 따른다.
- 최종 WalkExperience의 Snapshot 정책을 유지한다.
- `suggestedTags`는 DB에 저장하지 않는다.
- 배열 및 Nullable 필드의 PATCH 정책은 `docs/api-spec.md`를 따른다.
- Entity를 API Response로 직접 반환하지 않는다.
- 현재 요청받은 범위 밖의 기능을 임의로 구현하지 않는다.

## 3. 코드 구조

기능별 책임을 다음과 같이 분리한다.

- Controller → 요청/응답 처리
- Service → 비즈니스 로직 및 트랜잭션
- Repository → DB 접근
- Entity → DB 모델
- DTO → API Request/Response

기본 패키지 구조는 다음을 따른다.

- `controller`
- `service`
- `repository`
- `entity`
- `dto`

공통 기능은 `common`, 프로젝트 설정은 `config` 패키지에서 관리한다.

## 4. 작업 방식

- 작업 시작 전 관련 `docs/` 문서를 먼저 확인한다.
- 현재 요청받은 작업 범위만 구현한다.
- 다른 담당자의 도메인을 불필요하게 수정하지 않는다.
- 공통 코드 또는 다른 도메인의 수정이 필요한 경우 먼저 이유와 수정 범위를 보고한다.
- 새로운 라이브러리가 필요한 경우 임의로 추가하지 말고 이유를 먼저 설명한다.
- 기존 설계를 임의로 개선하거나 변경하지 않는다.
- Git commit, push, PR 생성은 명시적으로 요청받기 전까지 수행하지 않는다.

## 5. 민감 정보

다음과 같은 값을 코드 또는 Git에 포함하지 않는다.

- OpenAI API Key
- DB Password
- JWT Secret
- Access Token
- 기타 Secret 값

환경 변수 또는 별도 로컬 설정을 사용한다.

## 6. 검증

작업 완료 후 가능한 경우 다음 명령을 실행한다.

- `./gradlew compileJava`
- `./gradlew test`

오류 발생 시 설계를 임의로 변경하지 말고 원인을 분석하여 최소 범위로 수정한다.

## 7. 작업 완료 보고

작업 완료 후 다음 내용을 보고한다.

- 생성한 파일
- 수정한 파일
- 주요 구현 내용
- 문서 기준 확인 결과
- 검증 결과
- 추가 확인이 필요한 사항

확인할 사항이 없다면 `없음`으로 보고한다.