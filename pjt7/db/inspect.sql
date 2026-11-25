-- yamyam_pjt7를 기본 스키마로 선택한 후 실행합니다.
SELECT 'users' AS table_name, COUNT(*) AS row_count
FROM users
UNION ALL
SELECT 'foods', COUNT(*)
FROM foods
UNION ALL
SELECT 'meals', COUNT(*)
FROM meals
UNION ALL
SELECT 'meal_items', COUNT(*)
FROM meal_items;

-- 회원별 식단 목록의 조회 계획
EXPLAIN
SELECT id, meal_date, meal_type, note
FROM meals
WHERE user_id = '1'
ORDER BY meal_date DESC, id;

-- 특정 날짜의 식단과 구성 음식
SELECT m.id, m.meal_date, m.meal_type, i.position_no, f.food_name, i.amount, f.basis_unit
FROM meals m
         JOIN meal_items i ON i.meal_id = m.id
         JOIN foods f ON f.food_code = i.food_code
WHERE m.user_id = '1'
  AND m.meal_date = '2025-04-21'
ORDER BY m.id, i.position_no;

-- 화면의 하루 분석과 비교할 영양 합계
SELECT ROUND(CAST(SUM(f.calories * i.amount / f.basis_amount) AS DECIMAL(20, 8)), 1) AS calories,
       ROUND(CAST(SUM(f.carbs * i.amount / f.basis_amount) AS DECIMAL(20, 8)), 1)    AS carbs,
       ROUND(CAST(SUM(f.protein * i.amount / f.basis_amount) AS DECIMAL(20, 8)), 1)  AS protein,
       ROUND(CAST(SUM(f.fat * i.amount / f.basis_amount) AS DECIMAL(20, 8)), 1)      AS fat
FROM meals m
         JOIN meal_items i ON i.meal_id = m.id
         JOIN foods f ON f.food_code = i.food_code
WHERE m.user_id = '1'
  AND m.meal_date = '2025-04-21';
