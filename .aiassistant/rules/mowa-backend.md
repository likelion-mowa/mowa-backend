---
적용: 항상
---

---

## 적용: 항상

# MOWA Backend Development Rules

## 1. 기준 문서

백엔드 구현 시 관련 내용을 기억에 의존하지 말고 다음 문서를 실제로 확인한다.

```text
docs/api-spec.md
docs/data-table.md
docs/erd.md
docs/feature-spec.md
```

문서별 기준은 다음과 같다.

### `docs/api-spec.md`

다음을 판단하는 기준 문서이다.

* API Endpoint
* HTTP Method
* Request / Response
* Validation
* 상태 코드
* 인증 및 소유권 정책
* 상태 전이
* PATCH 처리 규칙
* API 비즈니스 규칙

### `docs/data-table.md`

다음을 판단하는 기준 문서이다.

* DB 테이블
* 컬럼명
* 자료형
* NULL 허용 여부
* UNIQUE / CHECK 등 제약조건
* Enum 저장값
* 인덱스
* Snapshot 저장 구조
* Soft Delete 구조

### `docs/erd.md`

다음을 판단하는 기준 문서이다.

* Entity 관계
* FK
* Cardinality
* `FK + UNIQUE` 관계
* 연결 테이블 구조

### `docs/feature-spec.md`

다음을 판단하는 기준 문서이다.

* 기능 목적
* 사용자 흐름
* MVP 기능 범위
* 기능별 정책과 예외의 배경

---

## 2. 문서 충돌 처리

문서와 코드 또는 문서끼리 충돌하는 경우 임의로 새로운 정책을 만들지 않는다.

우선 각 문서의 담당 영역을 기준으로 판단한다.

예:

```text
API Request/Response
→ api-spec.md

DB 컬럼 및 제약조건
→ data-table.md

FK 및 관계
→ erd.md

기능 목적 및 범위
→ feature-spec.md
```

담당 영역을 기준으로도 해결되지 않는 실질적인 모순이 있으면 구현을 임의 확정하지 말고 보고한다.

---

## 3. 기술 기준

현재 프로젝트의 설정을 우선한다.

기본 기술 기준:

```text
Java 21
Spring Boot
Gradle
Spring Data JPA
PostgreSQL
```

기존 프로젝트에서 사용 중인 라이브러리와 설정을 우선하며, 동일 목적을 위해 새로운 라이브러리를 임의로 추가하지 않는다.

---

## 4. 계층별 책임

각 계층의 책임을 분리한다.

### Controller

* HTTP Request 수신
* Request DTO 검증 연결
* Service 호출
* HTTP Response 반환

비즈니스 로직을 직접 구현하지 않는다.

### Service

* 비즈니스 규칙
* 데이터 소유권 검증
* 상태 전이
* 트랜잭션
* Entity 생성 및 변경
* 여러 Repository 간 처리

### Repository

* DB 접근
* 기능 구현에 실제 필요한 조회

미래 기능을 예상하여 Query Method, `@Query`, Specification 등을 선제적으로 추가하지 않는다.

### Entity

* DB 모델
* Entity 상태
* DB Mapping

Entity를 API Request 또는 Response 모델로 사용하지 않는다.

### DTO

* API Request
* API Response

API 명세와 필드명 및 nullable 정책을 일치시킨다.

---

## 5. 기본 패키지 구조

기존 구조를 우선하며 기본적으로 다음 패키지를 사용한다.

```text
controller
service
repository
entity
dto
```

공통 기능:

```text
common
```

프로젝트 설정:

```text
config
```

새로운 패키지 계층이나 추상화 구조를 특별한 필요 없이 추가하지 않는다.

---

## 6. API 및 사용자 소유권

API Request/Response는 `docs/api-spec.md`를 따른다.

`userId` 또는 `user_id`를 클라이언트 Request에서 받아 소유자를 결정하지 않는다.

인증된 사용자 정보에서 서버가 소유자를 결정한다.

다음 주요 리소스는 로그인 사용자 기준으로 분리한다.

```text
WalkCandidate
ExperienceDraft
WalkExperience
```

다른 사용자가 소유한 리소스 접근 처리는 `docs/api-spec.md`의 정책을 따른다.

---

## 7. Candidate → Draft → Experience 구조

MOWA의 핵심 데이터 생명주기는 다음과 같다.

```text
WalkCandidate
    ↓
ExperienceDraft
    ↓
WalkExperience
```

관계 구조는 다음 원칙을 유지한다.

```text
WalkCandidate 1 : 0..1 ExperienceDraft
ExperienceDraft 1 : 0..1 WalkExperience
```

DB에서는 다음 구조로 중복 생성을 방지한다.

```text
experience_drafts.candidate_id
→ FK + UNIQUE + NOT NULL

walk_experiences.draft_id
→ FK + UNIQUE + NOT NULL
```

사용자 소유권 일치는 Service 계층에서도 검증한다.

---

## 8. WalkExperience Snapshot

최종 `WalkExperience`는 일반 조회에서 `ExperienceDraft` 또는 `WalkCandidate`에 의존하지 않는 Snapshot 구조를 유지한다.

Candidate에서 다음 객관적 정보를 Snapshot한다.

```text
detectedStartAt → startedAt
detectedEndAt → endedAt
durationSeconds → durationSeconds
locationSummary → locationSummary
```

사용자가 최종 확인한 다음 값도 WalkExperience에 독립적으로 저장한다.

```text
title
body
photoUrl
companion
situation
emotions
tags
```

일반적인 아카이브·상세·캘린더·태그 조회를 위해 Candidate 또는 Draft를 JOIN하는 구조로 변경하지 않는다.

---

## 9. Emotion 및 Tag

감정과 태그는 현재 Entity 설계를 유지한다.

### Draft 감정

```text
experience_draft_emotions
```

`ExperienceDraft`의 `@ElementCollection`으로 관리한다.

### Experience 감정

```text
walk_experience_emotions
```

`WalkExperience`의 `@ElementCollection`으로 관리한다.

### Experience 태그

```text
walk_experience_tags
```

`WalkExperience`의 `@ElementCollection`으로 관리한다.

위 연결 테이블을 위한 별도 Entity 또는 Repository를 임의로 만들지 않는다.

현재 DB 설계는 관계 ID와 값으로 이루어진 복합 PK 구조이다.

---

## 10. AI 생성 정책

AI 생성 로직은 `docs/api-spec.md`와 `docs/data-table.md`를 따른다.

핵심 원칙:

* 사용자가 제공하지 않은 기억을 사실처럼 생성하지 않는다.
* 입력되지 않은 관계·감정·대화·사건을 임의로 확정하지 않는다.
* 사진만으로 사용자 감정 또는 인물 관계를 확정하지 않는다.
* 값이 없는 입력은 AI Prompt에서 제외한다.

AI가 생성한:

```text
aiTitle
aiBody
```

는 Draft에 저장한다.

AI의:

```text
suggestedTags
```

는 DB에 저장하지 않는다.

사용자가 확인·수정한 최종 태그만 `WalkExperience`의 태그로 저장한다.

---

## 11. AI 생성 상태

AI 생성 상태는 현재 정의된 값만 사용한다.

```text
PENDING
GENERATING
SUCCESS
FAILED
```

정의되지 않은 상태를 임의로 추가하지 않는다.

AI 생성 요청 및 재시도 가능 상태는 `docs/api-spec.md`를 따른다.

`SUCCESS` 상태에서는 명세에 정의된 AI 결과 필수 조건을 유지한다.

---

## 12. Candidate 상태

Candidate 상태는 현재 정의된 값만 사용한다.

```text
DETECTED
SUGGESTED
RECORDING
SKIPPED
```

상태 전이는 `docs/api-spec.md`를 따른다.

새로운 상태를 임의로 추가하지 않는다.

---

## 13. Enum

MOWA에서 정의된 코드값은 문서를 기준으로 사용한다.

### Companion

```text
ALONE
WITH_SOMEONE
PET
```

### Emotion

```text
CALM
HAPPY
TIRED
REFRESHED
PENSIVE
```

### Situation

```text
MORNING
AFTERNOON
EVENING
IN_TRANSIT
EXPLORING
```

문서에 정의되지 않은 Enum 값을 임의로 추가하지 않는다.

---

## 14. Soft Delete

`WalkExperience` 삭제는 Hard Delete가 아니라 `deletedAt` 기반 Soft Delete를 사용한다.

```text
deletedAt = 삭제 시각
```

일반 목록·상세·수정 대상에서는 삭제된 Experience를 제외한다.

Soft Delete된 행도 유지되므로 기존 `draftId UNIQUE` 제약 역시 유지된다.

동일 Draft로 새로운 WalkExperience를 재생성할 수 있도록 Soft Delete 정책을 우회하지 않는다.

---

## 15. PATCH 정책

PATCH의 세부 동작은 `docs/api-spec.md`를 따른다.

기본 원칙:

### 필드 생략

```text
기존 값 유지
```

### Nullable 필드에 null 전달

```text
기존 값 제거
```

단, 필수 필드는 API 명세의 Validation을 따른다.

### `emotions[]`

```text
필드 생략
→ 기존 값 유지

[]
→ 전체 제거

값 전달
→ 전체 교체
```

### `tags[]`

```text
필드 생략
→ 기존 값 유지

[]
→ 전체 제거

값 전달
→ 전체 교체
```

배열을 부분 추가/삭제 방식으로 임의 변경하지 않는다.

---

## 16. 태그 정책

최종 태그는 다음 정책을 유지한다.

* 최대 10개
* 각 태그 최대 50자
* 빈 문자열 또는 공백 태그 금지
* 동일 Experience 내 중복 금지
* DB에는 `#` 없이 저장
* AI 추천 태그는 자동 저장하지 않음

별도 Tag Master 테이블을 만들지 않는다.

---

## 17. 날짜 및 조회 정책

날짜 기반 산책 경험 조회는 다음 컬럼을 기준으로 한다.

```text
walk_experiences.started_at
```

서비스 날짜 기준 시간대:

```text
Asia/Seoul
```

별도의 다음 컬럼을 만들지 않는다.

```text
year
month
day
```

기본 목록 정렬은 명세에 정의된:

```text
startedAt DESC
```

를 따른다.

페이지네이션, 복합 검색, 고급 필터는 명세에 없는 한 선제 구현하지 않는다.

---

## 18. MVP에서 생성하지 않는 구조

현재 명세에 없는 다음 구조를 임의로 추가하지 않는다.

```text
Calendar 테이블
Album 테이블
AlbumExperience 테이블
Tag Master 테이블
Notification 테이블
Push Token 테이블
Refresh Token 테이블
Session 테이블
Profile 테이블
사진 전용 테이블
이동 경로 전용 테이블
```

기기 권한, 자동 걷기 감지 ON/OFF, 로컬 알림은 클라이언트 영역이다.

---

## 19. 구현 범위 원칙

현재 요청받은 작업 단계에 필요한 코드만 구현한다.

예를 들어 Repository 기본 세팅 작업이라면:

* 실제 필요한 Repository만 생성한다.
* Service를 만들지 않는다.
* DTO를 만들지 않는다.
* Controller를 만들지 않는다.
* 미래 API에서 사용할 Query Method를 미리 만들지 않는다.

기능 구현 단계에서 필요한 코드가 확인되면 그 시점에 추가한다.

**작게 구현하고 문서에 맞춰 확장하는 것을 기본 원칙으로 한다.**
