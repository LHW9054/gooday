-- 기본 사용자 + 처음 실행 시 보여줄 샘플 (고정 id + INSERT IGNORE → 재시작해도 중복 없음)
INSERT IGNORE INTO users (id, name) VALUES (1, '나');

INSERT IGNORE INTO ddays (id, user_id, title, category, target_date, repeat_type, pinned, memo) VALUES
 (1, 1, 'JLPT 시험', 'EXAM',   '2026-12-06', 'NONE',    TRUE,  '12월 첫째 일요일 (샘플 — 수정하세요)'),
 (2, 1, '주짓수 대회', 'BJJ',  '2027-01-17', 'NONE',    TRUE,  '샘플 대회'),
 (3, 1, '월급날',     'PAYDAY', '2026-01-25', 'MONTHLY', FALSE, '매월 25일');

INSERT IGNORE INTO routines (id, user_id, title, category, days_mask, sort_order, start_date) VALUES
 (1, 1, 'JLPT 단어 30개',  'STUDY',    127, 1, CURDATE()),
 (2, 1, '주짓수 수련',     'EXERCISE', 21,  2, CURDATE()),   -- 월·수·금
 (3, 1, '가계부 기록',     'FINANCE',  127, 3, CURDATE()),
 (4, 1, '물 2L 마시기',    'LIFE',     127, 4, CURDATE());

INSERT IGNORE INTO goals (id, user_id, title, category, period_type, target_value, unit) VALUES
 (1, 1, '주짓수 주 3회',       'EXERCISE', 'WEEKLY',  3,   '회'),
 (2, 1, 'JLPT 공부 월 30시간', 'STUDY',    'MONTHLY', 30,  '시간'),
 (3, 1, '올해 책 12권',        'LIFE',     'YEARLY',  12,  '권');
