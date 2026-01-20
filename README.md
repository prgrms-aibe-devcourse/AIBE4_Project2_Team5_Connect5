# 얼마고? [ 중고 물품 경매 플랫폼 ]
프로젝트 소개
리드미 추후 작성 예정

<br>

# 개발 기간
**2025.12 ~ 2026.01**

<br>

## 배포 링크
<a href="https://aibe4-project2-team5-connect5.onrender.com/">
  <img
    src="https://lpjncdsaqnkfjnhadodz.supabase.co/storage/v1/object/public/eolmago/logo.png"
    alt="얼마고"
    width="220"
  />
</a>

<br>

## 팀원
| <img src="https://github.com/so-myoung.png" width="100" height="100"/> | <img src="https://github.com/jk-Nam.png" width="100" height="100"/> | <img src="https://github.com/jihun4452.png" width="100" height="100"/> | <img src="https://github.com/yerincho94.png" width="100" height="100"/> | <img src="https://github.com/c-wonjun.png" width="100" height="100"/>         |
|-----------------------------------------------------------------------|-------------------------------------------|-----------------------------------------------|---|-----------------------------------------|
| 김소명 | 남준구 | 박지훈 | 조예린 | 최원준 |
| [so-myoung](https://github.com/so-myoung)| [jk-Nam](https://github.com/jk-Nam) | [jihun4452](https://github.com/jihun4452) | [yerincho94](https://github.com/yerincho94) | [c-wonjun](https://github.com/c-wonjun) |

<br>

## 기술 스택

### Backend
- Spring Boot / Spring Data JPA / Spring Security
- PostgreSQL
- Redis
- Swagger UI

### Infra
- Docker
- GitHub Actions
- Supabase
- Render

### Frontend
- Thymeleaf
- Tailwind CSS

<br>

## 주요 기능
추후 정리 예정

### 1. 경매 & 거래 관리

- 중고 전자기기 경매 등록/수정/삭제
- 실시간 입찰 및 낙찰 처리
- 경매 이후 자동으로 거래 엔티티 생성
- 거래 상태에 따른 상세한 **상태 전이 로직** 적용
- 판매자/구매자 마이페이지에서 **나의 경매 / 나의 거래 목록** 확인

### 2. 리뷰 시스템

- 거래 완료 시 상호 리뷰 작성
- 별점 + 텍스트 리뷰
- 받은 리뷰 / 작성한 리뷰 목록 분리 조회
- 리뷰 정렬/필터링 (예: 최신순, 별점 높은순 등)

### 3. 고급 검색 & 추천

> 이 프로젝트의 특징적인 기능 중 하나는 **한국어 환경에 최적화된 검색 시스템**입니다.

- **실시간 자동완성**
    - 검색어 입력 시 즉시 후보 키워드 제시
    - 초성 검색 지원 (예: `ㅇㅇㅍ` → `아이폰`)
    - 인기 기반 정렬 (검색량이 많은 키워드 우선)
    - 최대 10개까지 자동완성 노출

- **3단계 검색 전략 (Fallback Chain)**
    1. **초성 검색**
    2. **Full-Text Search (PostgreSQL + Redis)**
    3. **Trigram Similarity 기반 오타 보정**  
       (예: `아이혼` → `아이폰` 자동 매칭)

- 검색 결과가 없을 시 **추천 키워드** 노출

### 4. 인증 & 보안

- JWT 기반 인증/인가
- Spring Security 기반 권한 관리
- 로그인 사용자에 따라 접근 가능한 페이지/기능 제어

### 5. 기타 기능

- Swagger UI를 통한 API 문서 제공
- Docker 기반 컨테이너 환경 구성
- GitHub Actions를 활용한 CI/CD 파이프라인

---


### 폴더 구조
```
kr.eolmago
├── controller
│   ├── api         # REST API 컨트롤러
│   └── view        # 페이지 렌더링용 View 컨트롤러
├── dto
│   ├── api
│   │   ├── request  # API 요청 DTO
│   │   └── response # API 응답 DTO
│   └── view        # View 렌더링용 DTO
├── domain
│   └── entity      # JPA 엔티티
├── repository      # JPA Repository 인터페이스
├── service         # 비즈니스 로직
└── global
    ├── config      # 공통 설정 (Security, JPA 등)
    ├── exception   # 예외 처리
    ├── handler     # 글로벌 핸들러
    ├── common      # 공용 유틸/베이스 클래스
    └── util        # 유틸리티
```

### 로컬 실행 방법
```
# 1. 저장소 클론
git clone https://github.com/prgrms-aibe-devcourse/AIBE4_Project2_Team5_Connect5.git

# 2. 디렉토리 이동
cd AIBE4_Project2_Team5_Connect5

# 3. Docker 컨테이너 실행 (PostgreSQL, Redis 등)
docker compose up -d

# 4. 컨테이너 실행 확인
docker ps

# 5. 애플리케이션 실행
./gradlew bootRun

```

### 팀 협업 방식
## 커밋 컨벤션

| 타입                 | 설명                |
| ------------------ | ----------------- |
| `feat`             | 새로운 기능 추가         |
| `fix`              | 버그 수정             |
| `docs`             | 문서 수정             |
| `style`            | 코드 포맷팅, 세미콜론 누락 등 |
| `refactor`         | 코드 리팩토링           |
| `test`             | 테스트 코드 추가         |
| `chore`            | 빌드/패키지 수정, 기타 잡일  |
| `design`           | CSS 등 UI 디자인 변경   |
| `comment`          | 주석 추가 및 변경        |
| `rename`           | 파일/폴더명 수정 또는 이동   |
| `remove`           | 파일 삭제             |
| `!breaking change` | 큰 규모의 API 변경      |
| `!hotfix`          | 급한 버그 수정          |
| `assets`           | 에셋 파일 추가          |

```
[YYMMDD] type : 커밋메시지

### 작업 내용
- 작업 내용 1
- 작업 내용 2
- 작업 내용 3
```

### 테스트 & 품질
- 단위 테스트 / 통합 테스트를 통한 도메인 로직 검증
- Swagger 기반 수동 테스트 및 문서화
- GitHub Actions로 PR 단위 빌드/테스트 자동화 (구성 시)

### 트러블슈팅 및 해결방안 정리

1. 동시 입찰로 인한 중복 요청 & 데이터 경합 문제
- 문제 상황
    - 같은 아이템에 대해 여러 사용자가 동시에 입찰할 때
    - 중복 요청이 들어오며 데이터 정합성 불일치 발생 가능 (동시성 문제)
    - 트랜잭션 레벨에서 경쟁 조건 발생
    -
- 해결 전략
    - clientRequestId 기반 멱등 처리
      → 동일 요청 중복 실행 방지

    - DB UNIQUE 제약으로 중복 차단

    - SELECT FOR UPDATE 비관적 락 적용
      → 동시 입찰 충돌 제어 + 순차 처리 보장

    - REDIS STREAMS 사용
      → 입찰 요청을 큐 기반으로 직렬 처리
      → 과도한 동시 요청 방지 + 데이터 정합성 강화

      핵심 목표: 중복 입찰 방지 + 정합성 확보 + 안전한 경쟁 처리

2. LAZY 로딩으로 발생하는 N+1 문제
- 문제 상황
    - JPA 연관관계 Lazy 로딩에서 발생하는 N+1 쿼리 폭발 문제
    - 특히 DTO 매핑 과정에서 연관 엔티티 조회가 다수 발생
    -
- 해결 전략
    - QueryDSL DTO Projection 사용
      → 필요한 필드만 조회

    - fetch join 적용
      → 한 번의 쿼리로 필요한 연관 데이터 로딩

    - 불필요한 쿼리 제거 + 조회 성능 개선

    - 핵심 목표: 쿼리 최적화 + 성능 개선 + 오버패칭 제거

3. Self-invocation으로 인한 @Transactional 미적용 문제
- 문제 상황
    - 동일 클래스 내부 메소드 호출 시 AOP 프락시가 적용되지 않아
      @Transactional이 동작하지 않는 현상
    -
- 해결 전략
    - 트랜잭션 내부에서 이벤트 분리 (이벤트 발행)
    - 커밋 이후 리스너가 실행되도록 설계 분리

    - 핵심 목표: 트랜잭션 경계 명확화 + 이벤트 처리 안정화

4. Thymeleaf 렌더링 오류로 인한 로그인 풀림 문제
- 문제 상황
    - 로그인 후 렌더링 시 인증 정보가 유지되지 않는 현상
    - 인증 필터와 템플릿 사이 인증 상태 불일치

- 해결 전략
    - 템플릿 렌더링 정상화
    - NavModelAdvice 적용
    - JwtAuthenticationFilter 개선
    - 페이지 이동 시 인증 정보 유지 안정화

    - 핵심 목표: 사용자 경험 개선 + 인증 유지 일관성 확보
