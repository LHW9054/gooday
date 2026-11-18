# Gooday — 설계 문서

## 1. 시스템 아키텍처

```
┌──────────────────────── Browser (Vanilla HTML / JS / CSS) ────────────────────────┐
│  index.html(대시보드)  bjj.html  money.html  records.html  report.html              │
│  js/api.js  ── fetch(JSON) ──┐                                                     │
└──────────────────────────────┼─────────────────────────────────────────────────────┘
                               ▼  REST  /api/**
┌──────────────────────── Spring Boot 4 (Java 17, :8100) ───────────────────────────┐
│  Controller  →  Service(비즈니스 로직: 달성률·D-day·가용금액 계산)  →  Repository(JPA) │
│                                                                                    │
│  dashboard  dday  routine  goal  │  bjj  │  finance(가계부·카드·Import)  │ record  │  │
│  report(월간 집계 → PDF: OpenPDF)  │  common(예외·응답·현재사용자)                   │
│  @Scheduled: 카드 결제일 알림 / 월말 리포트 생성                                      │
└──────────────────────────────┬─────────────────────────────────────────────────────┘
                               ▼ JDBC
                     MySQL 9 (gooday_db)  ← schema.sql 로 자동 생성
                               ▲
      iCloud Numbers 가계부 → CSV 내보내기 → /api/finance/import (컬럼 매핑 + 중복 방지 해시)
```

- **패키지는 기능(도메인)별**로 나눔: 기능 하나를 고칠 때 한 폴더만 보면 됨.
- **계산은 전부 Service 에서**: 달성률·D-day·하루 가용금액은 저장하지 않고 조회 시 계산 → 데이터가 꼬이지 않음.
- **개인용 단일 사용자**로 시작(`users.id = 1`). `user_id` 컬럼은 처음부터 넣어 두어 나중에 로그인을 붙여도 스키마 변경이 없음.

## 2. DB 테이블 설계 (MySQL)

전체 DDL: `src/main/resources/schema.sql` (★ = 1단계에서 구현됨)

| 영역 | 테이블 | 핵심 컬럼 | 비고 |
|---|---|---|---|
| 공통 | ★`users` | name, timezone | 단일 사용자 id=1 |
| 대시보드 | ★`ddays` | title, category(EXAM/BJJ/PAYDAY/CARD/ETC), target_date, repeat_type(NONE/MONTHLY/YEARLY), pinned | 월급날처럼 반복되는 날은 다음 발생일 자동 계산 |
| 대시보드 | ★`routines` | title, category, days_mask(월=1…일=64), sort_order, active | 요일별 루틴 |
| 대시보드 | ★`routine_checks` | routine_id, check_date | UNIQUE(routine_id, check_date) |
| 대시보드 | ★`goals` | title, period_type(DAILY/WEEKLY/MONTHLY/YEARLY), category, target_value, unit | "주 3회 주짓수", "월 20시간 공부" |
| 대시보드 | ★`goal_progress` | goal_id, log_date, value | 기간 합계 / target = 달성률 |
| 운동 | `bjj_attendance` | attend_date, class_type(GI/NOGI/OPEN_MAT), duration_min, sparring_rounds, techniques, memo | 출석 체크 |
| 운동 | `bjj_competitions` | name, comp_date, location, division, weight_class, result | 대회 D-day 는 이 테이블에서도 계산 |
| 운동 | `bjj_monthly_goals` | ym(YYYY-MM), target_sessions, focus_technique, memo | UNIQUE(user_id, ym) |
| 공부 | `study_logs` | study_date, subject(JLPT 등), minutes, content | 월간 리포트용 |
| 자산 | `payment_methods` | name, type(CASH/BANK/CREDIT_CARD/CHECK_CARD), statement_day(마감일), billing_day(결제일) | 카드 결제 캘린더 |
| 자산 | `tx_categories` | type(INCOME/EXPENSE), name, parent_id | Numbers 의 대분류/소분류 |
| 자산 | `transactions` | tx_date, type, category_id, payment_method_id, amount, currency, description, source(MANUAL/IMPORT), import_batch_id, row_hash | row_hash UNIQUE → 같은 파일 재업로드해도 중복 안 됨 |
| 자산 | `import_batches` | file_name, column_mapping(JSON), row_count | Numbers 열 → DB 컬럼 매핑 기억 |
| 자산 | `monthly_budgets` | ym, expected_income, fixed_expense, savings_target | **하루 가용금액** = (수입 − 고정비 − 저축 − 이번 달 지출) ÷ 남은 일수 |
| 자산 | `card_bills` | payment_method_id, billing_ym, due_date, amount, paid | 청구액 확정/결제 여부 |
| 기록 | `notes` | note_date, title, content, tags, pinned | 자유 메모 |
| 기록 | `daily_reviews` | review_date, mood(1~5), good, bad, tomorrow | UNIQUE(user_id, review_date) |
| 리포트 | `monthly_reports` | ym, summary_json, generated_at | PDF 재생성 없이 스냅샷 보관(선택) |

### Numbers 가계부 연동 전략
21년치 시트는 해마다 열 구성이 조금씩 다를 가능성이 높으므로 **고정 포맷을 강요하지 않는다.**
1. Numbers → 파일 > 내보내기 > CSV (시트별/연도별)
2. 업로드 화면에서 열 매핑 지정 (예: `날짜→tx_date`, `분류→category`, `내역→description`, `금액→amount`, `카드→payment_method`) → `import_batches.column_mapping` 에 저장해 다음에 재사용
3. 행마다 `SHA-256(날짜|금액|내역|결제수단)` = `row_hash` → 중복 업로드 자동 무시
4. 처음 보는 분류명은 `tx_categories` 에 자동 생성

## 3. 폴더 구조

```
gooday/
├── pom.xml, mvnw                        Maven (Spring Boot 4.1, Java 17)
├── docs/ARCHITECTURE.md                 이 문서
└── src/main/
    ├── java/com/gooday/
    │   ├── GoodayApplication.java
    │   ├── common/        ApiException, GlobalExceptionHandler, CurrentUser, DateRanges
    │   ├── dashboard/     DashboardController, DashboardService, DashboardDto   ★
    │   ├── dday/          DDay, DDayRepository, DDayService, DDayController     ★
    │   ├── routine/       Routine, RoutineCheck, …Repository, RoutineService, RoutineController ★
    │   ├── goal/          Goal, GoalProgress, PeriodType, GoalService, GoalController ★
    │   ├── bjj/           (2단계) 출석·대회·월 목표
    │   ├── finance/       (3단계) 거래·카드·예산·CsvImportService
    │   ├── record/        (4단계) 메모·회고·공부기록
    │   └── report/        (5단계) MonthlyReportService, PdfExporter
    └── resources/
        ├── application.properties
        ├── schema.sql     전체 테이블 (IF NOT EXISTS)
        ├── data.sql       기본 사용자·샘플 데이터 (INSERT IGNORE)
        └── static/
            ├── index.html             대시보드 ★
            ├── css/app.css            공통 스타일 ★
            └── js/api.js, dashboard.js ★
```

## 4. 개발 단계 (로드맵)

| 단계 | 내용 | 상태 |
|---|---|---|
| 1 | 프로젝트 뼈대 · 전체 스키마 · 공통 예외 처리 · **대시보드(D-day, 루틴 체크, 목표 달성률 일/주/월/년)** | ✅ |
| 2 | 주짓수: 출석 체크 캘린더, 대회 관리(D-day 자동 연동), 월 목표·메모 | ⏳ |
| 3 | 자산: 거래 입력, 월 예산 → 하루 가용금액, 카드 결제일 캘린더/알림, Numbers CSV Import | ⏳ |
| 4 | 기록: 메모, 일일 회고, 공부 기록 | ⏳ |
| 5 | 리포트: 월간 집계(공부·운동·지출·달성률) + PDF Export (OpenPDF, 한글 폰트 내장) | ⏳ |
