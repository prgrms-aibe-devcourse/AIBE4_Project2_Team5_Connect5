## 팀 협업 방식

### 커밋 컨벤션
#### 커밋 메시지 유형
| 유형 | 의미 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 formatting, 세미콜론 누락 등 |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 추가 |
| `chore` | 패키지 매니저 수정, 기타 수정 |
| `design` | CSS 등 UI 디자인 변경 |
| `comment` | 주석 추가 및 변경 |
| `rename` | 파일/폴더명 수정 또는 이동 |
| `remove` | 파일 삭제 |
| `!breaking change` | 커다란 API 변경 |
| `!hotfix` | 급한 버그 수정 |
| `assets` | 에셋 파일 추가 |

#### 커밋 메시지 형식
**제목:**
```
[YYMMDD] type : 커밋메시지
```
**내용:**
```markdown
### 작업 내용
- 작업 내용 1
- 작업 내용 2
- 작업 내용 3
```

### PR 템플릿
**제목:**
```
[YYMMDD] type : PR메시지
```
**내용:**
```markdown
## 이슈 번호
#이슈번호

## 작업 내용
- 작업 내용 1
- 작업 내용 2
- 작업 내용 3
```

<br>

## 로컬 실행 방법

### 사전 준비
- Docker Desktop 설치

### 
```bash
# 1. 저장소 클론 (GitHub Desktop 사용 또는 CLI)
git clone https://github.com/your-repo/AIBE4_Project2_Team5_Connect5.git

# 2. 디렉토리 이동
cd AIBE4_Project2_Team5_Connect5

# 3. Docker 컨테이너 실행 (PostgreSQL, Redis)
docker compose up -d

# 4. 컨테이너 실행 확인
docker ps

# 5. 애플리케이션 실행
./gradlew bootRun
```