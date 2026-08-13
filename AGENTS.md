# MOWA Backend Agent Instructions

## 1. 작업 영역

백엔드 작업 루트는 다음과 같다.

```text
/workspace/backend
```

백엔드 작업 요청에서는 원칙적으로 위 디렉토리 내부만 수정한다.

다음 영역은 명시적인 요청이 없는 한 수정하지 않는다.

```text
/workspace/frontend
/workspace의 다른 프로젝트 및 파일
```

작업 전 반드시 현재 디렉토리와 Git 상태를 확인한다.

```bash
cd /workspace/backend
git branch --show-current
git status
```

사용자가 특정 브랜치를 지정한 경우 현재 브랜치와 일치하는지 확인한다.

브랜치가 다르면 임의로 checkout하거나 새 브랜치를 생성하지 말고 보고한다.

---

## 2. 기존 작업 보호

Working Tree에 존재하는 기존 변경사항은 다른 작업자의 코드 또는 이전 작업 결과일 수 있으므로 임의로 삭제하거나 되돌리지 않는다.

다음과 같은 파괴적 명령은 사용하지 않는다.

```text
git reset --hard
git clean
git checkout .
git restore .
```

기존 변경사항과 현재 작업의 변경사항을 구분하여 처리한다.

현재 작업에서 발생한 의도하지 않은 변경이 있다면 해당 변경만 최소 범위로 복구한다.

---

## 3. 작업 범위

사용자가 요청한 범위만 구현한다.

다음 원칙을 따른다.

* 요청받지 않은 기능을 선제 구현하지 않는다.
* 다른 계층이나 도메인을 편의상 함께 수정하지 않는다.
* 기존 설계를 임의로 리팩터링하거나 개선하지 않는다.
* 새로운 라이브러리를 임의로 추가하지 않는다.
* 공통 코드 또는 다른 담당 영역 수정이 필요하면 이유와 영향을 먼저 보고한다.
* 문서 수정이 명시적으로 요청되지 않았다면 구현 과정에서 문서를 임의로 변경하지 않는다.

구현 세부 규칙과 MOWA 도메인 정책은 다음 파일을 따른다.

```text
.aiassistant/rules/mowa-backend.md
```

---

## 4. Git 작업 원칙

사용자가 명시적으로 요청하기 전까지 다음 작업을 수행하지 않는다.

```text
git commit
git push
Pull Request 생성
```

일반적인 개발 작업이 끝나면 코드 리뷰가 가능하도록 변경사항을 Working Tree에 유지한다.

사용자가 별도로 요청하지 않은 경우 임의의 commit message를 만들거나 commit하지 않는다.

---

## 5. 검증

구현 완료 후 프로젝트 루트에서 가능한 범위까지 검증한다.

기본 검증:

```bash
./gradlew compileJava
```

가능하면 추가로 실행한다.

```bash
./gradlew test
```

검증 실패 시 다음 순서로 처리한다.

1. 현재 작업에서 발생한 오류인지 확인한다.
2. 현재 작업 범위 안의 오류라면 최소 범위로 수정한다.
3. 기존 코드 또는 작업 범위 밖의 문제라면 다른 영역을 임의로 수정하지 않고 원인을 보고한다.

검증을 통과시키기 위해 설계 또는 관련 없는 코드를 임의로 변경하지 않는다.

---

## 6. backend-review.patch

백엔드 구현 작업 후 사용자가 코드 리뷰를 진행할 수 있도록 다음 파일을 생성한다.

```text
/workspace/backend-review.patch
```

### Patch 원칙

* 이번 작업에서 발생한 변경사항만 포함한다.
* 이전 작업의 변경사항을 다시 포함하지 않는다.
* 신규 untracked 파일도 빠짐없이 포함한다.
* `backend-review.patch` 자체는 patch에 포함하지 않는다.
* 실제 Git index를 오염시키지 않는 방식을 우선한다.
* patch 생성 후 직접 내용을 확인한다.

신규 파일이 포함된 경우 단순 `git diff`만 사용하면 누락될 수 있으므로 임시 index 방식을 우선한다.

예시:

```bash
tmp_index=$(mktemp)
rm -f "$tmp_index"

GIT_INDEX_FILE="$tmp_index" git read-tree HEAD

GIT_INDEX_FILE="$tmp_index" git add -A -- <이번 작업 경로>

GIT_INDEX_FILE="$tmp_index" git diff --cached --binary HEAD > backend-review.patch

rm -f "$tmp_index"
```

`<이번 작업 경로>`에는 현재 요청에서 수정한 파일 또는 디렉토리만 지정한다.

Patch 생성 후 최소한 다음을 확인한다.

```bash
git status --short
wc -l ../backend-review.patch
```

그리고 patch 내용을 확인하여 다음을 검증한다.

* 이번 작업 파일이 모두 포함되었는지
* 기존 다른 작업이 섞이지 않았는지
* 불필요한 파일이 포함되지 않았는지

Patch 생성 과정 때문에 실제 Git index의 staged 상태가 변경되어서는 안 된다.

---

## 7. 민감 정보 및 로컬 설정

다음 정보는 코드, 문서, patch 또는 Git에 포함하지 않는다.

* API Key
* DB Password
* JWT Secret
* Access Token
* Refresh Token
* 기타 Secret 값

민감 정보는 환경 변수 또는 Git에 포함되지 않는 로컬 설정을 사용한다.

---

## 8. 작업 완료 보고

작업 완료 후 다음 내용을 간결하게 보고한다.

### 필수 항목

1. 작업 요약
2. 생성한 파일
3. 수정한 파일
4. 주요 구현 내용
5. 기준 문서 확인 결과
6. 검증 결과
7. `backend-review.patch` 생성 결과
8. Git 상태
9. 추가 확인 사항

추가 확인 사항이 없다면 다음과 같이 작성한다.

```text
없음
```

보고 시 commit, push, PR을 수행했는지도 명확히 표시한다.

기본값은 다음과 같다.

```text
commit: 하지 않음
push: 하지 않음
PR: 생성하지 않음
```
