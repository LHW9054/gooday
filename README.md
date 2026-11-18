# ☀️ Gooday

나의 개인 하루 비서 — Spring Boot 4 + MySQL + Vanilla JS

설계(아키텍처 · DB · 폴더 구조 · 로드맵): [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## 실행
1. MySQL 실행. DB 비밀번호는 `src/main/resources/application-local.properties.example` 을 `application-local.properties` 로 복사해 입력 (또는 환경변수 `DB_PASSWORD`)
   - `gooday_db` 와 전체 테이블, 샘플 데이터는 **자동 생성**
2. `./mvnw spring-boot:run`
3. http://localhost:8100

## 1단계에서 동작하는 것
- D-day: 추가/삭제, 고정, 매월·매년 반복 (월급날 → 다음 25일 자동 계산)
- 오늘의 루틴: 요일별 루틴, 클릭으로 체크/해제
- 루틴 달성률: 오늘 / 이번 주 / 이번 달 / 올해 누적
- 목표: 매일·매주·매월·매년 목표, +1 / 직접 입력으로 진행 기록, 달성률 바

## 자산 (돈 계획표) — http://localhost:8100/money.html
- Numbers `2027돈계획표上` 구조 그대로: 수입 / 지출 / 엔화저축 / 원화저축 + 종합표(자산 목표, 분기 목표, 원화저축, 빚 상환, 월별 표)
- 금액만 입력하면 합계·비율·전달대비·누적·성장률·**용돈(남은 돈)**·**투자·저축(엔화저축 합계)** 자동 계산
- '지난달 금액 복사'로 새 달 시작, 항목 추가/숨기기, 환율 기록
- 개인 금액 데이터는 DB(`gooday_db`)에만 있고 코드/저장소에는 없음

## API
| Method | URL | 설명 |
|---|---|---|
| GET | `/api/dashboard` | 대시보드 전체 (한 번에) |
| GET/POST | `/api/ddays` | D-day 목록 / 추가 |
| PUT/DELETE | `/api/ddays/{id}` | 수정 / 삭제 |
| GET/POST | `/api/routines?date=` | 날짜별 루틴 / 추가 |
| POST | `/api/routines/{id}/toggle?date=` | 체크 토글 |
| GET/POST | `/api/goals` | 목표 현황 / 추가 |
| POST | `/api/goals/{id}/progress` | 진행 기록 `{"value": 1.5}` |
