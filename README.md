# ⚙️ MOWA Backend

> **Moment & Walk, 걸으며 순간을 모으다.**

MOWA의 REST API, 사용자 인증, 산책 경험 데이터 관리 및 AI 산책 일기 생성을 담당하는 Backend Repository입니다.

클라이언트에서 감지한 걷기 활동을 `WalkCandidate`로 관리하고, 사용자가 남긴 최소한의 경험 정보를 바탕으로 AI 산책 일기를 생성한 뒤 최종 `WalkExperience`로 저장합니다.

---

## ✨ 주요 기능

- 🔐 Access Token 기반 사용자 인증 및 데이터 소유권 관리
- 🚶 산책 후보 생성·조회 및 상태 관리
- 💭 경험 초안 생성·수정
- 🤖 OpenAI API 기반 산책 일기 생성
- ✏️ AI 생성 결과 확인 후 최종 산책 경험 저장
- 🗂️ 산책 경험 목록 및 상세 조회
- 📅 날짜·캘린더 기반 산책 경험 조회
- 🏷️ 태그 기반 산책 경험 조회
- 📝 산책 경험 수정 및 Soft Delete
- 👤 사용자 정보 조회 및 닉네임 수정
- 🛡️ Candidate → Draft → Experience 데이터 정합성 및 소유권 검증

---

## 🛠 Tech Stack

| Category | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot |
| Build Tool | Gradle |
| ORM | Spring Data JPA |
| Database | PostgreSQL |
| Authentication | Access Token |
| AI | OpenAI API |
| API | REST API / JSON |

---

## 🏗 Backend Architecture

```text
Client
  │
  │ REST API
  ▼
Controller
  │
  ▼
Service
  │
  ├── 비즈니스 규칙
  ├── 사용자 소유권 검증
  ├── 상태 전이
  └── Transaction
  │
  ▼
Repository
  │
  ▼
PostgreSQL

Service
  │
  └──────────────→ OpenAI API
```

### Layer Responsibility

- **Controller**
  - HTTP Request / Response 처리
  - Request DTO Validation
  - Service 호출

- **Service**
  - 비즈니스 로직
  - 사용자 데이터 소유권 검증
  - Candidate / Draft 상태 전이
  - Transaction 관리
  - Entity 생성 및 변경

- **Repository**
  - 데이터베이스 접근
  - 기능 구현에 필요한 조회

- **Entity**
  - JPA Entity 및 DB Mapping

- **DTO**
  - API Request / Response 모델

---

## 🗄 Core Domain

MOWA Backend의 핵심 데이터 생명주기는 다음과 같습니다.

```text
WalkCandidate
산책 기록 후보
      ↓
ExperienceDraft
사용자 입력 + AI 생성 결과
      ↓
WalkExperience
사용자가 최종 확정한 산책 경험
```

### Relationship

```text
WalkCandidate 1 : 0..1 ExperienceDraft
ExperienceDraft 1 : 0..1 WalkExperience
```

DB에서는 다음 제약조건으로 중복 생성을 방지합니다.

```text
experience_drafts.candidate_id
→ FK + UNIQUE + NOT NULL

walk_experiences.draft_id
→ FK + UNIQUE + NOT NULL
```

따라서:

- 하나의 Candidate에서 최대 하나의 Draft만 생성할 수 있습니다.
- 하나의 Draft에서 최대 하나의 WalkExperience만 생성할 수 있습니다.
- Soft Delete 이후에도 동일 Draft에서 새로운 WalkExperience를 다시 생성할 수 없습니다.

---

## 🔄 State Flow

### WalkCandidate

```text
DETECTED
   ↓
SUGGESTED
   ├── RECORDING
   └── SKIPPED
```

- `DETECTED` : 걷기 활동 감지
- `SUGGESTED` : 사용자에게 기록 제안
- `RECORDING` : 사용자가 기록 진행 선택
- `SKIPPED` : 사용자가 기록 건너뛰기 선택

### ExperienceDraft

```text
PENDING
   ↓
GENERATING
   ├── SUCCESS
   └── FAILED
        ↓
      재시도
```

- `PENDING` : AI 생성 요청 전
- `GENERATING` : AI 생성 진행 중
- `SUCCESS` : AI 제목·본문 생성 완료
- `FAILED` : AI 생성 실패

AI 생성 실패 시 자동 재시도하지 않으며, `FAILED` 상태에서 사용자가 동일 API를 통해 다시 요청할 수 있습니다.

---

## 🤖 AI Generation

AI는 Draft에 저장된 사용자 입력과 연결된 Candidate의 객관적 정보를 사용합니다.

### Input

```text
Candidate
├── 시작 시각
├── 종료 시각
├── 걷기 지속 시간
└── 장소

ExperienceDraft
├── 사진
├── 동반자
├── 감정
└── 상황
```

### Output

```text
AI 산책 일기 제목
AI 산책 일기 본문
추천 태그
```

### AI 생성 원칙

- 사용자가 제공하지 않은 기억을 사실처럼 생성하지 않습니다.
- 입력되지 않은 관계·감정·대화·사건을 임의로 만들어내지 않습니다.
- 사진만으로 사용자 감정이나 인물 관계를 확정하지 않습니다.
- 값이 없는 입력은 Prompt에서 제외합니다.
- AI의 역할은 새로운 기억을 만드는 것이 아니라 **흩어진 경험의 단서를 하나의 회상 가능한 맥락으로 연결하는 것**입니다.

AI가 생성한 `aiTitle`, `aiBody`는 Draft에 저장합니다.

AI의 `suggestedTags`는 DB에 자동 저장하지 않으며, 사용자가 확인·수정한 최종 태그만 WalkExperience에 저장합니다.

---

## 📸 WalkExperience Snapshot

최종 `WalkExperience`는 일반 조회 시 Candidate나 Draft에 의존하지 않도록 Snapshot 구조로 저장합니다.

### Candidate에서 Snapshot

```text
detectedStartAt  → startedAt
detectedEndAt    → endedAt
durationSeconds  → durationSeconds
locationSummary  → locationSummary
```

### 사용자 최종 확인값

```text
title
body
photoUrl
companion
situation
emotions
tags
```

따라서 아카이브·상세·캘린더·태그 조회 시 Candidate 또는 Draft를 다시 조회하지 않고 최종 WalkExperience 데이터를 사용합니다.

---

## 🗃 Database

MVP에서는 총 7개의 테이블을 사용합니다.

| Domain | Table |
| --- | --- |
| 사용자 | `users` |
| 산책 후보 | `walk_candidates` |
| 경험 초안 | `experience_drafts` |
| Draft 감정 | `experience_draft_emotions` |
| 산책 경험 | `walk_experiences` |
| Experience 감정 | `walk_experience_emotions` |
| Experience 태그 | `walk_experience_tags` |

### Core Relationship

```text
users
  │
  ├── 1:N walk_candidates
  ├── 1:N experience_drafts
  └── 1:N walk_experiences

walk_candidates
      │
      │ 1 : 0..1
      ▼
experience_drafts
      │
      ├── 1:N experience_draft_emotions
      │
      │ 1 : 0..1
      ▼
walk_experiences
      │
      ├── 1:N walk_experience_emotions
      └── 1:N walk_experience_tags
```

감정과 태그는 별도의 대리키 없이 복합 Primary Key를 사용합니다.

```text
experience_draft_emotions
→ PK (draft_id, emotion)

walk_experience_emotions
→ PK (experience_id, emotion)

walk_experience_tags
→ PK (experience_id, tag)
```

---

## 📡 REST API

Base URL:

```text
/api/v1
```

### Authentication

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/login` | 로그인 |

### User

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/users/me` | 내 정보 조회 |
| `PATCH` | `/users/me` | 닉네임 수정 |

### Walk Candidate

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/walk-candidates` | 산책 후보 생성 |
| `GET` | `/walk-candidates/{candidateId}` | 산책 후보 조회 |
| `PATCH` | `/walk-candidates/{candidateId}` | 후보 정보·상태 변경 |

### Experience Draft

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/walk-candidates/{candidateId}/experience-drafts` | 경험 초안 생성 |
| `PATCH` | `/experience-drafts/{draftId}` | 경험 초안 수정 |

### AI Generation

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/experience-drafts/{draftId}/ai-generation` | AI 산책 일기 생성 |

### Walk Experience

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/walk-experiences` | 최종 산책 경험 저장 |
| `GET` | `/walk-experiences` | 산책 경험 목록·날짜·태그 조회 |
| `GET` | `/walk-experiences/{experienceId}` | 산책 경험 상세 조회 |
| `PATCH` | `/walk-experiences/{experienceId}` | 산책 경험 수정 |
| `DELETE` | `/walk-experiences/{experienceId}` | 산책 경험 삭제 |

총 **14개의 REST API**를 제공합니다.

> 세부 Request / Response, Validation, 상태 코드 및 예외 정책은 `docs/api-spec.md`를 기준으로 합니다.

---

## 📅 Archive Query

별도의 Calendar 또는 Archive 테이블을 생성하지 않습니다.

날짜 기반 조회는:

```text
walk_experiences.started_at
```

을 기준으로 처리합니다.

### 전체 조회

```http
GET /api/v1/walk-experiences
```

### 기간 조회

```http
GET /api/v1/walk-experiences?from=2026-08-01&to=2026-08-31
```

### 태그 조회

```http
GET /api/v1/walk-experiences?tag=망원동
```

날짜 기준 시간대는:

```text
Asia/Seoul
```

입니다.

기본 목록 정렬은:

```text
startedAt DESC
```

입니다.

---

## 🔐 Authentication & Ownership

MOWA는 Access Token을 이용해 로그인 사용자를 식별합니다.

```http
Authorization: Bearer {accessToken}
```

`userId`는 API Request에서 전달받지 않습니다.

서버가 인증된 사용자를 기준으로 다음 데이터의 소유자를 결정합니다.

```text
WalkCandidate
ExperienceDraft
WalkExperience
```

```text
walk_candidates.user_id
        =
experience_drafts.user_id
        =
walk_experiences.user_id
```

다른 사용자가 소유한 리소스에 접근한 경우 리소스 존재 여부를 노출하지 않고 `404 Not Found`로 처리합니다.

---

## 🗑 Soft Delete

WalkExperience는 실제 DB Row를 제거하지 않고 `deletedAt`을 이용해 삭제합니다.

```text
deletedAt = 삭제 시각
```

삭제된 Experience는 다음 대상에서 제외합니다.

- 목록 조회
- 상세 조회
- 수정
- 재삭제

MVP에서는 삭제 복구 API를 제공하지 않습니다.

---

## 🏷 Emotion & Tag

### Emotion

감정은 다중 선택이 가능하며 별도 연결 테이블에서 관리합니다.

```text
CALM
HAPPY
TIRED
REFRESHED
PENSIVE
```

### Tag

최종 태그 정책:

- Experience당 최대 10개
- 태그당 최대 50자
- 빈 문자열 및 공백 태그 금지
- 동일 Experience 내 중복 금지
- DB에는 `#` 없이 저장
- AI 추천 태그는 자동 저장하지 않음

---

## 🚀 Getting Started

### Requirements

- Java 21
- PostgreSQL
- Gradle

### 1. Clone

```bash
git clone https://github.com/likelion-mowa/mowa-backend.git
cd mowa-backend
```

### 2. Environment Variables

DB, 인증 및 외부 API에 필요한 민감 정보는 환경변수로 관리합니다.

예:

```text
Database Connection
JWT / Access Token Secret
OpenAI API Key
```

> API Key, DB Password, Secret 등 민감 정보는 Repository에 Commit하지 않습니다.

### 3. Run

macOS / Linux:

```bash
./gradlew bootRun
```

Windows:

```bash
gradlew.bat bootRun
```

### 4. Build

macOS / Linux:

```bash
./gradlew build
```

Windows:

```bash
gradlew.bat build
```

### 5. Test

macOS / Linux:

```bash
./gradlew test
```

Windows:

```bash
gradlew.bat test
```

---

## 🌿 Branch Convention

```text
main
├── feature/{기능명}
├── fix/{수정내용}
├── refactor/{수정내용}
└── docs/{문서작업}
```

작업 Branch는 최신 `main`을 기준으로 생성하고 작업 완료 후 `main`을 대상으로 Pull Request를 생성합니다.

예시:

```text
feature/walk-candidate-api
feature/experience-draft-api
feature/ai-generation
feature/walk-experience-api
fix/duplicate-experience
docs/api-spec-update
```

---

## 💬 Commit Convention

| Type | Description |
| --- | --- |
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 코드 리팩토링 |
| `docs` | 문서 수정 |
| `test` | 테스트 코드 |
| `chore` | 설정 및 기타 작업 |

예시:

```text
feat: 산책 후보 생성 API 구현
fix: 중복 산책 경험 저장 방지
refactor: AI 생성 서비스 로직 정리
docs: API 명세서 수정
chore: 배포 환경 설정
```

---

## 🔀 Pull Request

- 최신 `main`에서 작업 Branch를 생성합니다.
- 작업 완료 후 `main`을 대상으로 PR을 생성합니다.
- PR 생성 전 관련 기능의 정상 동작을 확인합니다.
- DB 구조 또는 API 계약 변경 시 PR 설명에 변경 내용을 명시합니다.
- 다른 담당 기능에 영향을 미치는 변경은 Merge 전에 공유합니다.
- API 명세와 구현이 불일치하지 않는지 확인합니다.

---

## 📚 Backend Specification

Backend 구현 시 다음 문서를 기준으로 합니다.

```text
docs/api-spec.md
docs/data-table.md
docs/erd.md
docs/feature-spec.md
```

| Document | 기준 |
| --- | --- |
| `api-spec.md` | Endpoint, Request/Response, Validation, 상태 코드 |
| `data-table.md` | DB 컬럼, 제약조건, Enum, 인덱스 |
| `erd.md` | Entity 관계, FK, Cardinality |
| `feature-spec.md` | 기능 목적, 사용자 흐름, MVP 범위 |

---

## ⚠️ Development Rules

- JSON 필드는 `camelCase`, DB 컬럼은 `snake_case`를 사용합니다.
- API 및 DB Enum 값은 명세에 정의된 값만 사용합니다.
- `userId`를 Request에서 받아 데이터 소유자를 결정하지 않습니다.
- Candidate당 Draft는 최대 하나만 생성합니다.
- Draft당 WalkExperience는 최대 하나만 생성합니다.
- 최종 WalkExperience는 Snapshot 데이터를 자체 보존합니다.
- 일반 조회에서 Draft 또는 Candidate에 불필요하게 의존하지 않습니다.
- Experience 삭제는 `deletedAt` 기반 Soft Delete를 사용합니다.
- 사진 파일 자체를 DB에 저장하지 않고 `photoUrl`만 저장합니다.
- 감정과 태그는 별도 연결 테이블을 통해 관리합니다.
- API Key, DB Password, JWT Secret 등 민감 정보는 Git에 포함하지 않습니다.
- 세부 개발 규칙은 `AGENTS.md`와 `.aiassistant/rules/mowa-backend.md`를 따릅니다.
