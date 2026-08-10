# ⚙️ [서비스명] Backend

`[서비스명]`의 REST API, 데이터 관리 및 AI 산책 일기 생성을 담당하는 Backend Repository입니다.

## 📌 주요 역할

* 산책 후보 데이터 및 상태 관리
* 경험 초안 저장 및 수정
* AI 산책 일기 생성
* 최종 산책 경험 Snapshot 저장
* 산책 경험 목록·상세 조회
* 산책 경험 수정 및 Soft Delete
* 감정·동반자 기반 필터
* 데이터 정합성 및 비즈니스 규칙 관리

---

## 🛠 Tech Stack

> 실제 프로젝트 설정 후 확정된 기술만 작성합니다.

```text
Framework       : [작성 예정]
Language        : [작성 예정]
Database        : PostgreSQL
ORM             : [작성 예정]
AI API          : [작성 예정]
Object Storage  : [작성 예정]
```

---

## 🗄 Core Domain

```text
walk_candidates
      ↓
experience_drafts
      ↓
walk_experiences
```

### 관계

```text
walk_candidates 1 : 0..1 experience_drafts
experience_drafts 1 : 0..1 walk_experiences
```

* Candidate 하나당 Draft는 최대 하나만 생성됩니다.
* Draft 하나당 WalkExperience는 최대 하나만 생성됩니다.
* 두 관계 모두 `FK + UNIQUE + NOT NULL`로 보장합니다.

---

## 📡 API

Base URL:

```text
/api/v1
```

핵심 API:

```text
POST   /walk-candidates
GET    /walk-candidates/{candidateId}
PATCH  /walk-candidates/{candidateId}

POST   /walk-candidates/{candidateId}/experience-drafts
PATCH  /experience-drafts/{draftId}

POST   /experience-drafts/{draftId}/ai-generation

POST   /walk-experiences
GET    /walk-experiences
GET    /walk-experiences/{experienceId}
PATCH  /walk-experiences/{experienceId}
DELETE /walk-experiences/{experienceId}
```

> 세부 Request/Response 및 예외 정책은 API 명세서를 기준으로 합니다.

---

## 🚀 Getting Started

### 1. Repository Clone

```bash
git clone [BACKEND_REPOSITORY_URL]
cd [BACKEND_REPOSITORY]
```

### 2. Environment Variables

DB, AI API, Object Storage 관련 환경변수는 로컬 환경 설정 파일로 관리합니다.

> 환경변수 및 API Key는 Git에 Commit하지 않습니다.

### 3. Database

```text
PostgreSQL
```

실제 연결 정보는 환경변수로 관리합니다.

### 4. Run

```text
프로젝트 기술 스택 확정 후 실행 명령어 작성
```

---

## 🌿 Branch Convention

```text
main
develop
feature/{기능명}
fix/{수정내용}
refactor/{리팩토링내용}
```

예시:

```text
feature/walk-candidate-api
feature/ai-diary-api
feature/walk-experience-api
fix/duplicate-experience
```

---

## 💬 Commit Convention

```text
feat: 새로운 기능
fix: 버그 수정
refactor: 리팩토링
docs: 문서 수정
test: 테스트
chore: 설정 및 기타 작업
```

예시:

```text
feat: 산책 후보 생성 API 구현
fix: 중복 산책 경험 저장 방지
refactor: AI 생성 서비스 로직 정리
```

---

## 🔀 Pull Request

* `develop`에서 기능 Branch를 생성합니다.
* 작업 완료 후 `develop` 대상으로 PR을 생성합니다.
* PR 생성 전 프로젝트 실행 및 관련 기능을 확인합니다.
* DB 구조 또는 API 계약을 변경할 경우 반드시 PR 설명에 작성합니다.
* 다른 담당 기능에 영향을 미치는 변경은 Merge 전에 공유합니다.

---

## ⚠️ Backend 개발 규칙

* API 명세서와 데이터 테이블을 기준으로 구현합니다.
* JSON 필드는 `camelCase`, DB 컬럼은 `snake_case`를 사용합니다.
* API와 DB의 Enum성 값은 확정된 코드값을 사용합니다.
* 하나의 Candidate에 Draft가 중복 생성되지 않도록 보장합니다.
* 하나의 Draft에 WalkExperience가 중복 생성되지 않도록 보장합니다.
* 최종 WalkExperience는 Snapshot 데이터를 자체 보존합니다.
* 일반 조회에서 Draft 또는 Candidate에 불필요하게 의존하지 않습니다.
* Experience 삭제는 `deleted_at`을 이용한 Soft Delete로 처리합니다.
* 삭제된 Experience는 일반 목록·상세·수정 대상에서 제외합니다.
* 사진 파일 자체는 DB에 저장하지 않고 `photo_url`만 저장합니다.
* API Key, DB 비밀번호 등 민감 정보는 Repository에 Commit하지 않습니다.
