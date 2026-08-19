⚙️ MOWA Backend

Moment & Walk, 걸으며 순간을 모으다.

MOWA의 REST API, 사용자 인증, 산책 경험 데이터 관리 및 AI 산책 일기 생성을 담당하는 Backend Repository입니다.

✨ 주요 기능
🔐 Access Token 기반 사용자 인증 및 데이터 소유권 관리
🚶 산책 후보 생성 및 상태 관리
💭 경험 초안 및 감정 데이터 관리
🤖 OpenAI 기반 산책 일기 생성
✏️ AI 생성 결과 기반 최종 산책 경험 저장
📦 WalkExperience Snapshot 관리
🗂️ 산책 경험 목록·상세 조회
📅 날짜·태그 기반 산책 경험 조회
✏️ 산책 경험 수정 및 Soft Delete
🛡️ Candidate → Draft → Experience 데이터 정합성 관리
🛠 Tech Stack
Category	Technology
Language	Java 21
Framework	Spring Boot
Build	Gradle
Web	Spring Web
ORM	Spring Data JPA / Hibernate
Database	PostgreSQL
Security	Spring Security · JWT
Validation	Spring Validation
AI	Spring AI · OpenAI API

API Key, DB Password, JWT Secret 등 민감한 정보는 환경변수로 관리하며 Repository에 포함하지 않습니다.

🏗 Architecture
Controller
    ↓
Service
    ↓
Repository
    ↓
PostgreSQL

각 계층은 다음 역할을 담당합니다.

Controller — HTTP Request/Response 및 입력 검증
Service — 비즈니스 규칙, 상태 전이, 사용자 소유권 및 트랜잭션 관리
Repository — 데이터 조회 및 저장
Entity — JPA 기반 도메인 및 DB Mapping
DTO — API Request / Response 모델
🗄 Core Domain

MOWA의 산책 경험 데이터는 다음 생명주기를 따릅니다.

WalkCandidate
산책 기록 후보
      ↓
ExperienceDraft
사용자 입력 + AI 생성 결과
      ↓
WalkExperience
사용자가 최종 확정한 산책 경험
관계
WalkCandidate 1 : 0..1 ExperienceDraft
ExperienceDraft 1 : 0..1 WalkExperience

데이터베이스에서는 다음 제약조건으로 중복 생성을 방지합니다.

experience_drafts.candidate_id
→ FK + UNIQUE + NOT NULL


walk_experiences.draft_id
→ FK + UNIQUE + NOT NULL
Candidate 하나에서는 최대 하나의 Draft만 생성할 수 있습니다.
Draft 하나에서는 최대 하나의 WalkExperience만 생성할 수 있습니다.
사용자 소유권은 Service 계층에서도 검증합니다.
📸 WalkExperience Snapshot

최종 확정된 산책 경험은 일반 조회 시 Candidate나 Draft에 의존하지 않도록 Snapshot 형태로 저장합니다.

Candidate
├── detectedStartAt  → startedAt
├── detectedEndAt    → endedAt
├── durationSeconds  → durationSeconds
└── locationSummary  → locationSummary


사용자 최종 확인값
├── title
├── body
├── photoUrl
├── companion
├── situation
├── emotions
└── tags

따라서 아카이브·상세·날짜·태그 조회는 최종 WalkExperience를 중심으로 수행합니다.

🤖 AI Generation

사용자가 입력한 경험 정보와 산책 후보 데이터를 기반으로 OpenAI API를 이용해 산책 일기를 생성합니다.

WalkCandidate
시간 · 장소 · 지속 시간
        +
ExperienceDraft
사진 · 동반자 · 감정 · 상황
        ↓
     OpenAI
        ↓
제목 · 본문 · 추천 태그
AI 생성 상태
PENDING
   ↓
GENERATING
   ├── SUCCESS
   └── FAILED
         ↓
       재시도

AI 생성 결과 중 제목과 본문은 Draft에 저장하며, 추천 태그는 자동 저장하지 않습니다.

사용자가 AI 결과를 확인하고 수정한 뒤 최종 저장한 태그만 WalkExperience에 보존합니다.

🔐 Authentication

MOWA는 Access Token 기반 인증을 사용합니다.

POST /api/v1/auth/login
        ↓
Access Token 발급
        ↓
Authorization: Bearer {accessToken}
        ↓
사용자 식별
        ↓
사용자별 데이터 접근

userId를 클라이언트 Request에서 전달받지 않고 Access Token에서 인증된 사용자를 서버가 식별합니다.

다른 사용자가 소유한 Candidate, Draft, Experience에는 접근할 수 없습니다.

📡 API
Base URL
/api/v1
Auth & User
POST   /auth/login
GET    /users/me
PATCH  /users/me
Walk Candidate
POST   /walk-candidates
GET    /walk-candidates/{candidateId}
PATCH  /walk-candidates/{candidateId}
Experience Draft
POST   /walk-candidates/{candidateId}/experience-drafts
PATCH  /experience-drafts/{draftId}
AI Generation
POST   /experience-drafts/{draftId}/ai-generation
Walk Experience
POST   /walk-experiences
GET    /walk-experiences
GET    /walk-experiences/{experienceId}
PATCH  /walk-experiences/{experienceId}
DELETE /walk-experiences/{experienceId}

총 14개의 REST API를 제공합니다.

세부 Request/Response, Validation, 상태 전이 및 예외 정책은 docs/api-spec.md를 기준으로 합니다.

🔎 Walk Experience Query

하나의 목록 API를 이용하여 전체·기간·태그 조회를 지원합니다.

전체 조회
GET /api/v1/walk-experiences
기간 조회
GET /api/v1/walk-experiences?from=2026-08-01&to=2026-08-31
태그 조회
GET /api/v1/walk-experiences?tag=망원동

날짜 조회는 Asia/Seoul을 기준으로 하며 기본 정렬은 startedAt DESC입니다.

🗃 Database

MVP에서는 총 7개의 물리 테이블을 사용합니다.

users


walk_candidates


experience_drafts
└── experience_draft_emotions


walk_experiences
├── walk_experience_emotions
└── walk_experience_tags

감정과 태그는 별도 Entity를 생성하지 않고 @ElementCollection을 이용하여 관리합니다.

🚀 Getting Started
1. Clone
git clone https://github.com/likelion-walk-diary/walk-diary-backend.git
cd walk-diary-backend
2. Environment Variables

애플리케이션 실행에 필요한 DB, JWT 및 OpenAI 관련 값은 환경변수로 관리합니다.

Database Connection
JWT Secret
JWT Access Token Expiration
OpenAI API Key

실제 Secret 값은 Repository에 포함하지 않습니다.

3. Database

로컬 환경에서 PostgreSQL을 실행하고 프로젝트의 DB 연결 정보를 설정합니다.

PostgreSQL
4. Run

macOS / Linux:

./gradlew bootRun

Windows:

.\gradlew.bat bootRun

기본 서버 주소:

http://localhost:8080
🧪 API Test

프로젝트에는 팀 공유용 Postman Collection이 포함되어 있습니다.

postman/
├── MOWA.postman_collection.json
└── README.md

Collection을 이용하면 다음 흐름을 순서대로 테스트할 수 있습니다.

로그인
  ↓
WalkCandidate 생성
  ↓
Candidate 상태 변경
  ↓
ExperienceDraft 생성
  ↓
AI 산책 일기 생성
  ↓
WalkExperience 최종 저장
  ↓
목록 · 상세 · 수정 · 삭제 조회

자세한 사용 방법은 postman/README.md를 참고해주세요.

🌿 Branch Convention
main
├── feature/{기능명}
├── fix/{수정내용}
└── refactor/{수정내용}

작업 브랜치는 최신 main에서 생성하고 작업 완료 후 main을 대상으로 PR을 생성합니다.

예시:

feature/walk-candidate-api
feature/ai-diary-api
feature/walk-experience-api
fix/duplicate-experience
💬 Commit Convention
Type	Description
feat	새로운 기능
fix	버그 수정
refactor	코드 리팩토링
docs	문서 수정
test	테스트
chore	설정 및 기타 작업

예시:

feat: 산책 후보 생성 API 구현
fix: 중복 산책 경험 저장 방지
refactor: AI 생성 서비스 로직 정리
🔀 Pull Request
최신 main에서 작업 Branch를 생성합니다.
작업 완료 후 main을 대상으로 PR을 생성합니다.
PR 생성 전 관련 기능과 빌드 상태를 확인합니다.
DB 구조 또는 API 계약 변경 시 PR 설명에 명시합니다.
다른 담당 기능에 영향을 미치는 변경은 Merge 전에 공유합니다.
API 명세와 데이터 테이블을 임의로 변경하지 않습니다.
⚠️ Backend Development Rules
API 계약은 docs/api-spec.md를 기준으로 합니다.
DB 구조와 제약조건은 docs/data-table.md를 기준으로 합니다.
Entity 관계는 docs/erd.md를 기준으로 합니다.
JSON 필드는 camelCase, DB 컬럼은 snake_case를 사용합니다.
사용자 소유자는 Access Token을 통해 서버에서 결정합니다.
Candidate → Draft → Experience 관계의 중복 생성을 방지합니다.
최종 WalkExperience는 Snapshot 데이터를 자체 보존합니다.
일반 조회에서 Candidate 또는 Draft에 의존하지 않습니다.
Experience 삭제는 deletedAt 기반 Soft Delete로 처리합니다.
삭제된 Experience는 일반 목록·상세·수정 대상에서 제외합니다.
API Key, DB Password, JWT Secret 등의 민감 정보는 Git에 포함하지 않습니다.
